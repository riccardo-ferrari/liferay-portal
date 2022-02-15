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

package com.liferay.headless.commerce.machine.learning.internal.dispatch.executor;

import com.liferay.analytics.exportimport.helper.AnalyticsExportImportHelper;
import com.liferay.dispatch.executor.BaseDispatchTaskExecutor;
import com.liferay.dispatch.executor.DispatchTaskExecutor;
import com.liferay.dispatch.executor.DispatchTaskExecutorOutput;
import com.liferay.dispatch.executor.DispatchTaskStatus;
import com.liferay.dispatch.model.DispatchLog;
import com.liferay.dispatch.model.DispatchTrigger;
import com.liferay.dispatch.service.DispatchLogLocalService;
import com.liferay.headless.commerce.machine.learning.dto.v1_0.Product;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.exception.PortalException;

import java.io.IOException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.Date;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Riccardo Ferrari
 */
@Component(
	enabled = false, immediate = true,
	property = {
		"dispatch.task.executor.name=" + AnalyticsUploadProductDispatchTaskExecutor.KEY,
		"dispatch.task.executor.type=" + AnalyticsUploadProductDispatchTaskExecutor.KEY
	},
	service = DispatchTaskExecutor.class
)
public class AnalyticsUploadProductDispatchTaskExecutor
	extends BaseDispatchTaskExecutor {

	public static final String KEY = "upload-analytics-dxp-product";

	@Override
	public void doExecute(
			DispatchTrigger dispatchTrigger,
			DispatchTaskExecutorOutput dispatchTaskExecutorOutput)
		throws IOException, PortalException {

		DispatchLog dispatchLog =
			_dispatchLogLocalService.fetchLatestDispatchLog(
				dispatchTrigger.getDispatchTriggerId(),
				DispatchTaskStatus.IN_PROGRESS);

		DispatchLog latestSuccessfulDispatchLog =
			_dispatchLogLocalService.fetchLatestDispatchLog(
				dispatchTrigger.getDispatchTriggerId(),
				DispatchTaskStatus.SUCCESSFUL);

		Date resourceLastModifiedDate = null;

		if (latestSuccessfulDispatchLog != null) {
			resourceLastModifiedDate =
				latestSuccessfulDispatchLog.getEndDate();
		}

		try {
			_analyticsExportImportHelper.exportToAnalyticsCloud(
				"analytics-dxp-product", dispatchTrigger.getCompanyId(), null,
				message -> _updateDispatchLog(
					dispatchLog.getDispatchLogId(), dispatchTaskExecutorOutput,
					message),
				resourceLastModifiedDate, Product.class.getName(),
				dispatchTrigger.getUserId());
		}
		catch (Exception exception) {
			throw new PortalException(exception);
		}
	}

	@Override
	public String getName() {
		return KEY;
	}

	private void _updateDispatchLog(
			long dispatchLogId,
			DispatchTaskExecutorOutput dispatchTaskExecutorOutput,
			String message)
		throws PortalException {

		StringBundler sb = new StringBundler(5);

		if (dispatchTaskExecutorOutput.getOutput() != null) {
			sb.append(dispatchTaskExecutorOutput.getOutput());
		}

		sb.append(_dateFormat.format(new Date()));
		sb.append(StringPool.SPACE);
		sb.append(message);
		sb.append(StringPool.NEW_LINE);

		dispatchTaskExecutorOutput.setOutput(sb.toString());

		_dispatchLogLocalService.updateDispatchLog(
			dispatchLogId, new Date(), dispatchTaskExecutorOutput.getError(),
			dispatchTaskExecutorOutput.getOutput(),
			DispatchTaskStatus.IN_PROGRESS);
	}

	private static final DateFormat _dateFormat = new SimpleDateFormat(
		"yyyy-MM-dd'T'HH:mm:ss.SSSZ");

	@Reference
	private AnalyticsExportImportHelper _analyticsExportImportHelper;

	@Reference
	private DispatchLogLocalService _dispatchLogLocalService;

}