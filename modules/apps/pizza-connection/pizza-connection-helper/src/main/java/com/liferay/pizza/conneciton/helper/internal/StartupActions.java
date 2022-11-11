package com.liferay.pizza.connection.helper.internal;

import com.liferay.object.model.ObjectDefinition;
import com.liferay.object.service.ObjectDefinitionLocalService;
import com.liferay.object.service.ObjectFieldLocalService;
import com.liferay.petra.string.StringPool;
import com.liferay.pizza.conneciton.helper.internal.util.AddressResolver;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.util.PropsUtil;

import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(immediate = true)
public class StartupActions {

	@Activate
	protected void activate(Map<String, Object> props) throws Exception {
		_setProperties();

		_configureCustomFields();

		_addressResolver.resolveAddress("via Valaro, 6 ,42019, Scandiano");
	}

	private void _configureCustomFields() throws PortalException {
		ObjectDefinition objectDefinition =
			_objectDefinitionLocalService.fetchSystemObjectDefinition(
				"AccountEntry");

		if (_objectFieldLocalService.fetchObjectField(
				objectDefinition.getObjectDefinitionId(), "loyaltyPoints") ==
					null) {

			return;
		}

		_objectFieldLocalService.addCustomObjectField(
			StringPool.BLANK,
			_userLocalService.getDefaultUserId(_portal.getDefaultCompanyId()),
			0, objectDefinition.getObjectDefinitionId(), "Integer", "Integer",
			null, false, false, null,
			HashMapBuilder.put(
				LocaleUtil.getDefault(), "loyaltyPoints"
			).build(),
			"loyaltyPoints", false, false, Collections.emptyList());
	}

	private void _setProperties() {
		Properties properties = new Properties();

		properties.setProperty("users.screen.name.always.autogenerate", "true");
		PropsUtil.addProperties(properties);
	}

	@Reference
	private AddressResolver _addressResolver;

	@Reference
	private ObjectDefinitionLocalService _objectDefinitionLocalService;

	@Reference
	private ObjectFieldLocalService _objectFieldLocalService;

	@Reference
	private Portal _portal;

	@Reference
	private UserLocalService _userLocalService;

}