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

package com.liferay.analytics.batch.exportimport.model.listener;

import com.liferay.analytics.message.storage.service.AnalyticsDeleteMessageLocalService;
import com.liferay.analytics.settings.configuration.AnalyticsConfigurationTracker;
import com.liferay.portal.kernel.exception.ModelListenerException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.BaseModel;
import com.liferay.portal.kernel.model.BaseModelListener;
import com.liferay.portal.kernel.model.ShardedModel;
import com.liferay.portal.kernel.service.UserLocalService;

import org.osgi.service.component.annotations.Reference;

/**
 * @author Marcos Martins
 * @author Riccardo Ferrari
 */
public abstract class BaseAnalyticsDeleteMessageModelListener
	<T extends BaseModel<T>>
		extends BaseModelListener<T> {

	@Override
	public void onBeforeRemove(T model) throws ModelListenerException {
		if (!analyticsConfigurationTracker.isActive() || !isTrack(model)) {
			return;
		}

		ShardedModel shardedModel = (ShardedModel)model;

		long companyId = shardedModel.getCompanyId();

		try {
			analyticsDeleteMessageLocalService.addAnalyticsDeleteMessage(
				companyId, model.getModelClassName(),
				(long)model.getPrimaryKeyObj(),
				userLocalService.getDefaultUserId(companyId));
		}
		catch (Exception exception) {
			if (_log.isWarnEnabled()) {
				_log.warn(
					"Unable to add analytics delete message for model " +
						model.getModelClassName(),
					exception);
			}
		}
	}

	protected boolean isTrack(T model) {
		return true;
	}

	@Reference
	protected AnalyticsConfigurationTracker analyticsConfigurationTracker;

	@Reference
	protected AnalyticsDeleteMessageLocalService
		analyticsDeleteMessageLocalService;

	@Reference
	protected UserLocalService userLocalService;

	private static final Log _log = LogFactoryUtil.getLog(
		BaseAnalyticsDeleteMessageModelListener.class);

}