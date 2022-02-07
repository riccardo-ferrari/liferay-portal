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

package com.liferay.analytics.dxp.entities.exporter.internal.dto.v1_0.converter;

import com.liferay.analytics.dxp.entities.exporter.dto.v1_0.Category;
import com.liferay.analytics.dxp.entities.exporter.dto.v1_0.Product;
import com.liferay.analytics.dxp.entities.exporter.dto.v1_0.Sku;
import com.liferay.analytics.dxp.entities.exporter.dto.v1_0.Status;
import com.liferay.asset.kernel.model.AssetCategory;
import com.liferay.asset.kernel.model.AssetTag;
import com.liferay.asset.kernel.model.AssetVocabulary;
import com.liferay.asset.kernel.service.AssetCategoryLocalService;
import com.liferay.asset.kernel.service.AssetTagLocalService;
import com.liferay.asset.kernel.service.AssetVocabularyLocalService;
import com.liferay.commerce.product.model.CPDefinition;
import com.liferay.commerce.product.model.CPInstance;
import com.liferay.commerce.product.model.CProduct;
import com.liferay.commerce.product.model.CommerceCatalog;
import com.liferay.commerce.product.service.CPDefinitionLocalService;
import com.liferay.commerce.product.type.CPType;
import com.liferay.commerce.product.type.CPTypeServicesTracker;
import com.liferay.headless.commerce.core.util.LanguageUtils;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.language.LanguageResources;
import com.liferay.portal.vulcan.dto.converter.DTOConverter;
import com.liferay.portal.vulcan.dto.converter.DTOConverterContext;
import com.liferay.portal.vulcan.util.TransformUtil;

import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Stream;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Riccardo Ferrari
 */
@Component(
	property = "dto.class.name=com.liferay.analytics.dxp.entities.exporter.dto.v1_0.Product",
	service = {DTOConverter.class, ProductDTOConverter.class}
)
public class ProductDTOConverter
	implements DTOConverter<CPDefinition, Product> {

	@Override
	public String getContentType() {
		return Product.class.getSimpleName();
	}

	@Override
	public CPDefinition getObject(String externalReferenceCode)
		throws Exception {

		return _cpDefinitionLocalService.fetchCPDefinition(
			GetterUtil.getLong(externalReferenceCode));
	}

	@Override
	public Product toDTO(
			DTOConverterContext dtoConverterContext, CPDefinition cpDefinition)
		throws Exception {

		if (cpDefinition == null) {
			return null;
		}

		CommerceCatalog commerceCatalog = cpDefinition.getCommerceCatalog();

		CProduct cProduct = cpDefinition.getCProduct();

		ResourceBundle resourceBundle = LanguageResources.getResourceBundle(
			dtoConverterContext.getLocale());

		String productStatusLabel = WorkflowConstants.getStatusLabel(
			cpDefinition.getStatus());

		String productStatusLabelI18n = LanguageUtil.get(
			resourceBundle,
			WorkflowConstants.getStatusLabel(cpDefinition.getStatus()));

		CPType cpType = _getCPType(cpDefinition.getProductTypeName());

		return new Product() {
			{
				active = cpDefinition.isInactive();
				catalogId = commerceCatalog.getCommerceCatalogId();
				categories = TransformUtil.transformToArray(
					_assetCategoryLocalService.getCategories(
						cpDefinition.getModelClassName(),
						cpDefinition.getCPDefinitionId()),
					assetCategory -> _toCategory(assetCategory),
					Category.class);
				createDate = cpDefinition.getCreateDate();
				//				customFields = CustomFieldsUtil.toCustomFields(
				//					dtoConverterContext.isAcceptAllLanguages(),
				//					CPDefinition.class.getName(),
				//					cpDefinition.getCPDefinitionId(),
				//					cpDefinition.getCompanyId(),
				//					dtoConverterContext.getLocale());
				description = LanguageUtils.getLanguageIdMap(
					cpDefinition.getDescriptionMap());
				displayDate = cpDefinition.getDisplayDate();
				expirationDate = cpDefinition.getExpirationDate();
				externalReferenceCode = cProduct.getExternalReferenceCode();
				id = cpDefinition.getCPDefinitionId();
				metaDescription = LanguageUtils.getLanguageIdMap(
					cpDefinition.getMetaDescriptionMap());
				metaKeyword = LanguageUtils.getLanguageIdMap(
					cpDefinition.getMetaKeywordsMap());
				metaTitle = LanguageUtils.getLanguageIdMap(
					cpDefinition.getMetaTitleMap());
				modifiedDate = cpDefinition.getModifiedDate();
				name = LanguageUtils.getLanguageIdMap(
					cpDefinition.getNameMap());
				productId = cProduct.getCProductId();
				productStatus = cpDefinition.getStatus();
				productType = cpType.getName();
				skus = TransformUtil.transformToArray(
					cpDefinition.getCPInstances(), sku -> _toSku(sku),
					Sku.class);
				tags = _getTags(cpDefinition);
				workflowStatusInfo = _toStatus(
					cpDefinition.getStatus(), productStatusLabel,
					productStatusLabelI18n);
			}
		};
	}

	private CPType _getCPType(String name) {
		return _cpTypeServicesTracker.getCPType(name);
	}

	private String[] _getTags(CPDefinition cpDefinition) {
		List<AssetTag> assetEntryAssetTags = _assetTagLocalService.getTags(
			cpDefinition.getModelClassName(), cpDefinition.getCPDefinitionId());

		Stream<AssetTag> stream = assetEntryAssetTags.stream();

		return stream.map(
			AssetTag::getName
		).toArray(
			String[]::new
		);
	}

	private Category _toCategory(AssetCategory assetCategory) {
		return new Category() {
			{
				externalReferenceCode =
					assetCategory.getExternalReferenceCode();
				id = assetCategory.getCategoryId();
				name = assetCategory.getName();
				siteId = assetCategory.getGroupId();

				setVocabulary(
					() -> {
						AssetVocabulary assetVocabulary =
							_assetVocabularyLocalService.fetchAssetVocabulary(
								assetCategory.getVocabularyId());

						if (assetVocabulary == null) {
							return null;
						}

						return assetVocabulary.getName();
					});
			}
		};
	}

	private Sku _toSku(CPInstance cpInstance) {
		return new Sku() {
			{
				cost = cpInstance.getCost();
				displayDate = cpInstance.getDisplayDate();
				expirationDate = cpInstance.getExpirationDate();
				externalReferenceCode = cpInstance.getExternalReferenceCode();
				gtin = cpInstance.getGtin();
				id = cpInstance.getCPInstanceId();
				manufacturerPartNumber = cpInstance.getManufacturerPartNumber();
				price = cpInstance.getPrice();
				sku = cpInstance.getSku();
			}
		};
	}

	private Status _toStatus(
		int statusCode, String productStatusLabel,
		String productStatusLabelI18n) {

		return new Status() {
			{
				code = statusCode;
				label = productStatusLabel;
				label_i18n = productStatusLabelI18n;
			}
		};
	}

	@Reference
	private AssetCategoryLocalService _assetCategoryLocalService;

	@Reference
	private AssetTagLocalService _assetTagLocalService;

	@Reference
	private AssetVocabularyLocalService _assetVocabularyLocalService;

	@Reference
	private CPDefinitionLocalService _cpDefinitionLocalService;

	@Reference
	private CPTypeServicesTracker _cpTypeServicesTracker;

}