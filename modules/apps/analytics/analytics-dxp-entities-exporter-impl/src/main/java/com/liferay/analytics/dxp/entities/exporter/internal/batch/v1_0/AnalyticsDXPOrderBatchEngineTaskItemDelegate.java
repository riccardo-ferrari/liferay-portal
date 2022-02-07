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

package com.liferay.analytics.dxp.entities.exporter.internal.batch.v1_0;

import com.liferay.analytics.dxp.entities.exporter.dto.v1_0.Order;
import com.liferay.analytics.dxp.entities.exporter.internal.dto.v1_0.converter.OrderDTOConverter;
import com.liferay.batch.engine.BaseBatchEngineTaskItemDelegate;
import com.liferay.batch.engine.BatchEngineTaskItemDelegate;
import com.liferay.batch.engine.pagination.Page;
import com.liferay.batch.engine.pagination.Pagination;
import com.liferay.commerce.model.CommerceOrder;
import com.liferay.commerce.product.model.CommerceChannel;
import com.liferay.commerce.product.service.CommerceChannelLocalService;
import com.liferay.petra.function.UnsafeConsumer;
import com.liferay.portal.kernel.search.Field;
import com.liferay.portal.kernel.search.SearchContext;
import com.liferay.portal.kernel.search.Sort;
import com.liferay.portal.kernel.search.filter.Filter;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.vulcan.dto.converter.DTOConverterRegistry;
import com.liferay.portal.vulcan.dto.converter.DefaultDTOConverterContext;
import com.liferay.portal.vulcan.util.SearchUtil;

import java.io.Serializable;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Riccardo Ferrari
 */
@Component(
	immediate = true,
	property = "batch.engine.task.item.delegate.name=analytics-dxp-order",
	service = BatchEngineTaskItemDelegate.class
)
public class AnalyticsDXPOrderBatchEngineTaskItemDelegate
	extends BaseBatchEngineTaskItemDelegate<Order> {

	@Override
	public Class<Order> getItemClass() {
		return Order.class;
	}

	@Override
	public Page<Order> read(
			Filter filter, Pagination pagination, Sort[] sorts,
			Map<String, Serializable> parameters, String search)
		throws Exception {

		com.liferay.portal.vulcan.pagination.Pagination vulcanPagination =
			com.liferay.portal.vulcan.pagination.Pagination.of(
				pagination.getPage(), pagination.getPageSize());

		com.liferay.portal.vulcan.pagination.Page<Order> ordersPage =
			SearchUtil.search(
				null, booleanQuery -> booleanQuery.getPreBooleanFilter(),
				filter, CommerceOrder.class.getName(), search, vulcanPagination,
				queryConfig -> queryConfig.setSelectedFieldNames(
					Field.ENTRY_CLASS_PK),
				new UnsafeConsumer() {

					public void accept(Object object) throws Exception {
						SearchContext searchContext = (SearchContext)object;

						searchContext.setCompanyId(
							contextCompany.getCompanyId());

						long[] commerceChannelGroupIds =
							_getCommerceChannelGroupIds(
								contextCompany.getCompanyId());

						if ((commerceChannelGroupIds != null) &&
							(commerceChannelGroupIds.length > 0)) {

							searchContext.setGroupIds(commerceChannelGroupIds);
						}
					}

				},
				sorts,
				document -> {
					long orderId = GetterUtil.getLong(
						document.get(Field.ENTRY_CLASS_PK));

					return _toOrder(orderId, contextUser.getLocale());
				});

		return Page.of(
			ordersPage.getItems(),
			Pagination.of(
				(int)ordersPage.getPage(), (int)ordersPage.getPageSize()),
			ordersPage.getTotalCount());
	}

	private long[] _getCommerceChannelGroupIds(long companyId)
		throws Exception {

		List<CommerceChannel> commerceChannels =
			_commerceChannelLocalService.search(companyId);

		Stream<CommerceChannel> stream = commerceChannels.stream();

		return stream.mapToLong(
			CommerceChannel::getGroupId
		).toArray();
	}

	private Order _toOrder(long orderId, Locale locale) throws Exception {
		return _orderDTOConverter.toDTO(
			new DefaultDTOConverterContext(
				false, null, _dtoConverterRegistry, orderId, locale, null,
				contextUser));
	}

	@Reference
	private CommerceChannelLocalService _commerceChannelLocalService;

	@Reference
	private DTOConverterRegistry _dtoConverterRegistry;

	@Reference
	private OrderDTOConverter _orderDTOConverter;

}