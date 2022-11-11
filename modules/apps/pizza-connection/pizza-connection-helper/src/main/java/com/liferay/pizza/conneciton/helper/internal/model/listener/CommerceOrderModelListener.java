package com.liferay.pizza.conneciton.helper.internal.model.listener;

import com.liferay.commerce.account.model.CommerceAccount;
import com.liferay.commerce.constants.CommerceOrderConstants;
import com.liferay.commerce.model.CommerceOrder;
import com.liferay.commerce.model.CommerceOrderItem;
import com.liferay.object.model.ObjectDefinition;
import com.liferay.object.model.ObjectEntry;
import com.liferay.object.service.ObjectDefinitionLocalService;
import com.liferay.object.service.ObjectEntryLocalService;
import com.liferay.portal.kernel.exception.ModelListenerException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.BaseModelListener;
import com.liferay.portal.kernel.model.ModelListener;

import java.io.Serializable;

import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(immediate = true, service = ModelListener.class)
public class CommerceOrderModelListener
	extends BaseModelListener<CommerceOrder> {

	@Override
	public void onAfterCreate(CommerceOrder commerceOrder)
		throws ModelListenerException {
	}

	@Override
	public void onAfterUpdate(
			CommerceOrder originalModel, CommerceOrder commerceOrder)
		throws ModelListenerException {

		if (commerceOrder.getPaymentStatus() ==
				CommerceOrderConstants.PAYMENT_STATUS_PAID) {

			try {
				CommerceAccount commerceAccount =
					commerceOrder.getCommerceAccount();

				ObjectDefinition objectDefinition =
					_objectDefinitionLocalService.fetchSystemObjectDefinition(
						"AccountEntry");

				if (objectDefinition == null) {
					return;
				}

				ObjectEntry objectEntry =
					_objectEntryLocalService.fetchObjectEntry(
						commerceAccount.getExternalReferenceCode(),
						objectDefinition.getObjectDefinitionId());

				if (objectEntry == null) {
					return;
				}

				Map<String, Serializable> values = objectEntry.getValues();

				Integer loyaltyPoints = (Integer)values.getOrDefault(
					"loyaltyPoints", 0);

				long pizzaCount = commerceOrder.getCommerceOrderItems(
				).stream(
				).map(
					CommerceOrderItem::getQuantity
				).mapToInt(
					Integer::intValue
				).sum();

				// TODO: Check those are pizzas

				loyaltyPoints += (int)pizzaCount;

				if (loyaltyPoints > 10) {
					values.put("loyaltyPoints", loyaltyPoints - 10);

					// TODO: Create discount

				}
				else {
					values.put("loyaltyPoints", loyaltyPoints);
				}

				values.put(
					"loyaltyPoints",
					loyaltyPoints > 10 ? 0 : loyaltyPoints - 10);

				objectEntry.setValues(values);

				_objectEntryLocalService.updateObjectEntry(objectEntry);
			}
			catch (PortalException e) {
				_log.error(e);
			}
		}
	}

	private static final Log _log = LogFactoryUtil.getLog(
		CommerceOrderModelListener.class);

	@Reference
	private ObjectDefinitionLocalService _objectDefinitionLocalService;

	@Reference
	private ObjectEntryLocalService _objectEntryLocalService;

}