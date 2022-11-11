package com.liferay.pizza.conneciton.helper.internal.portlet;

import com.liferay.account.model.AccountEntry;
import com.liferay.commerce.account.model.CommerceAccount;
import com.liferay.commerce.account.service.CommerceAccountLocalService;
import com.liferay.commerce.constants.CommerceAddressConstants;
import com.liferay.commerce.model.CommerceAddress;
import com.liferay.commerce.service.CommerceAddressLocalService;
import com.liferay.login.web.constants.LoginPortletKeys;
import com.liferay.petra.string.StringPool;
import com.liferay.pizza.conneciton.helper.internal.util.AddressResolver;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.model.Address;
import com.liferay.portal.kernel.model.Country;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCActionCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCActionCommand;
import com.liferay.portal.kernel.service.AddressLocalService;
import com.liferay.portal.kernel.service.CountryLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.WebKeys;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
	property = {
		"javax.portlet.name=" + LoginPortletKeys.FAST_LOGIN,
		"javax.portlet.name=" + LoginPortletKeys.LOGIN,
		"mvc.command.name=/login/create_account", "service.ranking:Integer=100"
	},
	service = MVCActionCommand.class
)
public class CreateAccountMVCActionCommand extends BaseMVCActionCommand {

	@Override
	protected void doProcessAction(
			ActionRequest actionRequest, ActionResponse actionResponse)
		throws Exception {

		ThemeDisplay themeDisplay = (ThemeDisplay)actionRequest.getAttribute(
			WebKeys.THEME_DISPLAY);

		String emailAddress = ParamUtil.getString(
			actionRequest, "emailAddress");

		String fullAddress = ParamUtil.getString(actionRequest, "fullAddress");
		String phoneNumber = ParamUtil.getString(actionRequest, "phoneNumber");

		mvcActionCommand.processAction(actionRequest, actionResponse);

		User user = _userLocalService.getUserByEmailAddress(
			themeDisplay.getCompanyId(), emailAddress);

		CommerceAccount commerceAccount =
			_commerceAccountLocalService.getPersonalCommerceAccount(
				user.getUserId());

		JSONObject jsonObject = _addressResolver.resolveAddress(fullAddress);

		JSONArray results = jsonObject.getJSONArray("results");

		JSONArray address_components = results.getJSONObject(
			0
		).getJSONArray(
			"address_components"
		);

		String city = "";
		String zip = "";
		long regionId = 0;

		Country country = null;

		for (int i = 0; i < address_components.length(); i++) {
			JSONObject jsonObject1 = address_components.getJSONObject(i);

			String[] types = JSONUtil.toStringArray(
				jsonObject1.getJSONArray("types"));

			if (ArrayUtil.contains(types, "locality")) {
				city = jsonObject1.getString("short_name");
			}

			if (ArrayUtil.contains(types, "postal_code")) {
				zip = jsonObject1.getString("short_name");
			}

			if (ArrayUtil.contains(types, "country")) {
				String a2 = jsonObject1.getString("short_name");

				country = _countryLocalService.getCountryByA2(
					themeDisplay.getCompanyId(), a2);
			}
		}

		ServiceContext serviceContext = new ServiceContext();
		serviceContext.setUserId(themeDisplay.getUserId());

		CommerceAddress commerceAddress =
			_commerceAddressLocalService.addCommerceAddress(
				AccountEntry.class.getName(),
				commerceAccount.getCommerceAccountId(),
				commerceAccount.getName(),
				StringPool.BLANK, fullAddress, StringPool.BLANK,
				StringPool.BLANK,
				city, zip, regionId, country.getCountryId(), phoneNumber,
				CommerceAddressConstants.ADDRESS_TYPE_BILLING_AND_SHIPPING,
				serviceContext);

		JSONObject location = results.getJSONObject(
			0
		).getJSONObject(
			"geometry"
		).getJSONObject(
			"location"
		);

		Address address = _addressLocalService.getAddress(
			commerceAddress.getCommerceAddressId());

		address.setLatitude(location.getDouble("lat", 0.0));
		address.setLongitude(location.getDouble("lng", 0.0));


		_addressLocalService.updateAddress(address);
	}

	@Reference(
		target = "(component.name=com.liferay.login.web.internal.portlet.action.CreateAccountMVCActionCommand)"
	)
	protected MVCActionCommand mvcActionCommand;

	@Reference
	private AddressLocalService _addressLocalService;

	@Reference
	private AddressResolver _addressResolver;

	@Reference
	private CommerceAccountLocalService _commerceAccountLocalService;

	@Reference
	private CommerceAddressLocalService _commerceAddressLocalService;

	@Reference
	private CountryLocalService _countryLocalService;

	@Reference
	private Portal _portal;

	@Reference
	private UserLocalService _userLocalService;

}