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

package com.liferay.analytics.dxp.entities.exporter.internal.dispatch.executor.helper;

import com.liferay.analytics.dxp.entities.exporter.internal.batch.engine.mapper.BatchEngineTaskItemDelegateResourceMapper;
import com.liferay.analytics.dxp.entities.exporter.internal.batch.helper.BatchEngineExportTaskHelper;
import com.liferay.analytics.dxp.entities.exporter.internal.batch.helper.BatchEngineImportTaskHelper;
import com.liferay.analytics.dxp.entities.exporter.internal.batch.helper.BatchEngineTaskHelperFactory;
import com.liferay.analytics.message.sender.client.AnalyticsBatchClient;
import com.liferay.dispatch.executor.DispatchTaskExecutorOutput;
import com.liferay.dispatch.executor.DispatchTaskStatus;
import com.liferay.dispatch.model.DispatchLog;
import com.liferay.dispatch.model.DispatchTrigger;
import com.liferay.dispatch.service.DispatchLogLocalService;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.Date;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Riccardo Ferrari
 */
@Component(
	immediate = true, service = AnalyticsDispatchTaskExecutorHelper.class
)
public class AnalyticsDispatchTaskExecutorHelper {

	public void downloadResources(
			DispatchTrigger dispatchTrigger,
			DispatchTaskExecutorOutput dispatchTaskExecutorOutput,
			BatchEngineTaskItemDelegateResourceMapper[]
				batchEngineTaskItemDelegateResourceMappers)
		throws IOException, PortalException {

		DispatchLog dispatchLog =
			dispatchLogLocalService.fetchLatestDispatchLog(
				dispatchTrigger.getDispatchTriggerId(),
				DispatchTaskStatus.IN_PROGRESS);

		for (BatchEngineTaskItemDelegateResourceMapper
				batchEngineTaskItemDelegateResourceMapper :
					batchEngineTaskItemDelegateResourceMappers) {

			_updateDispatchLog(
				dispatchLog.getDispatchLogId(), dispatchTaskExecutorOutput,
				String.format(
					"Checking updates for resource %s",
					batchEngineTaskItemDelegateResourceMapper.
						getResourceName()));

			Date resourceLastModifiedDate = dispatchTrigger.getCreateDate();

			DispatchLog latestSuccessfulDispatchLog =
				dispatchLogLocalService.fetchLatestDispatchLog(
					dispatchTrigger.getDispatchTriggerId(),
					DispatchTaskStatus.SUCCESSFUL);

			if (latestSuccessfulDispatchLog != null) {
				resourceLastModifiedDate =
					latestSuccessfulDispatchLog.getEndDate();
			}

			File resourceFile = _analyticsBatchClient.downloadResource(
				dispatchTrigger.getCompanyId(), resourceLastModifiedDate,
				batchEngineTaskItemDelegateResourceMapper.getResourceName());

			if (resourceFile == null) {
				if (_log.isDebugEnabled()) {
					_log.debug(
						String.format(
							"No resource was updated since: %s",
							resourceLastModifiedDate));
				}

				_updateDispatchLog(
					dispatchLog.getDispatchLogId(), dispatchTaskExecutorOutput,
					String.format(
						"No resource was updated since: %s",
						resourceLastModifiedDate));
			}
			else {
				_downloadResource(
					dispatchTrigger, dispatchLog, dispatchTaskExecutorOutput,
					batchEngineTaskItemDelegateResourceMapper, resourceFile);
			}
		}
	}

	public void uploadResources(
			DispatchTrigger dispatchTrigger,
			DispatchTaskExecutorOutput dispatchTaskExecutorOutput,
			BatchEngineTaskItemDelegateResourceMapper[]
				batchEngineTaskItemDelegateResourceMappers)
		throws IOException, PortalException {

		DispatchLog dispatchLog =
			dispatchLogLocalService.fetchLatestDispatchLog(
				dispatchTrigger.getDispatchTriggerId(),
				DispatchTaskStatus.IN_PROGRESS);

		for (BatchEngineTaskItemDelegateResourceMapper
				batchEngineTaskItemDelegateResourceMapper :
					batchEngineTaskItemDelegateResourceMappers) {

			_uploadResource(
				dispatchTrigger, dispatchLog, dispatchTaskExecutorOutput,
				batchEngineTaskItemDelegateResourceMapper);
		}
	}

	protected static final DateFormat dateFormat = new SimpleDateFormat(
		"yyyy-MM-dd'T'HH:mm:ss.SSSZ");

	@Reference
	protected DispatchLogLocalService dispatchLogLocalService;

