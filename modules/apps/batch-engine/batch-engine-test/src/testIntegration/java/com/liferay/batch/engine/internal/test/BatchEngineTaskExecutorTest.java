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

package com.liferay.batch.engine.internal.test;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.batch.engine.BatchEngineTaskExecuteStatus;
import com.liferay.batch.engine.BatchEngineTaskExecutor;
import com.liferay.batch.engine.BatchEngineTaskFieldId;
import com.liferay.batch.engine.BatchEngineTaskMethod;
import com.liferay.batch.engine.BatchEngineTaskOperation;
import com.liferay.batch.engine.model.BatchEngineTask;
import com.liferay.batch.engine.service.BatchEngineTaskLocalService;
import com.liferay.blogs.model.BlogsEntry;
import com.liferay.blogs.service.BlogsEntryLocalService;
import com.liferay.headless.delivery.dto.v1_0.BlogPosting;
import com.liferay.headless.delivery.dto.v1_0.Rating;
import com.liferay.headless.delivery.resource.v1_0.BlogPostingResource;
import com.liferay.petra.io.unsync.UnsyncBufferedReader;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.search.Sort;
import com.liferay.portal.kernel.search.filter.Filter;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.rule.DeleteAfterTestRun;
import com.liferay.portal.kernel.test.util.GroupTestUtil;
import com.liferay.portal.kernel.test.util.ServiceContextTestUtil;
import com.liferay.portal.kernel.test.util.UserTestUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.HashMapDictionary;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Time;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.portal.vulcan.accept.language.AcceptLanguage;
import com.liferay.portal.vulcan.pagination.Page;
import com.liferay.portal.vulcan.pagination.Pagination;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;

import java.nio.charset.StandardCharsets;

import java.sql.Blob;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;

/**
 * @author Ivica Cardic
 */
