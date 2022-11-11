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
package com.liferay.pizza.conneciton.helper.internal.util;

import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Http;
import com.liferay.portal.kernel.util.HttpComponentsUtil;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Riccardo Ferrari
 */
@Component(immediate = true, service = {AddressResolver.class})
public class AddressResolver {

	public JSONObject resolveAddress(String fullAddress) throws Exception {
		String url = HttpComponentsUtil.addParameter(
			_GOOGLE_API_URL, "address", fullAddress);

		url = HttpComponentsUtil.addParameter(
			url, "key", "AIzaSyAGoWEged7A8Dyoe-A1j1cuTfDMmA_vQ8E");

		String content = _http.URLtoString(url);

		return JSONFactoryUtil.createJSONObject(content);
	}

	private static final String _GOOGLE_API_URL =
		"https://maps.googleapis.com/maps/api/geocode/json";

	private static final Log _log = LogFactoryUtil.getLog(
		AddressResolver.class);

	@Reference
	private Http _http;

}