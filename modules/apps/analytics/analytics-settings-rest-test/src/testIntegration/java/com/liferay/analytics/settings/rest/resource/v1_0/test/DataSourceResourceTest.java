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

package com.liferay.analytics.settings.rest.resource.v1_0.test;

import com.liferay.analytics.settings.rest.client.dto.v1_0.DataSourceToken;
import com.liferay.analytics.settings.rest.client.http.HttpInvoker;
import com.liferay.analytics.settings.rest.internal.helper.v1_0.AnalyticsCloudClientHelper;
import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;

import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

/**
 * @author Riccardo Ferrari
 */
@RunWith(Arquillian.class)
public class DataSourceResourceTest extends BaseDataSourceResourceTestCase {

	@Override
	public void testPostDataSource() throws Exception {
		DataSourceToken dataSourceToken = new DataSourceToken();

		dataSourceToken.setToken(RandomTestUtil.randomString());

		Mockito.when(
			_analyticsCloudClientHelper.connectDataSource(
				Mockito.anyLong(), Mockito.anyString())
		).thenReturn(
			JSONUtil.put(
				"liferayAnalyticsEndpointURL", "osbasahpublisher.lfr.cloud"
			).put(
				"liferayAnalyticsURL", "analytics.lfr.cloud"
			).put(
				"liferayAnalyticsProjectId", "asah123456789"
			).put(
				"liferayAnalyticsFaroBackendSecuritySignature",
				"<secret-signature>"
			).put(
				"liferayAnalyticsDataSourceId", "574745902859054171"
			).put(
				"publicKey", "<some-public-key>"
			).put(
				"liferayAnalyticsFaroBackendURL", "osbasahbackend.lfr.cloud"
			)
		);

		HttpInvoker.HttpResponse httpResponse =
			dataSourceResource.postDataSourceHttpResponse(dataSourceToken);

		assertHttpResponseStatusCode(204, httpResponse);
	}

	private AnalyticsCloudClientHelper _analyticsCloudClientHelper = Mockito.mock(AnalyticsCloudClientHelper.class);
}