	private DispatchTaskExecutorOutput _downloadResource(
			DispatchTrigger dispatchTrigger, DispatchLog dispatchLog,
			DispatchTaskExecutorOutput dispatchTaskExecutorOutput,
			BatchEngineTaskItemDelegateResourceMapper
				batchEngineTaskItemDelegateResourceMapper,
			File resourceFile)
		throws IOException, PortalException {

		String resourceName =
			batchEngineTaskItemDelegateResourceMapper.getResourceName();

		_updateDispatchLog(
			dispatchLog.getDispatchLogId(), dispatchTaskExecutorOutput,
			"Importing resource " + resourceName);

		BatchEngineImportTaskHelper batchEngineImportTaskHelper =
			_batchEngineTaskHelperFactory.getBatchEngineImportTaskHelper(
				batchEngineTaskItemDelegateResourceMapper.
					getBatchEngineTaskItemDelegate(),
				dispatchTrigger.getCompanyId(),
				batchEngineTaskItemDelegateResourceMapper.getFieldMapping(),
				resourceFile, resourceName, dispatchTrigger.getUserId());

		if (batchEngineImportTaskHelper.execute()) {
			if (_log.isDebugEnabled()) {
				_log.debug("Importing resource complete for " + resourceName);
			}

			batchEngineImportTaskHelper.clean();
		}
		else {
			throw new PortalException(
				"Importing resource failed for: " + resourceName);
		}

		return dispatchTaskExecutorOutput;
	}

	private DispatchTaskExecutorOutput _updateDispatchLog(
			long dispatchLogId,
			DispatchTaskExecutorOutput dispatchTaskExecutorOutput,
			String message)
		throws PortalException {

		StringBundler sb = new StringBundler(5);

		if (dispatchTaskExecutorOutput.getOutput() != null) {
			sb.append(dispatchTaskExecutorOutput.getOutput());
		}

		sb.append(dateFormat.format(new Date()));
		sb.append(StringPool.SPACE);
		sb.append(message);
		sb.append(StringPool.NEW_LINE);

		dispatchTaskExecutorOutput.setOutput(sb.toString());

		dispatchLogLocalService.updateDispatchLog(
			dispatchLogId, new Date(), dispatchTaskExecutorOutput.getError(),
			dispatchTaskExecutorOutput.getOutput(),
			DispatchTaskStatus.IN_PROGRESS);

		return dispatchTaskExecutorOutput;
	}

	private DispatchTaskExecutorOutput _uploadResource(
			DispatchTrigger dispatchTrigger, DispatchLog dispatchLog,
			DispatchTaskExecutorOutput dispatchTaskExecutorOutput,
			BatchEngineTaskItemDelegateResourceMapper
				batchEngineTaskItemDelegateResourceMapper)
		throws IOException, PortalException {

		String resourceName =
			batchEngineTaskItemDelegateResourceMapper.getResourceName();

		_updateDispatchLog(
			dispatchLog.getDispatchLogId(), dispatchTaskExecutorOutput,
			"Exporting resource: " + resourceName);

		BatchEngineExportTaskHelper batchEngineExportTaskHelper =
			_batchEngineTaskHelperFactory.getBatchEngineExportTaskHelper(
				batchEngineTaskItemDelegateResourceMapper.
					getBatchEngineTaskItemDelegate(),
				dispatchTrigger.getCompanyId(), resourceName,
				dispatchTrigger.getUserId());

		if (batchEngineExportTaskHelper.execute()) {
			if (_log.isDebugEnabled()) {
				_log.debug("Exporting resource complete for: " + resourceName);
			}

			_updateDispatchLog(
				dispatchLog.getDispatchLogId(), dispatchTaskExecutorOutput,
				"Exporting resource complete for: " + resourceName);

			_updateDispatchLog(
				dispatchLog.getDispatchLogId(), dispatchTaskExecutorOutput,
				"Uploading resource: " + resourceName);

			InputStream inputStream =
				batchEngineExportTaskHelper.getContentInputStream();

			_analyticsBatchClient.uploadResource(
				dispatchTrigger.getCompanyId(), inputStream, resourceName);

			inputStream.close();

			batchEngineExportTaskHelper.clean();

			_updateDispatchLog(
				dispatchLog.getDispatchLogId(), dispatchTaskExecutorOutput,
				"Uploading resource complete for: " + resourceName);
		}
		else {
			throw new PortalException(
				"Exporting resource failed for: " + resourceName);
		}

		return dispatchTaskExecutorOutput;
	}

	private static final Log _log = LogFactoryUtil.getLog(
		AnalyticsDispatchTaskExecutorHelper.class);

	@Reference
	private AnalyticsBatchClient _analyticsBatchClient;

	@Reference
	private BatchEngineTaskHelperFactory _batchEngineTaskHelperFactory;

}