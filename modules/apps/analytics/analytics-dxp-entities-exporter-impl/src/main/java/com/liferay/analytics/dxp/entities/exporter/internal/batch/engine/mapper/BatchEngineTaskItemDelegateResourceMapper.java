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

package com.liferay.analytics.dxp.entities.exporter.internal.batch.engine.mapper;

import java.util.Map;

/**
 * @author Riccardo Ferrari
 */
public class BatchEngineTaskItemDelegateResourceMapper {

	public BatchEngineTaskItemDelegateResourceMapper(
		String resourceName, Map<String, String> fieldMapping,
		String batchEngineTaskItemDelegate) {

		_resourceName = resourceName;
		_fieldMapping = fieldMapping;
		_batchEngineTaskItemDelegate = batchEngineTaskItemDelegate;
	}

	public String getBatchEngineTaskItemDelegate() {
		return _batchEngineTaskItemDelegate;
	}

	public Map<String, String> getFieldMapping() {
		return _fieldMapping;
	}

	public String getResourceName() {
		return _resourceName;
	}

	private final String _batchEngineTaskItemDelegate;
	private final Map<String, String> _fieldMapping;
	private final String _resourceName;

}