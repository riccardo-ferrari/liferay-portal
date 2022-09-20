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

package com.liferay.analytics.settings.rest.internal.resource.v1_0;

import com.liferay.analytics.settings.rest.dto.v1_0.Channel;
import com.liferay.analytics.settings.rest.internal.dto.v1_0.converter.ChannelDTOConverter;
import com.liferay.analytics.settings.rest.internal.helper.v1_0.AnalyticsCloudClientHelper;
import com.liferay.analytics.settings.rest.resource.v1_0.ChannelResource;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.search.filter.Filter;
import com.liferay.portal.vulcan.dto.converter.DTOConverterRegistry;
import com.liferay.portal.vulcan.dto.converter.DefaultDTOConverterContext;
import com.liferay.portal.vulcan.pagination.Page;
import com.liferay.portal.vulcan.pagination.Pagination;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

/**
 * @author Riccardo Ferrari
 */
@Component(
	properties = "OSGI-INF/liferay/rest/v1_0/channel.properties",
	scope = ServiceScope.PROTOTYPE, service = ChannelResource.class
)
public class ChannelResourceImpl extends BaseChannelResourceImpl {

	@Override
	public Page<Channel> getChannelPage(
			String keywords, Filter filter, Pagination pagination)
		throws Exception {

		JSONObject channelsPageJSONObject =
			_analyticsCloudClientHelper.fetchChannelsPage(
				contextCompany.getCompanyId(), keywords,
				pagination.getPage() - 1, pagination.getPageSize());

		JSONObject embeddedJSONObject = channelsPageJSONObject.getJSONObject(
			"_embedded");

		JSONObject pageJSONObject = channelsPageJSONObject.getJSONObject(
			"page");

		return Page.of(
			JSONUtil.toList(
				embeddedJSONObject.getJSONArray("channels"),
				channel -> _channelDTOConverter.toDTO(
					new DefaultDTOConverterContext(
						false, null, dtoConverterRegistry, null,
						contextUser.getLocale(), null, contextUser),
					channel)),
			pagination, pageJSONObject.getInt("totalElements"));
	}

	@Reference
	protected DTOConverterRegistry dtoConverterRegistry;

	@Reference
	private AnalyticsCloudClientHelper _analyticsCloudClientHelper;

	@Reference
	private ChannelDTOConverter _channelDTOConverter;

}