@RunWith(Arquillian.class)
public class BatchEngineTaskExecutorTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new LiferayIntegrationTestRule();

	@BeforeClass
	public static void setUpClass() {
		_objectMapper.addMixIn(BlogPosting.class, BlogPostingMixin.class);
	}

	@Before
	public void setUp() throws Exception {
		_group = GroupTestUtil.addGroup();

		_user = UserTestUtil.addGroupAdminUser(_group);

		_baseDate = _dateFormat.parse(_dateFormat.format(new Date()));

		Bundle bundle = FrameworkUtil.getBundle(
			BatchEngineTaskExecutorTest.class);

		BundleContext bundleContext = bundle.getBundleContext();

		_blogPostingResourceServiceRegistration = bundleContext.registerService(
			BlogPostingResource.class, new TestBlogPostingResourceImpl(),
			new HashMapDictionary<String, String>() {
				{
					put("api.version", "v1.0");
					put("osgi.jaxrs.resource", "true");
				}
			});
	}

	@After
	public void tearDown() throws Exception {
		if (_batchEngineTask != null) {
			_batchEngineTaskLocalService.deleteBatchEngineTask(
				_batchEngineTask.getBatchEngineTaskId());
		}

		_blogsEntryLocalService.deleteEntries(_group.getGroupId());

		_blogPostingResourceServiceRegistration.unregister();
	}

	@Test
	public void testCreateBlogPostingsFromCSVFile() {
		_importBlogPostings(
			BatchEngineTaskOperation.CREATE,
			_getBlogPostingsCSVCreateContent(_group.getGroupId(), _FIELD_NAMES),
			"CSV", Collections.emptyMap());

		_assertCreatedBlogPostings();
	}

	@Test
	public void testCreateBlogPostingsFromCSVFileWithFieldMappings() {
		_importBlogPostings(
			BatchEngineTaskOperation.CREATE,
			_getBlogPostingsCSVCreateContent(
				_group.getGroupId(), _ALTERNATE_FIELD_NAMES),
			"CSV", _fieldNamesMappingMap);

		_assertCreatedBlogPostings();
	}

	@Test
	public void testCreateBlogPostingsFromInvalidCSVFile() {
		StringBundler sb = new StringBundler();

		_createCSVRow(
			sb, _FIELD_NAMES[0], _FIELD_NAMES[1], _FIELD_NAMES[2],
			_FIELD_NAMES[3], _FIELD_NAMES[4], "unknownColumn");

		_createCSVRow(
			sb, "alternativeHeadline", "articleBody",
			_dateFormat.format(new Date(_baseDate.getTime())), "headline",
			String.valueOf(_group.getGroupId()), "unknownValue");

		String content = sb.toString();

		try {
			_importBlogPostings(
				BatchEngineTaskOperation.CREATE,
				content.getBytes(StandardCharsets.UTF_8), "CSV",
				Collections.emptyMap());

			Assert.fail();
		}
		catch (AssertionError ae) {
		}
	}

	@Test
	public void testCreateBlogPostingsFromInvalidJSONFile() {
		StringBundler sb = new StringBundler();

		sb.append("[");

		_createJSONRow(
			sb, _FIELD_NAMES[0], _getJSONValue("alternativeHeadline"),
			_FIELD_NAMES[1], _getJSONValue("articleBody"), _FIELD_NAMES[2],
			_getJSONValue(_dateFormat.format(new Date(_baseDate.getTime()))),
			_FIELD_NAMES[3], _getJSONValue("headline"), _FIELD_NAMES[4],
			String.valueOf(_group.getGroupId()), "unknownColumn",
			_getJSONValue("unknownValue"));

		sb.append("]");

		String content = sb.toString();

		try {
			_importBlogPostings(
				BatchEngineTaskOperation.CREATE,
				content.getBytes(StandardCharsets.UTF_8), "JSON",
				Collections.emptyMap());

			Assert.fail();
		}
		catch (AssertionError ae) {
		}
	}

	@Test
	public void testCreateBlogPostingsFromInvalidJSONLFile() {
		StringBundler sb = new StringBundler();

		_createJSONRow(
			sb, _FIELD_NAMES[0], _getJSONValue("alternativeHeadline"),
			_FIELD_NAMES[1], _getJSONValue("articleBody"), _FIELD_NAMES[2],
			_getJSONValue(_dateFormat.format(new Date(_baseDate.getTime()))),
			_FIELD_NAMES[3], _getJSONValue("headline"), _FIELD_NAMES[4],
			String.valueOf(_group.getGroupId()), "unknownColumn",
			_getJSONValue("unknownValue"));

		String content = sb.toString();

		try {
			_importBlogPostings(
				BatchEngineTaskOperation.CREATE,
				content.getBytes(StandardCharsets.UTF_8), "JSONL",
				Collections.emptyMap());

			Assert.fail();
		}
		catch (AssertionError ae) {
		}
	}

	@Test
	public void testCreateBlogPostingsFromInvalidXLSFile() throws Exception {
		XSSFWorkbook xssfWorkbook = new XSSFWorkbook();

		Sheet sheet = xssfWorkbook.createSheet();

		_createXLSRow(
			sheet.createRow(0), _FIELD_NAMES[0], _FIELD_NAMES[1],
			_FIELD_NAMES[2], _FIELD_NAMES[3], _FIELD_NAMES[4], "unknownColumn");

		_createXLSRow(
			sheet.createRow(1), "alternativeHeadline", "articleBody",
			_dateFormat.format(new Date(_baseDate.getTime())), "headline",
			_group.getGroupId(), "unknownValue");

		try {
			_importBlogPostings(
				BatchEngineTaskOperation.CREATE, _getContent(xssfWorkbook),
				"XLS", Collections.emptyMap());

			Assert.fail();
		}
		catch (AssertionError ae) {
		}
	}

	@Test
	public void testCreateBlogPostingsFromJSONFile() {
		_importBlogPostings(
			BatchEngineTaskOperation.CREATE,
			_getBlogPostingsJSONCreateContent(
				_group.getGroupId(), _FIELD_NAMES),
			"JSON", Collections.emptyMap());

		_assertCreatedBlogPostings();
	}

	@Test
	public void testCreateBlogPostingsFromJSONFileWithFieldMappings() {
		_importBlogPostings(
			BatchEngineTaskOperation.CREATE,
			_getBlogPostingsJSONCreateContent(
				_group.getGroupId(), _ALTERNATE_FIELD_NAMES),
			"JSON", _fieldNamesMappingMap);

		_assertCreatedBlogPostings();
	}

	@Test
	public void testCreateBlogPostingsFromJSONLFile() {
		_importBlogPostings(
			BatchEngineTaskOperation.CREATE,
			_getBlogPostingsJSONLCreateContent(
				_group.getGroupId(), _FIELD_NAMES),
			"JSONL", Collections.emptyMap());

		_assertCreatedBlogPostings();
	}

	@Test
	public void testCreateBlogPostingsFromJSONLFileWithFieldMappings() {
		_importBlogPostings(
			BatchEngineTaskOperation.CREATE,
			_getBlogPostingsJSONLCreateContent(
				_group.getGroupId(), _ALTERNATE_FIELD_NAMES),
			"JSONL", _fieldNamesMappingMap);

		_assertCreatedBlogPostings();
	}

	@Test
	public void testCreateBlogPostingsFromXLSFile() throws Exception {
		_importBlogPostings(
			BatchEngineTaskOperation.CREATE,
			_getBlogPostingsXLSCreateContent(_group.getGroupId(), _FIELD_NAMES),
			"XLS", Collections.emptyMap());

		_assertCreatedBlogPostings();
	}

	@Test
	public void testCreateBlogPostingsFromXLSFileWithFieldMappings()
		throws Exception {

		_importBlogPostings(
			BatchEngineTaskOperation.CREATE,
			_getBlogPostingsXLSCreateContent(
				_group.getGroupId(), _ALTERNATE_FIELD_NAMES),
			"XLS", _fieldNamesMappingMap);

		_assertCreatedBlogPostings();
	}

	@Test
	public void testDeleteBlogPostingsFromCSVFile() throws Exception {
		List<BlogsEntry> blogsEntries = _addBlogsEntries();

		Assert.assertEquals(
			_ROWS_COUNT, _blogsEntryLocalService.getBlogsEntriesCount());

		_importBlogPostings(
			BatchEngineTaskOperation.DELETE,
			_getBlogPostingsCSVDeleteContent(blogsEntries), "CSV",
			Collections.emptyMap());

		Assert.assertEquals(0, _blogsEntryLocalService.getBlogsEntriesCount());
	}

	@Test
	public void testDeleteBlogPostingsFromJSONFile() throws Exception {
		List<BlogsEntry> blogsEntries = _addBlogsEntries();

		Assert.assertEquals(
			_ROWS_COUNT, _blogsEntryLocalService.getBlogsEntriesCount());

		_importBlogPostings(
			BatchEngineTaskOperation.DELETE,
			_getBlogPostingsJSONDeleteContent(blogsEntries), "JSON",
			Collections.emptyMap());

		Assert.assertEquals(0, _blogsEntryLocalService.getBlogsEntriesCount());
	}

	@Test
	public void testDeleteBlogPostingsFromJSONLFile() throws Exception {
		List<BlogsEntry> blogsEntries = _addBlogsEntries();

		Assert.assertEquals(
			_ROWS_COUNT, _blogsEntryLocalService.getBlogsEntriesCount());

		_importBlogPostings(
			BatchEngineTaskOperation.DELETE,
			_getBlogPostingsJSONLDeleteContent(blogsEntries), "JSONL",
			Collections.emptyMap());

		Assert.assertEquals(0, _blogsEntryLocalService.getBlogsEntriesCount());
	}

	@Test
	public void testDeleteBlogPostingsFromXLSFile() throws Exception {
		List<BlogsEntry> blogsEntries = _addBlogsEntries();

		Assert.assertEquals(
			_ROWS_COUNT, _blogsEntryLocalService.getBlogsEntriesCount());

		_importBlogPostings(
			BatchEngineTaskOperation.DELETE,
			_getBlogPostingsXLSDeleteContent(blogsEntries), "XLS",
			Collections.emptyMap());

		Assert.assertEquals(0, _blogsEntryLocalService.getBlogsEntriesCount());
	}

	@Test
	public void testExportBlogPostingsToCSVFile() throws Exception {
		List<BlogsEntry> blogsEntries = _addBlogsEntries();

		Assert.assertEquals(
			_ROWS_COUNT, _blogsEntryLocalService.getBlogsEntriesCount());

		_exportBlogPostings("CSV");

		BatchEngineTask batchEngineTask =
			_batchEngineTaskLocalService.getBatchEngineTask(
				_batchEngineTask.getBatchEngineTaskId());

		Blob content = batchEngineTask.getContent();

		UnsyncBufferedReader unsyncBufferedReader = new UnsyncBufferedReader(
			new InputStreamReader(content.getBinaryStream()));

		unsyncBufferedReader.readLine();

		String line = null;
		List<String[]> rowsValues = new ArrayList<>();

		while ((line = unsyncBufferedReader.readLine()) != null) {
			rowsValues.add(StringUtil.split(line, ','));
		}

		blogsEntries.sort(Comparator.comparing(BlogsEntry::getSubtitle));
		rowsValues.sort(Comparator.comparing(rowValues -> rowValues[0]));

		for (int i = 0; i < blogsEntries.size(); i++) {
			BlogsEntry blogsEntry = blogsEntries.get(i);
			String[] rowValues = rowsValues.get(i);

			Assert.assertEquals(rowValues[0], blogsEntry.getSubtitle());
			Assert.assertEquals(rowValues[1], blogsEntry.getContent());
			Assert.assertEquals(
				_dateFormat.parse(rowValues[4]), blogsEntry.getDisplayDate());
			Assert.assertEquals(rowValues[8], blogsEntry.getTitle());
			Assert.assertEquals(
				GetterUtil.getLong(rowValues[11]), blogsEntry.getGroupId());
		}
	}

	@Test
	public void testExportBlogPostingsToJSONFile() throws Exception {
		List<BlogsEntry> blogsEntries = _addBlogsEntries();

		Assert.assertEquals(
			_ROWS_COUNT, _blogsEntryLocalService.getBlogsEntriesCount());

		_exportBlogPostings("JSON");

		BatchEngineTask batchEngineTask =
			_batchEngineTaskLocalService.getBatchEngineTask(
				_batchEngineTask.getBatchEngineTaskId());

		Blob content = batchEngineTask.getContent();

		List<BlogPosting> blogPostings = _objectMapper.readValue(
			content.getBinaryStream(),
			new TypeReference<List<BlogPosting>>() {
			});

		blogsEntries.sort(Comparator.comparing(BlogsEntry::getSubtitle));
		blogPostings.sort(
			Comparator.comparing(BlogPosting::getAlternativeHeadline));

		for (int i = 0; i < blogsEntries.size(); i++) {
			BlogsEntry blogsEntry = blogsEntries.get(i);
			BlogPosting blogPosting = blogPostings.get(i);

			Assert.assertEquals(
				blogPosting.getAlternativeHeadline(), blogsEntry.getSubtitle());
			Assert.assertEquals(
				blogPosting.getArticleBody(), blogsEntry.getContent());
			Assert.assertEquals(
				blogPosting.getDatePublished(), blogsEntry.getDisplayDate());
			Assert.assertEquals(
				blogPosting.getHeadline(), blogsEntry.getTitle());

			Long siteId = blogPosting.getSiteId();

			Assert.assertEquals(siteId.longValue(), blogsEntry.getGroupId());
		}
	}

	@Test
	public void testExportBlogPostingsToJSONLFile() throws Exception {
		List<BlogsEntry> blogsEntries = _addBlogsEntries();

		Assert.assertEquals(
			_ROWS_COUNT, _blogsEntryLocalService.getBlogsEntriesCount());

		_exportBlogPostings("JSONL");

		BatchEngineTask batchEngineTask =
			_batchEngineTaskLocalService.getBatchEngineTask(
				_batchEngineTask.getBatchEngineTaskId());

		Blob content = batchEngineTask.getContent();

		UnsyncBufferedReader unsyncBufferedReader = new UnsyncBufferedReader(
			new InputStreamReader(content.getBinaryStream()));

		List<BlogPosting> blogPostings = new ArrayList<>();
		String line = null;

		while ((line = unsyncBufferedReader.readLine()) != null) {
			blogPostings.add(
				_objectMapper.readValue(
					line,
					new TypeReference<BlogPosting>() {
					}));
		}

		blogsEntries.sort(Comparator.comparing(BlogsEntry::getSubtitle));
		blogPostings.sort(
			Comparator.comparing(BlogPosting::getAlternativeHeadline));

		for (int i = 0; i < blogsEntries.size(); i++) {
			BlogsEntry blogsEntry = blogsEntries.get(i);
			BlogPosting blogPosting = blogPostings.get(i);

			Assert.assertEquals(
				blogPosting.getAlternativeHeadline(), blogsEntry.getSubtitle());
			Assert.assertEquals(
				blogPosting.getArticleBody(), blogsEntry.getContent());
			Assert.assertEquals(
				blogPosting.getDatePublished(), blogsEntry.getDisplayDate());
			Assert.assertEquals(
				blogPosting.getHeadline(), blogsEntry.getTitle());

			Long siteId = blogPosting.getSiteId();

			Assert.assertEquals(siteId.longValue(), blogsEntry.getGroupId());
		}
	}

	@Test
	public void testExportBlogPostingsToXLSFile() throws Exception {
		List<BlogsEntry> blogsEntries = _addBlogsEntries();

		Assert.assertEquals(
			_ROWS_COUNT, _blogsEntryLocalService.getBlogsEntriesCount());

		_exportBlogPostings("XLS");

		BatchEngineTask batchEngineTask =
			_batchEngineTaskLocalService.getBatchEngineTask(
				_batchEngineTask.getBatchEngineTaskId());

		Blob content = batchEngineTask.getContent();

		XSSFWorkbook xssfWorkbook = new XSSFWorkbook(content.getBinaryStream());

		Sheet sheet = xssfWorkbook.getSheetAt(0);

		Iterator<Row> rowIterator = sheet.iterator();

		rowIterator.next();

		List<List<Object>> rowsValues = new ArrayList<>();

		while (rowIterator.hasNext()) {
			Row row = rowIterator.next();

			List<Object> rowValues = new ArrayList<>();

			for (Cell cell : row) {
				if (CellType.BOOLEAN == cell.getCellType()) {
					rowValues.add(cell.getBooleanCellValue());
				}
				else if (CellType.NUMERIC == cell.getCellType()) {
					if (DateUtil.isCellDateFormatted(cell)) {
						rowValues.add(cell.getDateCellValue());
					}
					else {
						rowValues.add(cell.getNumericCellValue());
					}
				}
				else {
					rowValues.add(cell.getStringCellValue());
				}
			}

			rowsValues.add(rowValues);
		}

		blogsEntries.sort(Comparator.comparing(BlogsEntry::getSubtitle));
		rowsValues.sort(
			Comparator.comparing(rowValues -> (String)rowValues.get(0)));

		for (int i = 0; i < blogsEntries.size(); i++) {
			BlogsEntry blogsEntry = blogsEntries.get(i);
			List<Object> rowValues = rowsValues.get(i);

			Assert.assertEquals(rowValues.get(0), blogsEntry.getSubtitle());
			Assert.assertEquals(rowValues.get(1), blogsEntry.getContent());
			Assert.assertEquals(rowValues.get(4), blogsEntry.getDisplayDate());
			Assert.assertEquals(rowValues.get(8), blogsEntry.getTitle());

			Number siteId = (Number)rowValues.get(11);

			Assert.assertEquals(siteId.longValue(), blogsEntry.getGroupId());
		}
	}

	@Test
	public void testUpdateBlogPostingsFromCSVFile() throws Exception {
		List<BlogsEntry> blogsEntries = _addBlogsEntries();

		Assert.assertEquals(
			_ROWS_COUNT, _blogsEntryLocalService.getBlogsEntriesCount());

		_importBlogPostings(
			BatchEngineTaskOperation.UPDATE,
			_getBlogPostingsCSVUpdateContent(blogsEntries), "CSV",
			Collections.emptyMap());

		_assertUpdatedBlogPostings();
	}

	@Test
	public void testUpdateBlogPostingsFromJSONFile() throws Exception {
		List<BlogsEntry> blogsEntries = _addBlogsEntries();

		Assert.assertEquals(
			_ROWS_COUNT, _blogsEntryLocalService.getBlogsEntriesCount());

		_importBlogPostings(
			BatchEngineTaskOperation.UPDATE,
			_getBlogPostingsJSONUpdateContent(blogsEntries), "JSON",
			Collections.emptyMap());

		_assertUpdatedBlogPostings();
	}

	@Test
	public void testUpdateBlogPostingsFromJSONLFile() throws Exception {
		List<BlogsEntry> blogsEntries = _addBlogsEntries();

		Assert.assertEquals(
			_ROWS_COUNT, _blogsEntryLocalService.getBlogsEntriesCount());

		_importBlogPostings(
			BatchEngineTaskOperation.UPDATE,
			_getBlogPostingsJSONLUpdateContent(blogsEntries), "JSONL",
			Collections.emptyMap());

		_assertUpdatedBlogPostings();
	}

	@Test
	public void testUpdateBlogPostingsFromXLSFile() throws Exception {
		List<BlogsEntry> blogsEntries = _addBlogsEntries();

		Assert.assertEquals(
			_ROWS_COUNT, _blogsEntryLocalService.getBlogsEntriesCount());

		_importBlogPostings(
			BatchEngineTaskOperation.UPDATE,
			_getBlogPostingsXLSUpdateContent(blogsEntries), "XLS",
			Collections.emptyMap());

		_assertUpdatedBlogPostings();
	}

	public abstract class BaseBlogPostingResourceImpl
		implements BlogPostingResource {

		@DELETE
		@Override
		@Path("/blog-postings/{blogPostingId}")
		@Produces("application/json")
		public void deleteBlogPosting(
				@PathParam("blogPostingId") Long blogPostingId)
			throws Exception {
		}

		@GET
		@Override
		@Path("/sites/{siteId}/blog-postings")
		@Produces({"application/json", "application/xml"})
		public Page<BlogPosting> getSiteBlogPostingsPage(
				@PathParam("siteId") Long siteId,
				@QueryParam("search") String search, @Context Filter filter,
				@Context Pagination pagination, @Context Sort[] sorts)
			throws Exception {

			return Page.of(Collections.emptyList());
		}

		@Consumes({"application/json", "application/xml"})
		@Override
		@Path("/sites/{siteId}/blog-postings")
		@POST
		@Produces({"application/json", "application/xml"})
		public BlogPosting postSiteBlogPosting(
				@PathParam("siteId") Long siteId, BlogPosting blogPosting)
			throws Exception {

			return new BlogPosting();
		}

		@Consumes({"application/json", "application/xml"})
		@Override
		@Path("/blog-postings/{blogPostingId}")
		@Produces({"application/json", "application/xml"})
		@PUT
		public BlogPosting putBlogPosting(
				@PathParam("blogPostingId") Long blogPostingId,
				BlogPosting blogPosting)
			throws Exception {

			return new BlogPosting();
		}

		protected AcceptLanguage contextAcceptLanguage;
		protected Company contextCompany;
		protected User contextUser;

	}

	public abstract class BlogPostingMixin {

		@JsonProperty(access = JsonProperty.Access.READ_WRITE)
		public Long siteId;

	}

	public class TestBlogPostingResourceImpl
		extends BaseBlogPostingResourceImpl {

		@BatchEngineTaskMethod(
			batchEngineTaskOperation = BatchEngineTaskOperation.DELETE,
			itemClass = BlogPosting.class
		)
		@Override
		public void deleteBlogPosting(
				@BatchEngineTaskFieldId("id") Long blogPostingId)
			throws Exception {

			_initContextFields();

			_blogPostingResource.deleteBlogPosting(blogPostingId);
		}

		@Override
		public void deleteBlogPostingMyRating(Long blogPostingId)
			throws Exception {
		}

		@Override
		public BlogPosting getBlogPosting(Long blogPostingId) throws Exception {
			return null;
		}

		@Override
		public Rating getBlogPostingMyRating(Long blogPostingId)
			throws Exception {

			return null;
		}

		@BatchEngineTaskMethod(
			batchEngineTaskOperation = BatchEngineTaskOperation.READ,
			itemClass = BlogPosting.class
		)
		@Override
		public Page<BlogPosting> getSiteBlogPostingsPage(
				Long siteId, String search, Filter filter,
				Pagination pagination, Sort[] sorts)
			throws Exception {

			_initContextFields();

			return _blogPostingResource.getSiteBlogPostingsPage(
				siteId, search, filter, pagination, sorts);
		}

		@Override
		public BlogPosting patchBlogPosting(
				Long blogPostingId, BlogPosting blogPosting)
			throws Exception {

			return null;
		}

		@Override
		public Rating postBlogPostingMyRating(Long blogPostingId, Rating rating)
			throws Exception {

			return null;
		}

		@BatchEngineTaskMethod(
			batchEngineTaskOperation = BatchEngineTaskOperation.CREATE,
			itemClass = BlogPosting.class
		)
		@Override
		public BlogPosting postSiteBlogPosting(
				Long siteId, BlogPosting blogPosting)
			throws Exception {

			_initContextFields();

			return _blogPostingResource.postSiteBlogPosting(
				siteId, blogPosting);
		}

		@BatchEngineTaskMethod(
			batchEngineTaskOperation = BatchEngineTaskOperation.UPDATE,
			itemClass = BlogPosting.class
		)
		@Override
		public BlogPosting putBlogPosting(
				@BatchEngineTaskFieldId("id") Long blogPostingId,
				BlogPosting blogPosting)
			throws Exception {

			_initContextFields();

			return _blogPostingResource.putBlogPosting(
				blogPostingId, blogPosting);
		}

		@Override
		public Rating putBlogPostingMyRating(Long blogPostingId, Rating rating)
			throws Exception {

			return null;
		}

		@Override
		public void putSiteBlogPostingSubscribe(Long siteId) throws Exception {
		}

		@Override
		public void putSiteBlogPostingUnsubscribe(Long siteId)
			throws Exception {
		}

		@Override
		public void setContextCompany(Company contextCompany) {
		}

		@Override
		public void setContextUser(User contextUser) {
		}

		private void _initContextFields() {
			_blogPostingResource.setContextAcceptLanguage(
				contextAcceptLanguage);
			_blogPostingResource.setContextCompany(contextCompany);
			_blogPostingResource.setContextUser(contextUser);
		}

	}

	private List<BlogsEntry> _addBlogsEntries() throws Exception {
		List<BlogsEntry> blogsEntries = new ArrayList<>();

		for (int i = 0; i < _ROWS_COUNT; i++) {
			blogsEntries.add(
				_blogsEntryLocalService.addEntry(
					_user.getUserId(), "headline" + i,
					"alternativeHeadline" + i, null, "articleBody" + i,
					new Date(_baseDate.getTime()), false, false, null, null,
					null, null,
					ServiceContextTestUtil.getServiceContext(
						_user.getCompanyId(), _group.getGroupId(),
						_user.getUserId())));
		}

		return blogsEntries;
	}

	private void _assertCreatedBlogPostings() {
		Assert.assertEquals(
			_ROWS_COUNT, _blogsEntryLocalService.getBlogsEntriesCount());

		List<BlogsEntry> blogsEntries = _blogsEntryLocalService.getBlogsEntries(
			0, _blogsEntryLocalService.getBlogsEntriesCount());

		blogsEntries = new ArrayList<>(blogsEntries);

		blogsEntries.sort(Comparator.comparingLong(BlogsEntry::getEntryId));

		for (int i = 0; i < blogsEntries.size(); i++) {
			BlogsEntry blogsEntry = blogsEntries.get(i);

			Assert.assertEquals(
				"alternativeHeadline" + i, blogsEntry.getSubtitle());
			Assert.assertEquals("articleBody" + i, blogsEntry.getContent());
			Assert.assertEquals(
				_baseDate.getTime() + i * Time.MINUTE,
				_getTime(blogsEntry.getDisplayDate()));
			Assert.assertEquals("headline" + i, blogsEntry.getTitle());
		}
	}

	private void _assertUpdatedBlogPostings() {
		Assert.assertEquals(
			_ROWS_COUNT, _blogsEntryLocalService.getBlogsEntriesCount());

		List<BlogsEntry> blogsEntries = _blogsEntryLocalService.getBlogsEntries(
			0, _blogsEntryLocalService.getBlogsEntriesCount());

		blogsEntries = new ArrayList<>(blogsEntries);

		blogsEntries.sort(Comparator.comparingLong(BlogsEntry::getEntryId));

		for (int i = 0; i < blogsEntries.size(); i++) {
			BlogsEntry blogsEntry = blogsEntries.get(i);

			Assert.assertEquals(
				"alternativeHeadline" + i + i, blogsEntry.getSubtitle());
			Assert.assertEquals("articleBody" + i + i, blogsEntry.getContent());
			Assert.assertEquals(
				_baseDate.getTime() + i * Time.MINUTE,
				_getTime(blogsEntry.getDisplayDate()));
			Assert.assertEquals("headline" + i + i, blogsEntry.getTitle());
		}
	}

	private void _createCSVRow(StringBundler sb, String... values) {
		for (int i = 0; i < values.length; i++) {
			sb.append(values[i]);

			if (i < (values.length - 1)) {
				sb.append(",");
			}
		}

		sb.append("\n");
	}

	private void _createJSONRow(StringBundler sb, String... values) {
		sb.append("{");

		for (int i = 0; i < values.length; i = i + 2) {
			sb.append("\"");
			sb.append(values[i]);
			sb.append("\":");
			sb.append(values[i + 1]);

			if (i < (values.length - 2)) {
				sb.append(",");
			}
		}

		sb.append("}");
	}

	private void _createXLSRow(Row row, Object... values) {
		for (int i = 0; i < values.length; i++) {
			Cell cell = row.createCell(i);

			if (values[i] instanceof Boolean) {
				cell.setCellValue((Boolean)values[i]);
			}
			else if (values[i] instanceof Date) {
				cell.setCellValue((Date)values[i]);
			}
			else if (values[i] instanceof Number) {
				Number value = (Number)values[i];

				cell.setCellValue(value.doubleValue());
			}
			else {
				cell.setCellValue((String)values[i]);
			}
		}
	}

	private void _exportBlogPostings(String contentType) {
		Map<String, Serializable> parameters = new HashMap<>();

		parameters.put("siteId", _group.getGroupId());

		_batchEngineTask = _batchEngineTaskLocalService.addBatchEngineTask(
			_user.getCompanyId(), _user.getUserId(), 10, null,
			BlogPosting.class.getName(), null, contentType,
			BatchEngineTaskExecuteStatus.INITIAL.name(), Collections.emptyMap(),
			BatchEngineTaskOperation.READ.name(), parameters, "v1.0");

		_batchEngineTaskExecutor.execute(_batchEngineTask);
	}

	private byte[] _getBlogPostingsCSVCreateContent(
		long siteId, String[] fieldNames) {

		StringBundler sb = new StringBundler();

		_createCSVRow(sb, fieldNames);

		for (int i = 0; i < _ROWS_COUNT; i++) {
			_createCSVRow(
				sb, "alternativeHeadline" + i, "articleBody" + i,
				_dateFormat.format(
					new Date(_baseDate.getTime() + (i * Time.MINUTE))),
				"headline" + i, String.valueOf(siteId));
		}

		String content = sb.toString();

		return content.getBytes(StandardCharsets.UTF_8);
	}

	private byte[] _getBlogPostingsCSVDeleteContent(
		List<BlogsEntry> blogsEntries) {

		StringBundler sb = new StringBundler();

		_createCSVRow(sb, "id");

		for (BlogsEntry blogsEntry : blogsEntries) {
			_createCSVRow(sb, String.valueOf(blogsEntry.getEntryId()));
		}

		String content = sb.toString();

		return content.getBytes(StandardCharsets.UTF_8);
	}

	private byte[] _getBlogPostingsCSVUpdateContent(
		List<BlogsEntry> blogsEntries) {

		StringBundler sb = new StringBundler();

		_createCSVRow(
			sb, "alternativeHeadline", "articleBody", "datePublished",
			"headline", "id");

		for (int i = 0; i < blogsEntries.size(); i++) {
			BlogsEntry blogsEntry = blogsEntries.get(i);

			_createCSVRow(
				sb, blogsEntry.getSubtitle() + i, blogsEntry.getContent() + i,
				_dateFormat.format(
					new Date(
						_getTime(blogsEntry.getDisplayDate()) +
							i * Time.MINUTE)),
				blogsEntry.getTitle() + i,
				String.valueOf(blogsEntry.getEntryId()));
		}

		String content = sb.toString();

		return content.getBytes(StandardCharsets.UTF_8);
	}

	private byte[] _getBlogPostingsJSONCreateContent(
		long siteId, String[] fieldNames) {

		StringBundler sb = new StringBundler();

		sb.append("[");

		for (int i = 0; i < _ROWS_COUNT; i++) {
			_createJSONRow(
				sb, fieldNames[0], _getJSONValue("alternativeHeadline" + i),
				fieldNames[1], _getJSONValue("articleBody" + i), fieldNames[2],
				_getJSONValue(
					_dateFormat.format(
						new Date(_baseDate.getTime() + (i * Time.MINUTE)))),
				fieldNames[3], _getJSONValue("headline" + i), fieldNames[4],
				String.valueOf(siteId));

			if (i < (_ROWS_COUNT - 1)) {
				sb.append(",");
			}
		}

		sb.append("]");

		String content = sb.toString();

		return content.getBytes(StandardCharsets.UTF_8);
	}

	private byte[] _getBlogPostingsJSONDeleteContent(
		List<BlogsEntry> blogsEntries) {

		StringBundler sb = new StringBundler();

		sb.append("[");

		for (int i = 0; i < blogsEntries.size(); i++) {
			BlogsEntry blogsEntry = blogsEntries.get(i);

			_createJSONRow(sb, "id", String.valueOf(blogsEntry.getEntryId()));

			if (i < (blogsEntries.size() - 1)) {
				sb.append(",");
			}
		}

		sb.append("]");

		String content = sb.toString();

		return content.getBytes(StandardCharsets.UTF_8);
	}

	private byte[] _getBlogPostingsJSONLCreateContent(
		long siteId, String[] fieldNames) {

		StringBundler sb = new StringBundler();

		for (int i = 0; i < _ROWS_COUNT; i++) {
			_createJSONRow(
				sb, fieldNames[0], _getJSONValue("alternativeHeadline" + i),
				fieldNames[1], _getJSONValue("articleBody" + i), fieldNames[2],
				_getJSONValue(
					_dateFormat.format(
						new Date(_baseDate.getTime() + (i * Time.MINUTE)))),
				fieldNames[3], _getJSONValue("headline" + i), fieldNames[4],
				String.valueOf(siteId));

			if (i < (_ROWS_COUNT - 1)) {
				sb.append("\n");
			}
		}

		String content = sb.toString();

		return content.getBytes(StandardCharsets.UTF_8);
	}

	private byte[] _getBlogPostingsJSONLDeleteContent(
		List<BlogsEntry> blogsEntries) {

		StringBundler sb = new StringBundler();

		for (int i = 0; i < blogsEntries.size(); i++) {
			BlogsEntry blogsEntry = blogsEntries.get(i);

			_createJSONRow(sb, "id", String.valueOf(blogsEntry.getEntryId()));

			if (i < (blogsEntries.size() - 1)) {
				sb.append("\n");
			}
		}

		String content = sb.toString();

		return content.getBytes(StandardCharsets.UTF_8);
	}

	private byte[] _getBlogPostingsJSONLUpdateContent(
		List<BlogsEntry> blogsEntries) {

		StringBundler sb = new StringBundler();

		for (int i = 0; i < blogsEntries.size(); i++) {
			BlogsEntry blogsEntry = blogsEntries.get(i);

			_createJSONRow(
				sb, _FIELD_NAMES[0],
				_getJSONValue(blogsEntry.getSubtitle() + i), _FIELD_NAMES[1],
				_getJSONValue(blogsEntry.getContent() + i), _FIELD_NAMES[2],
				_getJSONValue(
					_dateFormat.format(
						new Date(
							_getTime(blogsEntry.getDisplayDate()) +
								i * Time.MINUTE))),
				_FIELD_NAMES[3], _getJSONValue(blogsEntry.getTitle() + i), "id",
				String.valueOf(blogsEntry.getEntryId()));

			if (i < (blogsEntries.size() - 1)) {
				sb.append("\n");
			}
		}

		String content = sb.toString();

		return content.getBytes(StandardCharsets.UTF_8);
	}

	private byte[] _getBlogPostingsJSONUpdateContent(
		List<BlogsEntry> blogsEntries) {

		StringBundler sb = new StringBundler();

		sb.append("[");

		for (int i = 0; i < blogsEntries.size(); i++) {
			BlogsEntry blogsEntry = blogsEntries.get(i);

			_createJSONRow(
				sb, _FIELD_NAMES[0],
				_getJSONValue(blogsEntry.getSubtitle() + i), _FIELD_NAMES[1],
				_getJSONValue(blogsEntry.getContent() + i), _FIELD_NAMES[2],
				_getJSONValue(
					_dateFormat.format(
						new Date(
							_getTime(blogsEntry.getDisplayDate()) +
								i * Time.MINUTE))),
				_FIELD_NAMES[3], _getJSONValue(blogsEntry.getTitle() + i), "id",
				String.valueOf(blogsEntry.getEntryId()));

			if (i < (blogsEntries.size() - 1)) {
				sb.append(",");
			}
		}

		sb.append("]");

		String content = sb.toString();

		return content.getBytes(StandardCharsets.UTF_8);
	}

	private byte[] _getBlogPostingsXLSCreateContent(
			long siteId, String[] fieldNames)
		throws IOException {

		XSSFWorkbook xssfWorkbook = new XSSFWorkbook();

		Sheet sheet = xssfWorkbook.createSheet();

		_createXLSRow(
			sheet.createRow(0), fieldNames[0], fieldNames[1], fieldNames[2],
			fieldNames[3], fieldNames[4]);

		for (int i = 0; i < _ROWS_COUNT; i++) {
			_createXLSRow(
				sheet.createRow(i + 1), "alternativeHeadline" + i,
				"articleBody" + i,
				_dateFormat.format(
					new Date(_baseDate.getTime() + i * Time.MINUTE)),
				"headline" + i, siteId);
		}

		return _getContent(xssfWorkbook);
	}

	private byte[] _getBlogPostingsXLSDeleteContent(
			List<BlogsEntry> blogsEntries)
		throws IOException {

		XSSFWorkbook xssfWorkbook = new XSSFWorkbook();

		Sheet sheet = xssfWorkbook.createSheet();

		_createXLSRow(sheet.createRow(0), "id");

		for (int i = 0; i < blogsEntries.size(); i++) {
			BlogsEntry blogsEntry = blogsEntries.get(i);

			_createXLSRow(sheet.createRow(i + 1), blogsEntry.getEntryId());
		}

		return _getContent(xssfWorkbook);
	}

	private byte[] _getBlogPostingsXLSUpdateContent(
			List<BlogsEntry> blogsEntries)
		throws IOException {

		XSSFWorkbook xssfWorkbook = new XSSFWorkbook();

		Sheet sheet = xssfWorkbook.createSheet();

		_createXLSRow(
			sheet.createRow(0), _FIELD_NAMES[0], _FIELD_NAMES[1],
			_FIELD_NAMES[2], _FIELD_NAMES[3], "id");

		for (int i = 0; i < blogsEntries.size(); i++) {
			BlogsEntry blogsEntry = blogsEntries.get(i);

			_createXLSRow(
				sheet.createRow(i + 1), blogsEntry.getSubtitle() + i,
				blogsEntry.getContent() + i,
				_dateFormat.format(
					new Date(
						_getTime(blogsEntry.getDisplayDate()) +
							i * Time.MINUTE)),
				blogsEntry.getTitle() + i, blogsEntry.getEntryId());
		}

		return _getContent(xssfWorkbook);
	}

	private byte[] _getContent(XSSFWorkbook xssfWorkbook) throws IOException {
		ByteArrayOutputStream byteArrayOutputStream =
			new ByteArrayOutputStream();

		xssfWorkbook.write(byteArrayOutputStream);

		xssfWorkbook.close();

		try {
			return byteArrayOutputStream.toByteArray();
		}
		finally {
			byteArrayOutputStream.close();
		}
	}

	private String _getJSONValue(String value) {
		return "\"" + value + "\"";
	}

	private long _getTime(Date date) {
		return date.getTime();
	}

	private void _importBlogPostings(
		BatchEngineTaskOperation batchEngineTaskOperation, byte[] content,
		String contentType, Map<String, String> fieldNameMappingMap) {

		_batchEngineTask = _batchEngineTaskLocalService.addBatchEngineTask(
			_user.getCompanyId(), _user.getUserId(), 10, null,
			BlogPosting.class.getName(), content, contentType,
			BatchEngineTaskExecuteStatus.INITIAL.name(), fieldNameMappingMap,
			batchEngineTaskOperation.name(), Collections.emptyMap(), "v1.0");

		_batchEngineTaskExecutor.execute(_batchEngineTask);
	}

	private static final String[] _ALTERNATE_FIELD_NAMES = {
		"alternativeHeadline1", "articleBody1", "datePublished1", "headline1",
		"siteId1"
	};

	private static final String[] _FIELD_NAMES = {
		"alternativeHeadline", "articleBody", "datePublished", "headline",
		"siteId"
	};

	private static final int _ROWS_COUNT = 18;

	private static final Map<String, String> _fieldNamesMappingMap =
		new HashMap<String, String>() {
			{
				put("alternativeHeadline1", "alternativeHeadline");
				put("articleBody1", "articleBody");
				put("datePublished1", "datePublished");
				put("headline1", "headline");
				put("siteId1", "siteId");
			}
		};
	private static final ObjectMapper _objectMapper = new ObjectMapper();

	private Date _baseDate;
	private BatchEngineTask _batchEngineTask;

	@Inject
	private BatchEngineTaskExecutor _batchEngineTaskExecutor;

	@Inject
	private BatchEngineTaskLocalService _batchEngineTaskLocalService;

	@Inject
	private BlogPostingResource _blogPostingResource;

	private ServiceRegistration<BlogPostingResource>
		_blogPostingResourceServiceRegistration;

	@Inject
	private BlogsEntryLocalService _blogsEntryLocalService;

	private final DateFormat _dateFormat = new SimpleDateFormat(
		"yyyy-MM-dd'T'HH:mm:00.000Z");

	@DeleteAfterTestRun
	private Group _group;

	@DeleteAfterTestRun
	private User _user;

}