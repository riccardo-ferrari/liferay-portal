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

package com.liferay.headless.batch.engine.internal.resource.v1_0;

import com.liferay.batch.engine.BatchEngineTaskExecuteStatus;
import com.liferay.batch.engine.BatchEngineTaskExecutor;
import com.liferay.batch.engine.BatchEngineTaskOperation;
import com.liferay.batch.engine.ItemClassRegistry;
import com.liferay.batch.engine.configuration.BatchEngineTaskConfiguration;
import com.liferay.batch.engine.model.BatchEngineTask;
import com.liferay.batch.engine.service.BatchEngineTaskLocalService;
import com.liferay.headless.batch.engine.dto.v1_0.ExportTask;
import com.liferay.headless.batch.engine.resource.v1_0.ExportTaskResource;
import com.liferay.petra.executor.PortalExecutorManager;
import com.liferay.petra.io.StreamUtil;
import com.liferay.portal.configuration.metatype.bnd.util.ConfigurableUtil;
import com.liferay.portal.kernel.util.StringUtil;

import java.io.Serializable;

import java.sql.Blob;
import java.sql.SQLException;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

/**
 * @author Ivica Cardic
 */
@Component(
	configurationPid = "com.liferay.batch.engine.configuration.BatchEngineTaskConfiguration",
	properties = "OSGI-INF/liferay/rest/v1_0/export-task.properties",
	property = "batch.engine=true", scope = ServiceScope.PROTOTYPE,
	service = ExportTaskResource.class
)
public class ExportTaskResourceImpl extends BaseExportTaskResourceImpl {

	@Activate
	public void activate(Map<String, Object> properties) {
		BatchEngineTaskConfiguration batchEngineTaskConfiguration =
			ConfigurableUtil.createConfigurable(
				BatchEngineTaskConfiguration.class, properties);

		_batchSize = batchEngineTaskConfiguration.exportBatchSize();

		if (_batchSize <= 0) {
			_batchSize = 1;
		}
	}

	@Override
	public ExportTask getExportTask(Long exportTaskId) throws Exception {
		return _toExportTask(
			_batchEngineTaskLocalService.getBatchEngineTask(exportTaskId));
	}

	@GET
	@Path("/export-task/{exportTaskId}/content/")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response getExportTaskContent(
			@PathParam("exportTaskId") Long exportTaskId)
		throws Exception {

		BatchEngineTask batchEngineTask =
			_batchEngineTaskLocalService.getBatchEngineTask(exportTaskId);

		BatchEngineTaskExecuteStatus batchEngineTaskExecuteStatus =
			BatchEngineTaskExecuteStatus.valueOf(
				batchEngineTask.getExecuteStatus());

		if (batchEngineTaskExecuteStatus ==
				BatchEngineTaskExecuteStatus.COMPLETED) {

			Blob content = batchEngineTask.getContent();

			StreamingOutput stream = outputStream -> {
				try {
					StreamUtil.transfer(
						content.getBinaryStream(), outputStream);
				}
				catch (SQLException sqle) {
					throw new WebApplicationException(sqle);
				}
			};

			return Response.ok(
				stream
			).header(
				"content-disposition",
				"attachment; filename=export." +
					StringUtil.toLowerCase(batchEngineTask.getContentType())
			).build();
		}

		return Response.status(
			Response.Status.NOT_FOUND
		).build();
	}

	@Override
	public ExportTask postExportTask(
			String className, String contentType, String version,
			String callbackURL)
		throws Exception {

		Class<?> clazz = _itemClassRegistry.getItemClass(className);

		if (clazz == null) {
			throw new IllegalArgumentException(
				"Unknown class name: " + className);
		}

		ExecutorService executorService =
			_portalExecutorManager.getPortalExecutor(
				ExportTaskResourceImpl.class.getName());

		Map<String, Serializable> parameters = new HashMap<>();

		MultivaluedMap<String, String> queryParameters =
			contextUriInfo.getQueryParameters();

		for (Map.Entry<String, List<String>> entry :
				queryParameters.entrySet()) {

			List<String> values = entry.getValue();

			if (!values.isEmpty()) {
				parameters.put(entry.getKey(), values.get(0));
			}
		}

		BatchEngineTask batchEngineTask =
			_batchEngineTaskLocalService.addBatchEngineTask(
				contextCompany.getCompanyId(), contextUser.getUserId(),
				_batchSize, callbackURL, className, null,
				StringUtil.upperCase(contentType),
				BatchEngineTaskExecuteStatus.INITIAL.name(),
				Collections.emptyMap(), BatchEngineTaskOperation.READ.name(),
				parameters, version);

		executorService.submit(
			() -> _batchEngineTaskExecutor.execute(batchEngineTask));

		return _toExportTask(batchEngineTask);
	}

	private ExportTask _toExportTask(BatchEngineTask batchEngineTask) {
		return new ExportTask() {
			{
				className = batchEngineTask.getClassName();
				contentType = batchEngineTask.getContentType();
				endTime = batchEngineTask.getEndTime();
				errorMessage = batchEngineTask.getErrorMessage();
				executeStatus = ExportTask.ExecuteStatus.valueOf(
					batchEngineTask.getExecuteStatus());
				id = batchEngineTask.getBatchEngineTaskId();
				startTime = batchEngineTask.getStartTime();
				version = batchEngineTask.getVersion();
			}
		};
	}

	@Reference
	private BatchEngineTaskExecutor _batchEngineTaskExecutor;

	@Reference
	private BatchEngineTaskLocalService _batchEngineTaskLocalService;

	private int _batchSize;

	@Reference
	private ItemClassRegistry _itemClassRegistry;

	@Reference
	private PortalExecutorManager _portalExecutorManager;

}