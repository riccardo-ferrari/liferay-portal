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

package com.liferay.headless.commerce.admin.catalog.internal.util.v1_0;

import com.liferay.asset.kernel.model.AssetCategory;
import com.liferay.asset.kernel.model.AssetVocabulary;
import com.liferay.asset.kernel.service.AssetCategoryLocalService;
import com.liferay.asset.kernel.service.AssetVocabularyLocalService;
import com.liferay.headless.commerce.admin.catalog.dto.v1_0.Category;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.service.ServiceContext;

import java.util.List;

/**
 * @author Riccardo Ferrari
 */
public class CategoryUtil {

	public static AssetCategory upsertCategory(
			AssetCategoryLocalService assetCategoryLocalService,
			AssetVocabularyLocalService assetVocabularyLocalService,
			Category category, ServiceContext serviceContext)
		throws PortalException {

		List<AssetVocabulary> groupVocabularies =
			assetVocabularyLocalService.getGroupVocabularies(
				serviceContext.getScopeGroupId());

		long vocabularyId = 0;

		for (AssetVocabulary vocabulary : groupVocabularies) {
			String vocabularyTitle = vocabulary.getTitle();

			if (vocabularyTitle.equals(category.getVocabulary())) {
				vocabularyId = vocabulary.getVocabularyId();

				break;
			}
		}

		if (vocabularyId == 0) {
			AssetVocabulary assetVocabulary =
				assetVocabularyLocalService.addVocabulary(
					serviceContext.getUserId(),
					serviceContext.getScopeGroupId(), category.getVocabulary(),
					serviceContext);

			vocabularyId = assetVocabulary.getVocabularyId();
		}

		AssetCategory assetCategory = assetCategoryLocalService.fetchCategory(
			serviceContext.getScopeGroupId(), 0, category.getName(),
			vocabularyId);

		if (assetCategory == null) {
			assetCategory = assetCategoryLocalService.addCategory(
				serviceContext.getUserId(), serviceContext.getScopeGroupId(),
				category.getName(), vocabularyId, serviceContext);
		}

		return assetCategory;
	}

}