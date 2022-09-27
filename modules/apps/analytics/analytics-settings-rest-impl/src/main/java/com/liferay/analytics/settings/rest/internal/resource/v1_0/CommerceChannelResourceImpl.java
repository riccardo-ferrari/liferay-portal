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

import com.liferay.analytics.settings.rest.dto.v1_0.CommerceChannel;
import com.liferay.analytics.settings.rest.internal.dto.v1_0.converter.CommerceChannelDTOConverter;
import com.liferay.analytics.settings.rest.resource.v1_0.CommerceChannelResource;
import com.liferay.commerce.product.service.CommerceChannelService;
import com.liferay.portal.vulcan.pagination.Page;
import com.liferay.portal.vulcan.pagination.Pagination;

import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

/**
 * @author Riccardo Ferrari
 */
@Component(
	properties = "OSGI-INF/liferay/rest/v1_0/commerce-channel.properties",
	scope = ServiceScope.PROTOTYPE, service = CommerceChannelResource.class
)
public class CommerceChannelResourceImpl
	extends BaseCommerceChannelResourceImpl {

	@Override
	public Page<CommerceChannel> getCommerceChannelPage(Pagination pagination)
		throws Exception {

		List<com.liferay.commerce.product.model.CommerceChannel>
			commerceChannels = _commerceChannelService.getCommerceChannels(
				contextCompany.getCompanyId());

		return Page.of(
			transform(
				commerceChannels,
				group -> _commerceChannelDTOConverter.toDTO(group)),
			pagination, commerceChannels.size());
	}

	@Reference
	private CommerceChannelDTOConverter _commerceChannelDTOConverter;

	@Reference
	private CommerceChannelService _commerceChannelService;

}