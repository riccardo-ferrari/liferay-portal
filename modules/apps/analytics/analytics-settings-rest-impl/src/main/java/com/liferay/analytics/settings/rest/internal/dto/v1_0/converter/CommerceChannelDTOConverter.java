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

package com.liferay.analytics.settings.rest.internal.dto.v1_0.converter;

import com.liferay.analytics.settings.rest.dto.v1_0.CommerceChannel;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.util.UnicodeProperties;
import com.liferay.portal.vulcan.dto.converter.DTOConverter;
import com.liferay.portal.vulcan.dto.converter.DTOConverterContext;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Riccardo Ferrari
 */
@Component(
	property = "dto.class.name=com.liferay.portal.kernel.model.Group",
	service = {CommerceChannelDTOConverter.class, DTOConverter.class}
)
public class CommerceChannelDTOConverter
	implements DTOConverter
		<com.liferay.commerce.product.model.CommerceChannel, CommerceChannel> {

	@Override
	public String getContentType() {
		return CommerceChannel.class.getSimpleName();
	}

	@Override
	public CommerceChannel toDTO(
			DTOConverterContext dtoConverterContext,
			com.liferay.commerce.product.model.CommerceChannel commerceChannel)
		throws Exception {

		Group commerceChannelGroup = commerceChannel.getGroup();

		UnicodeProperties typeSettingsUnicodeProperties =
			commerceChannelGroup.getTypeSettingsProperties();

		Group siteGroup = _groupLocalService.fetchGroup(
			commerceChannel.getSiteGroupId());

		return new CommerceChannel() {
			{
				channelName = typeSettingsUnicodeProperties.getProperty(
					"analyticsChannelId");
				id = commerceChannelGroup.getGroupId();
				name = commerceChannelGroup.getDescriptiveName();
				relatedSite = siteGroup.getDescriptiveName();
			}
		};
	}

	@Reference
	private GroupLocalService _groupLocalService;

}