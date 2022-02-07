/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.analytics.dxp.entities.exporter.internal.dto.v1_0.converter;

import com.liferay.analytics.dxp.entities.exporter.dto.v1_0.Channel;
import com.liferay.analytics.dxp.entities.exporter.dto.v1_0.Order;
import com.liferay.analytics.dxp.entities.exporter.dto.v1_0.OrderItem;
import com.liferay.commerce.account.model.CommerceAccount;
import com.liferay.commerce.currency.model.CommerceCurrency;
import com.liferay.commerce.model.CommerceOrder;
import com.liferay.commerce.model.CommerceOrderItem;
import com.liferay.commerce.model.CommerceOrderType;
import com.liferay.commerce.product.model.CPInstance;
import com.liferay.commerce.product.model.CommerceChannel;
import com.liferay.commerce.product.service.CommerceChannelLocalService;
import com.liferay.commerce.service.CommerceOrderLocalService;
import com.liferay.commerce.service.CommerceOrderTypeLocalService;
import com.liferay.headless.commerce.core.util.LanguageUtils;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.vulcan.dto.converter.DTOConverter;
import com.liferay.portal.vulcan.dto.converter.DTOConverterContext;

import java.util.ArrayList;
import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Riccardo Ferrari
 */
@Component(
	property = "dto.class.name=com.liferay.analytics.dxp.entities.exporter.dto.v1_0.Order",
	service = {DTOConverter.class, OrderDTOConverter.class}
)
public class OrderDTOConverter implements DTOConverter<CommerceOrder, Order> {

	@Override
	public String getContentType() {
		return Order.class.getSimpleName();
	}

	@Override
	public CommerceOrder getObject(String externalReferenceCode)
		throws Exception {

		return _commerceOrderLocalService.getCommerceOrder(
			GetterUtil.getLong(externalReferenceCode));
	}

	@Override
	public Order toDTO(
			DTOConverterContext dtoConverterContext,
			CommerceOrder commerceOrder)
		throws Exception {

		if (commerceOrder == null) {
			return null;
		}

		CommerceAccount commerceAccount = commerceOrder.getCommerceAccount();

		CommerceChannel commerceChannel =
			_commerceChannelLocalService.getCommerceChannelByOrderGroupId(
				commerceOrder.getGroupId());

		CommerceCurrency commerceCurrency = commerceOrder.getCommerceCurrency();

		return new Order() {
			{
				accountExternalReferenceCode =
					commerceAccount.getExternalReferenceCode();
				accountId = commerceAccount.getCommerceAccountId();
				advanceStatus = commerceOrder.getAdvanceStatus();
				channel = _getChannel(commerceChannel);
				channelExternalReferenceCode = null;
				channelId = null;
				createDate = commerceOrder.getCreateDate();
				currencyCode = commerceCurrency.getCode();
				externalReferenceCode =
					commerceOrder.getExternalReferenceCode();
				id = commerceOrder.getCommerceOrderId();
				modifiedDate = commerceOrder.getModifiedDate();
				orderDate = commerceOrder.getOrderDate();
				orderItems = _getCommerceOrderItems(commerceOrder);
				orderStatus = commerceOrder.getOrderStatus();
				orderTypeExternalReferenceCode =
					_getOrderTypeExternalReferenceCode(
						commerceOrder.getCommerceOrderTypeId());
				orderTypeId = commerceOrder.getCommerceOrderTypeId();
				paymentMethod = commerceOrder.getCommercePaymentMethodKey();
				paymentStatus = commerceOrder.getPaymentStatus();
				purchaseOrderNumber = commerceOrder.getPurchaseOrderNumber();
				requestedDeliveryDate =
					commerceOrder.getRequestedDeliveryDate();
				total = commerceOrder.getTotal();
				transactionId = commerceOrder.getTransactionId();
			}
		};
	}

	private Channel _getChannel(CommerceChannel commerceChannel) {
		return new Channel() {
			{
				currencyCode = commerceChannel.getCommerceCurrencyCode();
				externalReferenceCode =
					commerceChannel.getExternalReferenceCode();
				id = commerceChannel.getCommerceChannelId();
				name = commerceChannel.getName();
				type = commerceChannel.getType();
			}
		};
	}

	private OrderItem[] _getCommerceOrderItems(CommerceOrder commerceOrder) {
		List<OrderItem> orderItems = new ArrayList<>();

		for (CommerceOrderItem commerceOrderItem :
				commerceOrder.getCommerceOrderItems()) {

			CPInstance cpInstance = commerceOrderItem.fetchCPInstance();

			orderItems.add(
				new OrderItem() {
					{
						externalReferenceCode =
							commerceOrderItem.getExternalReferenceCode();
						finalPrice = commerceOrderItem.getFinalPrice();
						id = commerceOrderItem.getCommerceOrderItemId();
						name = LanguageUtils.getLanguageIdMap(
							commerceOrderItem.getNameMap());
						options = commerceOrderItem.getJson();
						orderExternalReferenceCode =
							commerceOrder.getExternalReferenceCode();
						orderId = commerceOrder.getCommerceOrderId();
						quantity = commerceOrderItem.getQuantity();
						sku = commerceOrderItem.getSku();
						skuExternalReferenceCode = _getSkuExternalReferenceCode(
							cpInstance);
						skuId = _getSkuId(cpInstance);
						subscription = commerceOrderItem.isSubscription();
						unitPrice = commerceOrderItem.getUnitPrice();
					}
				});
		}

		return orderItems.toArray(new OrderItem[0]);
	}

	private String _getOrderTypeExternalReferenceCode(long commerceOrderTypeId)
		throws Exception {

		CommerceOrderType commerceOrderType =
			_commerceOrderTypeLocalService.fetchCommerceOrderType(
				commerceOrderTypeId);

		if (commerceOrderType == null) {
			return null;
		}

		return commerceOrderType.getExternalReferenceCode();
	}

	private String _getSkuExternalReferenceCode(CPInstance cpInstance) {
		if (cpInstance == null) {
			return StringPool.BLANK;
		}

		return cpInstance.getExternalReferenceCode();
	}

	private Long _getSkuId(CPInstance cpInstance) {
		if (cpInstance == null) {
			return 0L;
		}

		return cpInstance.getCPInstanceId();
	}

	@Reference
	private CommerceChannelLocalService _commerceChannelLocalService;

	@Reference
	private CommerceOrderLocalService _commerceOrderLocalService;

	@Reference
	private CommerceOrderTypeLocalService _commerceOrderTypeLocalService;

}