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

package com.liferay.headless.commerce.admin.catalog.internal.resource.v1_0;

import com.liferay.commerce.product.model.CPDefinition;
import com.liferay.commerce.product.model.CommerceChannel;
import com.liferay.commerce.product.model.CommerceChannelRel;
import com.liferay.commerce.product.service.CPDefinitionService;
import com.liferay.commerce.product.service.CommerceChannelRelService;
import com.liferay.headless.commerce.admin.catalog.dto.v1_0.Channel;
import com.liferay.headless.commerce.admin.catalog.dto.v1_0.Product;
import com.liferay.headless.commerce.admin.catalog.resource.v1_0.ChannelResource;
import com.liferay.portal.vulcan.fields.NestedField;
import com.liferay.portal.vulcan.fields.NestedFieldId;
import com.liferay.portal.vulcan.fields.NestedFieldSupport;
import com.liferay.portal.vulcan.pagination.Page;
import com.liferay.portal.vulcan.pagination.Pagination;

import java.util.Collections;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

/**
 * @author Zoltán Takács
 */
@Component(
	enabled = false,
	properties = "OSGI-INF/liferay/rest/v1_0/channel.properties",
	scope = ServiceScope.PROTOTYPE,
	service = {ChannelResource.class, NestedFieldSupport.class}
)
public class ChannelResourceImpl
	extends BaseChannelResourceImpl implements NestedFieldSupport {

	@Override
	public void deleteProductChannel(Long id) throws Exception {
		_commerceChannelRelService.deleteCommerceChannelRel(id);
	}

	@Override
	public Channel getProductChannel(Long id) throws Exception {
		return _toChannel(_commerceChannelRelService.getCommerceChannelRel(id));
	}

	@NestedField(parentClass = Product.class, value = "channels")
	@Override
	public Page<Channel> getProductIdChannelsPage(
			@NestedFieldId(value = "productId") Long id, Pagination pagination)
		throws Exception {

		CPDefinition cpDefinition =
			_cpDefinitionService.fetchCPDefinitionByCProductId(id);

		if (cpDefinition == null) {
			return Page.of(Collections.emptyList());
		}

		int commerceChannelRelsCount =
			_commerceChannelRelService.getCommerceChannelRelsCount(
				CPDefinition.class.getName(), cpDefinition.getCPDefinitionId());

		return Page.of(
			transform(
				_commerceChannelRelService.getCommerceChannelRels(
					CPDefinition.class.getName(),
					cpDefinition.getCPDefinitionId(), null,
					pagination.getStartPosition(), pagination.getEndPosition()),
				commerceChannelRel -> _toChannel(commerceChannelRel)),
			pagination, commerceChannelRelsCount);
	}

	private Channel _toChannel(CommerceChannelRel commerceChannelRel)
		throws Exception {

		CommerceChannel commerceChannel =
			commerceChannelRel.getCommerceChannel();

		return new Channel() {
			{
				currencyCode = commerceChannel.getCommerceCurrencyCode();
				externalReferenceCode =
					commerceChannel.getExternalReferenceCode();
				id = commerceChannel.getCommerceChannelId();
				name = commerceChannel.getName();
				productChannelId = commerceChannelRel.getCommerceChannelRelId();
				type = commerceChannel.getType();
			}
		};
	}

	@Reference
	private CommerceChannelRelService _commerceChannelRelService;

	@Reference
	private CPDefinitionService _cpDefinitionService;

}