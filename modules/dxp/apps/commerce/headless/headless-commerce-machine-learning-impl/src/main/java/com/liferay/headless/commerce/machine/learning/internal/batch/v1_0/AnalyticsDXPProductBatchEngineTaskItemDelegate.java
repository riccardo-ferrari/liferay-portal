/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of the Liferay Enterprise
 * Subscription License ("License"). You may not use this file except in
 * compliance with the License. You can obtain a copy of the License by
 * contacting Liferay, Inc. See the License for the specific language governing
 * permissions and limitations under the License, including but not limited to
 * distribution rights of the Software.
 *
 *
 *
 */

package com.liferay.headless.commerce.machine.learning.internal.batch.v1_0;

import com.liferay.batch.engine.BaseBatchEngineTaskItemDelegate;
import com.liferay.batch.engine.BatchEngineTaskItemDelegate;
import com.liferay.batch.engine.pagination.Page;
import com.liferay.batch.engine.pagination.Pagination;
import com.liferay.commerce.product.model.CPDefinition;
import com.liferay.commerce.product.model.CommerceCatalog;
import com.liferay.commerce.product.service.CommerceCatalogLocalService;
import com.liferay.headless.commerce.machine.learning.dto.v1_0.Product;
import com.liferay.headless.commerce.machine.learning.internal.dto.v1_0.converter.ProductDTOConverter;
import com.liferay.headless.commerce.machine.learning.internal.odata.entity.v1_0.ProductEntityModel;
import com.liferay.portal.kernel.search.Field;
import com.liferay.portal.kernel.search.Sort;
import com.liferay.portal.kernel.search.filter.Filter;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.odata.entity.EntityModel;
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
	enabled = false, immediate = true,
	property = "batch.engine.task.item.delegate.name=analytics-dxp-product",
	service = BatchEngineTaskItemDelegate.class
)
public class AnalyticsDXPProductBatchEngineTaskItemDelegate
	extends BaseBatchEngineTaskItemDelegate<Product> {

	@Override
	public EntityModel getEntityModel(Map<String, List<String>> multivaluedMap)
		throws Exception {

		return new ProductEntityModel();
	}

	@Override
	public Class<Product> getItemClass() {
		return Product.class;
	}

	@Override
	public Page<Product> read(
			Filter filter, Pagination pagination, Sort[] sorts,
			Map<String, Serializable> parameters, String search)
		throws Exception {

		com.liferay.portal.vulcan.pagination.Pagination vulcanPagination =
			com.liferay.portal.vulcan.pagination.Pagination.of(
				pagination.getPage(), pagination.getPageSize());

		com.liferay.portal.vulcan.pagination.Page<Product> productsPage =
			SearchUtil.search(
				null, booleanQuery -> booleanQuery.getPreBooleanFilter(),
				filter, CPDefinition.class.getName(), search, vulcanPagination,
				queryConfig -> queryConfig.setSelectedFieldNames(
					Field.ENTRY_CLASS_PK),
				searchContext -> {
					searchContext.setCompanyId(contextCompany.getCompanyId());

					long[] commerceCatalogGroupIds =
						_getCommerceCatalogGroupIds(
							contextCompany.getCompanyId());

					if ((commerceCatalogGroupIds != null) &&
						(commerceCatalogGroupIds.length > 0)) {

						searchContext.setGroupIds(commerceCatalogGroupIds);
					}

					searchContext.setAttribute(
						Field.STATUS, WorkflowConstants.STATUS_ANY);

					if (contextUser.getLocale() != null) {
						searchContext.setLocale(contextUser.getLocale());
					}
				},
				sorts,
				document -> {
					long productId = GetterUtil.getLong(
						document.get(Field.ENTRY_CLASS_PK));

					return _toProduct(productId, contextUser.getLocale());
				});

		return Page.of(
			productsPage.getItems(),
			Pagination.of(
				(int)productsPage.getPage(), (int)productsPage.getPageSize()),
			productsPage.getTotalCount());
	}

	private long[] _getCommerceCatalogGroupIds(long companyId)
		throws Exception {

		List<CommerceCatalog> commerceCatalogs =
			_commerceCatalogLocalService.search(companyId);

		Stream<CommerceCatalog> stream = commerceCatalogs.stream();

		return stream.mapToLong(
			CommerceCatalog::getGroupId
		).toArray();
	}

	private Product _toProduct(long productId, Locale locale) throws Exception {
		return _productDTOConverter.toDTO(
			new DefaultDTOConverterContext(
				false, null, _dtoConverterRegistry, productId, locale, null,
				contextUser));
	}

	@Reference
	private CommerceCatalogLocalService _commerceCatalogLocalService;

	@Reference
	private DTOConverterRegistry _dtoConverterRegistry;

	@Reference
	private ProductDTOConverter _productDTOConverter;

}