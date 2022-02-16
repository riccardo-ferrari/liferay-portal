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

package com.liferay.headless.commerce.machine.learning.internal.dto.v1_0.converter;

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
import com.liferay.headless.commerce.machine.learning.dto.v1_0.Category;
import com.liferay.headless.commerce.machine.learning.dto.v1_0.Product;
import com.liferay.headless.commerce.machine.learning.dto.v1_0.Sku;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.vulcan.dto.converter.DTOConverter;
import com.liferay.portal.vulcan.dto.converter.DTOConverterContext;
import com.liferay.portal.vulcan.util.TransformUtil;

import java.util.List;
import java.util.stream.Stream;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Riccardo Ferrari
 */
@Component(
	enabled = false,
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

				//			  customFields = CustomFieldsUtil.toCustomFields(
				//					  dtoConverterContext.isAcceptAllLanguages(),
				//					  CPDefinition.class.getName(),
				//					  cpDefinition.getCPDefinitionId(),
				//					  cpDefinition.getCompanyId(),
				//					  dtoConverterContext.getLocale());

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