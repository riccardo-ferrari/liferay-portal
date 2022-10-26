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

package com.liferay.analytics.settings.rest.resource.v1_0.test;

import com.liferay.analytics.settings.rest.client.dto.v1_0.ContactConfiguration;
import com.liferay.analytics.settings.rest.client.http.HttpInvoker;
import com.liferay.analytics.settings.rest.client.pagination.Page;
import com.liferay.analytics.settings.rest.client.pagination.Pagination;
import com.liferay.analytics.settings.rest.client.resource.v1_0.ContactResource;
import com.liferay.petra.function.UnsafeTriConsumer;
import com.liferay.petra.reflect.ReflectionUtil;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.service.CompanyLocalServiceUtil;
import com.liferay.portal.kernel.test.util.GroupTestUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.DateFormatFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.odata.entity.EntityField;
import com.liferay.portal.odata.entity.EntityModel;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.portal.vulcan.resource.EntityModelResource;

import java.lang.reflect.Method;

import java.text.DateFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Generated;

import javax.ws.rs.core.MultivaluedHashMap;

import org.apache.commons.lang.time.DateUtils;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Riccardo Ferrari
 * @generated
 */
@Generated("")
public abstract class BaseContactResourceTestCase {

	@ClassRule
	@Rule
	public static final LiferayIntegrationTestRule liferayIntegrationTestRule =
		new LiferayIntegrationTestRule();

	@BeforeClass
	public static void setUpClass() throws Exception {
		_dateFormat = DateFormatFactoryUtil.getSimpleDateFormat(
			"yyyy-MM-dd'T'HH:mm:ss'Z'");
	}

	@Before
	public void setUp() throws Exception {
		irrelevantGroup = GroupTestUtil.addGroup();
		testGroup = GroupTestUtil.addGroup();

		testCompany = CompanyLocalServiceUtil.getCompany(
			testGroup.getCompanyId());

		_contactResource.setContextCompany(testCompany);

		ContactResource.Builder builder = ContactResource.builder();

		contactResource = builder.authentication(
			"test@liferay.com", "test"
		).locale(
			LocaleUtil.getDefault()
		).build();
	}

	@After
	public void tearDown() throws Exception {
		GroupTestUtil.deleteGroup(irrelevantGroup);
		GroupTestUtil.deleteGroup(testGroup);
	}

	@Test
	public void testGetContactAccountGroupsPage() throws Exception {
		Page<Contact> page = contactResource.getContactAccountGroupsPage(
			Pagination.of(1, 10), null);

		long totalCount = page.getTotalCount();

		Contact contact1 = testGetContactAccountGroupsPage_addContact(
			randomContact());

		Contact contact2 = testGetContactAccountGroupsPage_addContact(
			randomContact());

		page = contactResource.getContactAccountGroupsPage(
			Pagination.of(1, 10), null);

		Assert.assertEquals(totalCount + 2, page.getTotalCount());

		assertContains(contact1, (List<Contact>)page.getItems());
		assertContains(contact2, (List<Contact>)page.getItems());
		assertValid(page);
	}

	@Test
	public void testGetContactAccountGroupsPageWithPagination()
		throws Exception {

		Page<Contact> totalPage = contactResource.getContactAccountGroupsPage(
			null, null);

		int totalCount = GetterUtil.getInteger(totalPage.getTotalCount());

		Contact contact1 = testGetContactAccountGroupsPage_addContact(
			randomContact());

		Contact contact2 = testGetContactAccountGroupsPage_addContact(
			randomContact());

		Contact contact3 = testGetContactAccountGroupsPage_addContact(
			randomContact());

		Page<Contact> page1 = contactResource.getContactAccountGroupsPage(
			Pagination.of(1, totalCount + 2), null);

		List<Contact> contacts1 = (List<Contact>)page1.getItems();

		Assert.assertEquals(
			contacts1.toString(), totalCount + 2, contacts1.size());

		Page<Contact> page2 = contactResource.getContactAccountGroupsPage(
			Pagination.of(2, totalCount + 2), null);

		Assert.assertEquals(totalCount + 3, page2.getTotalCount());

		List<Contact> contacts2 = (List<Contact>)page2.getItems();

		Assert.assertEquals(contacts2.toString(), 1, contacts2.size());

		Page<Contact> page3 = contactResource.getContactAccountGroupsPage(
			Pagination.of(1, totalCount + 3), null);

		assertContains(contact1, (List<Contact>)page3.getItems());
		assertContains(contact2, (List<Contact>)page3.getItems());
		assertContains(contact3, (List<Contact>)page3.getItems());
	}

	@Test
	public void testGetContactAccountGroupsPageWithSortDateTime()
		throws Exception {

		testGetContactAccountGroupsPageWithSort(
			EntityField.Type.DATE_TIME,
			(entityField, contact1, contact2) -> {
				BeanTestUtil.setProperty(
					contact1, entityField.getName(),
					DateUtils.addMinutes(new Date(), -2));
			});
	}

	@Test
	public void testGetContactAccountGroupsPageWithSortDouble()
		throws Exception {

		testGetContactAccountGroupsPageWithSort(
			EntityField.Type.DOUBLE,
			(entityField, contact1, contact2) -> {
				BeanTestUtil.setProperty(contact1, entityField.getName(), 0.1);
				BeanTestUtil.setProperty(contact2, entityField.getName(), 0.5);
			});
	}

	@Test
	public void testGetContactAccountGroupsPageWithSortInteger()
		throws Exception {

		testGetContactAccountGroupsPageWithSort(
			EntityField.Type.INTEGER,
			(entityField, contact1, contact2) -> {
				BeanTestUtil.setProperty(contact1, entityField.getName(), 0);
				BeanTestUtil.setProperty(contact2, entityField.getName(), 1);
			});
	}

	@Test
	public void testGetContactAccountGroupsPageWithSortString()
		throws Exception {

		testGetContactAccountGroupsPageWithSort(
			EntityField.Type.STRING,
			(entityField, contact1, contact2) -> {
				Class<?> clazz = contact1.getClass();

				String entityFieldName = entityField.getName();

				Method method = clazz.getMethod(
					"get" + StringUtil.upperCaseFirstLetter(entityFieldName));

				Class<?> returnType = method.getReturnType();

				if (returnType.isAssignableFrom(Map.class)) {
					BeanTestUtil.setProperty(
						contact1, entityFieldName,
						Collections.singletonMap("Aaa", "Aaa"));
					BeanTestUtil.setProperty(
						contact2, entityFieldName,
						Collections.singletonMap("Bbb", "Bbb"));
				}
				else if (entityFieldName.contains("email")) {
					BeanTestUtil.setProperty(
						contact1, entityFieldName,
						"aaa" +
							StringUtil.toLowerCase(
								RandomTestUtil.randomString()) +
									"@liferay.com");
					BeanTestUtil.setProperty(
						contact2, entityFieldName,
						"bbb" +
							StringUtil.toLowerCase(
								RandomTestUtil.randomString()) +
									"@liferay.com");
				}
				else {
					BeanTestUtil.setProperty(
						contact1, entityFieldName,
						"aaa" +
							StringUtil.toLowerCase(
								RandomTestUtil.randomString()));
					BeanTestUtil.setProperty(
						contact2, entityFieldName,
						"bbb" +
							StringUtil.toLowerCase(
								RandomTestUtil.randomString()));
				}
			});
	}

	protected void testGetContactAccountGroupsPageWithSort(
			EntityField.Type type,
			UnsafeTriConsumer<EntityField, Contact, Contact, Exception>
				unsafeTriConsumer)
		throws Exception {

		List<EntityField> entityFields = getEntityFields(type);

		if (entityFields.isEmpty()) {
			return;
		}

		Contact contact1 = randomContact();
		Contact contact2 = randomContact();

		for (EntityField entityField : entityFields) {
			unsafeTriConsumer.accept(entityField, contact1, contact2);
		}

		contact1 = testGetContactAccountGroupsPage_addContact(contact1);

		contact2 = testGetContactAccountGroupsPage_addContact(contact2);

		for (EntityField entityField : entityFields) {
			Page<Contact> ascPage = contactResource.getContactAccountGroupsPage(
				Pagination.of(1, 2), entityField.getName() + ":asc");

			assertEquals(
				Arrays.asList(contact1, contact2),
				(List<Contact>)ascPage.getItems());

			Page<Contact> descPage =
				contactResource.getContactAccountGroupsPage(
					Pagination.of(1, 2), entityField.getName() + ":desc");

			assertEquals(
				Arrays.asList(contact2, contact1),
				(List<Contact>)descPage.getItems());
		}
	}

	protected Contact testGetContactAccountGroupsPage_addContact(
			Contact contact)
		throws Exception {

		throw new UnsupportedOperationException(
			"This method needs to be implemented");
	}

	@Test
	public void testPutContactConfiguration() throws Exception {
		Assert.assertTrue(false);
	}

	@Test
	public void testGetContactOrganizationsPage() throws Exception {
		Page<Contact> page = contactResource.getContactOrganizationsPage(
			Pagination.of(1, 10), null);

		long totalCount = page.getTotalCount();

		Contact contact1 = testGetContactOrganizationsPage_addContact(
			randomContact());

		Contact contact2 = testGetContactOrganizationsPage_addContact(
			randomContact());

		page = contactResource.getContactOrganizationsPage(
			Pagination.of(1, 10), null);

		Assert.assertEquals(totalCount + 2, page.getTotalCount());

		assertContains(contact1, (List<Contact>)page.getItems());
		assertContains(contact2, (List<Contact>)page.getItems());
		assertValid(page);
	}

	@Test
	public void testGetContactOrganizationsPageWithPagination()
		throws Exception {

		Page<Contact> totalPage = contactResource.getContactOrganizationsPage(
			null, null);

		int totalCount = GetterUtil.getInteger(totalPage.getTotalCount());

		Contact contact1 = testGetContactOrganizationsPage_addContact(
			randomContact());

		Contact contact2 = testGetContactOrganizationsPage_addContact(
			randomContact());

		Contact contact3 = testGetContactOrganizationsPage_addContact(
			randomContact());

		Page<Contact> page1 = contactResource.getContactOrganizationsPage(
			Pagination.of(1, totalCount + 2), null);

		List<Contact> contacts1 = (List<Contact>)page1.getItems();

		Assert.assertEquals(
			contacts1.toString(), totalCount + 2, contacts1.size());

		Page<Contact> page2 = contactResource.getContactOrganizationsPage(
			Pagination.of(2, totalCount + 2), null);

		Assert.assertEquals(totalCount + 3, page2.getTotalCount());

		List<Contact> contacts2 = (List<Contact>)page2.getItems();

		Assert.assertEquals(contacts2.toString(), 1, contacts2.size());

		Page<Contact> page3 = contactResource.getContactOrganizationsPage(
			Pagination.of(1, totalCount + 3), null);

		assertContains(contact1, (List<Contact>)page3.getItems());
		assertContains(contact2, (List<Contact>)page3.getItems());
		assertContains(contact3, (List<Contact>)page3.getItems());
	}

	@Test
	public void testGetContactOrganizationsPageWithSortDateTime()
		throws Exception {

		testGetContactOrganizationsPageWithSort(
			EntityField.Type.DATE_TIME,
			(entityField, contact1, contact2) -> {
				BeanTestUtil.setProperty(
					contact1, entityField.getName(),
					DateUtils.addMinutes(new Date(), -2));
			});
	}

	@Test
	public void testGetContactOrganizationsPageWithSortDouble()
		throws Exception {

		testGetContactOrganizationsPageWithSort(
			EntityField.Type.DOUBLE,
			(entityField, contact1, contact2) -> {
				BeanTestUtil.setProperty(contact1, entityField.getName(), 0.1);
				BeanTestUtil.setProperty(contact2, entityField.getName(), 0.5);
			});
	}

	@Test
	public void testGetContactOrganizationsPageWithSortInteger()
		throws Exception {

		testGetContactOrganizationsPageWithSort(
			EntityField.Type.INTEGER,
			(entityField, contact1, contact2) -> {
				BeanTestUtil.setProperty(contact1, entityField.getName(), 0);
				BeanTestUtil.setProperty(contact2, entityField.getName(), 1);
			});
	}

	@Test
	public void testGetContactOrganizationsPageWithSortString()
		throws Exception {

		testGetContactOrganizationsPageWithSort(
			EntityField.Type.STRING,
			(entityField, contact1, contact2) -> {
				Class<?> clazz = contact1.getClass();

				String entityFieldName = entityField.getName();

				Method method = clazz.getMethod(
					"get" + StringUtil.upperCaseFirstLetter(entityFieldName));

				Class<?> returnType = method.getReturnType();

				if (returnType.isAssignableFrom(Map.class)) {
					BeanTestUtil.setProperty(
						contact1, entityFieldName,
						Collections.singletonMap("Aaa", "Aaa"));
					BeanTestUtil.setProperty(
						contact2, entityFieldName,
						Collections.singletonMap("Bbb", "Bbb"));
				}
				else if (entityFieldName.contains("email")) {
					BeanTestUtil.setProperty(
						contact1, entityFieldName,
						"aaa" +
							StringUtil.toLowerCase(
								RandomTestUtil.randomString()) +
									"@liferay.com");
					BeanTestUtil.setProperty(
						contact2, entityFieldName,
						"bbb" +
							StringUtil.toLowerCase(
								RandomTestUtil.randomString()) +
									"@liferay.com");
				}
				else {
					BeanTestUtil.setProperty(
						contact1, entityFieldName,
						"aaa" +
							StringUtil.toLowerCase(
								RandomTestUtil.randomString()));
					BeanTestUtil.setProperty(
						contact2, entityFieldName,
						"bbb" +
							StringUtil.toLowerCase(
								RandomTestUtil.randomString()));
				}
			});
	}

	protected void testGetContactOrganizationsPageWithSort(
			EntityField.Type type,
			UnsafeTriConsumer<EntityField, Contact, Contact, Exception>
				unsafeTriConsumer)
		throws Exception {

		List<EntityField> entityFields = getEntityFields(type);

		if (entityFields.isEmpty()) {
			return;
		}

		Contact contact1 = randomContact();
		Contact contact2 = randomContact();

		for (EntityField entityField : entityFields) {
			unsafeTriConsumer.accept(entityField, contact1, contact2);
		}

		contact1 = testGetContactOrganizationsPage_addContact(contact1);

		contact2 = testGetContactOrganizationsPage_addContact(contact2);

		for (EntityField entityField : entityFields) {
			Page<Contact> ascPage = contactResource.getContactOrganizationsPage(
				Pagination.of(1, 2), entityField.getName() + ":asc");

			assertEquals(
				Arrays.asList(contact1, contact2),
				(List<Contact>)ascPage.getItems());

			Page<Contact> descPage =
				contactResource.getContactOrganizationsPage(
					Pagination.of(1, 2), entityField.getName() + ":desc");

			assertEquals(
				Arrays.asList(contact2, contact1),
				(List<Contact>)descPage.getItems());
		}
	}

	protected Contact testGetContactOrganizationsPage_addContact(
			Contact contact)
		throws Exception {

		throw new UnsupportedOperationException(
			"This method needs to be implemented");
	}

	@Test
	public void testGetContactUserGroupsPage() throws Exception {
		Page<Contact> page = contactResource.getContactUserGroupsPage(
			Pagination.of(1, 10), null);

		long totalCount = page.getTotalCount();

		Contact contact1 = testGetContactUserGroupsPage_addContact(
			randomContact());

		Contact contact2 = testGetContactUserGroupsPage_addContact(
			randomContact());

		page = contactResource.getContactUserGroupsPage(
			Pagination.of(1, 10), null);

		Assert.assertEquals(totalCount + 2, page.getTotalCount());

		assertContains(contact1, (List<Contact>)page.getItems());
		assertContains(contact2, (List<Contact>)page.getItems());
		assertValid(page);
	}

	@Test
	public void testGetContactUserGroupsPageWithPagination() throws Exception {
		Page<Contact> totalPage = contactResource.getContactUserGroupsPage(
			null, null);

		int totalCount = GetterUtil.getInteger(totalPage.getTotalCount());

		Contact contact1 = testGetContactUserGroupsPage_addContact(
			randomContact());

		Contact contact2 = testGetContactUserGroupsPage_addContact(
			randomContact());

		Contact contact3 = testGetContactUserGroupsPage_addContact(
			randomContact());

		Page<Contact> page1 = contactResource.getContactUserGroupsPage(
			Pagination.of(1, totalCount + 2), null);

		List<Contact> contacts1 = (List<Contact>)page1.getItems();

		Assert.assertEquals(
			contacts1.toString(), totalCount + 2, contacts1.size());

		Page<Contact> page2 = contactResource.getContactUserGroupsPage(
			Pagination.of(2, totalCount + 2), null);

		Assert.assertEquals(totalCount + 3, page2.getTotalCount());

		List<Contact> contacts2 = (List<Contact>)page2.getItems();

		Assert.assertEquals(contacts2.toString(), 1, contacts2.size());

		Page<Contact> page3 = contactResource.getContactUserGroupsPage(
			Pagination.of(1, totalCount + 3), null);

		assertContains(contact1, (List<Contact>)page3.getItems());
		assertContains(contact2, (List<Contact>)page3.getItems());
		assertContains(contact3, (List<Contact>)page3.getItems());
	}

	@Test
	public void testGetContactUserGroupsPageWithSortDateTime()
		throws Exception {

		testGetContactUserGroupsPageWithSort(
			EntityField.Type.DATE_TIME,
			(entityField, contact1, contact2) -> {
				BeanTestUtil.setProperty(
					contact1, entityField.getName(),
					DateUtils.addMinutes(new Date(), -2));
			});
	}

	@Test
	public void testGetContactUserGroupsPageWithSortDouble() throws Exception {
		testGetContactUserGroupsPageWithSort(
			EntityField.Type.DOUBLE,
			(entityField, contact1, contact2) -> {
				BeanTestUtil.setProperty(contact1, entityField.getName(), 0.1);
				BeanTestUtil.setProperty(contact2, entityField.getName(), 0.5);
			});
	}

	@Test
	public void testGetContactUserGroupsPageWithSortInteger() throws Exception {
		testGetContactUserGroupsPageWithSort(
			EntityField.Type.INTEGER,
			(entityField, contact1, contact2) -> {
				BeanTestUtil.setProperty(contact1, entityField.getName(), 0);
				BeanTestUtil.setProperty(contact2, entityField.getName(), 1);
			});
	}

	@Test
	public void testGetContactUserGroupsPageWithSortString() throws Exception {
		testGetContactUserGroupsPageWithSort(
			EntityField.Type.STRING,
			(entityField, contact1, contact2) -> {
				Class<?> clazz = contact1.getClass();

				String entityFieldName = entityField.getName();

				Method method = clazz.getMethod(
					"get" + StringUtil.upperCaseFirstLetter(entityFieldName));

				Class<?> returnType = method.getReturnType();

				if (returnType.isAssignableFrom(Map.class)) {
					BeanTestUtil.setProperty(
						contact1, entityFieldName,
						Collections.singletonMap("Aaa", "Aaa"));
					BeanTestUtil.setProperty(
						contact2, entityFieldName,
						Collections.singletonMap("Bbb", "Bbb"));
				}
				else if (entityFieldName.contains("email")) {
					BeanTestUtil.setProperty(
						contact1, entityFieldName,
						"aaa" +
							StringUtil.toLowerCase(
								RandomTestUtil.randomString()) +
									"@liferay.com");
					BeanTestUtil.setProperty(
						contact2, entityFieldName,
						"bbb" +
							StringUtil.toLowerCase(
								RandomTestUtil.randomString()) +
									"@liferay.com");
				}
				else {
					BeanTestUtil.setProperty(
						contact1, entityFieldName,
						"aaa" +
							StringUtil.toLowerCase(
								RandomTestUtil.randomString()));
					BeanTestUtil.setProperty(
						contact2, entityFieldName,
						"bbb" +
							StringUtil.toLowerCase(
								RandomTestUtil.randomString()));
				}
			});
	}

	protected void testGetContactUserGroupsPageWithSort(
			EntityField.Type type,
			UnsafeTriConsumer<EntityField, Contact, Contact, Exception>
				unsafeTriConsumer)
		throws Exception {

		List<EntityField> entityFields = getEntityFields(type);

		if (entityFields.isEmpty()) {
			return;
		}

		Contact contact1 = randomContact();
		Contact contact2 = randomContact();

		for (EntityField entityField : entityFields) {
			unsafeTriConsumer.accept(entityField, contact1, contact2);
		}

		contact1 = testGetContactUserGroupsPage_addContact(contact1);

		contact2 = testGetContactUserGroupsPage_addContact(contact2);

		for (EntityField entityField : entityFields) {
			Page<Contact> ascPage = contactResource.getContactUserGroupsPage(
				Pagination.of(1, 2), entityField.getName() + ":asc");

			assertEquals(
				Arrays.asList(contact1, contact2),
				(List<Contact>)ascPage.getItems());

			Page<Contact> descPage = contactResource.getContactUserGroupsPage(
				Pagination.of(1, 2), entityField.getName() + ":desc");

			assertEquals(
				Arrays.asList(contact2, contact1),
				(List<Contact>)descPage.getItems());
		}
	}

	protected Contact testGetContactUserGroupsPage_addContact(Contact contact)
		throws Exception {

		throw new UnsupportedOperationException(
			"This method needs to be implemented");
	}

	@Test
	public void testGetContactConfiguration() throws Exception {
		Contact postContact = testGetContact_addContact();

		ContactConfiguration postContactConfiguration =
			testGetContactConfiguration_addContactConfiguration(
				postContact.getId(), randomContactConfiguration());

		ContactConfiguration getContactConfiguration =
			contactResource.getContactConfiguration(postContact.getId());

		assertEquals(postContactConfiguration, getContactConfiguration);
		assertValid(getContactConfiguration);
	}

	protected ContactConfiguration
			testGetContactConfiguration_addContactConfiguration(
				long contactId, ContactConfiguration contactConfiguration)
		throws Exception {

		throw new UnsupportedOperationException(
			"This method needs to be implemented");
	}

	protected void assertContains(Object contact, List<Object> contacts) {
		boolean contains = false;

		for (Object item : contacts) {
			if (equals(contact, item)) {
				contains = true;

				break;
			}
		}

		Assert.assertTrue(contacts + " does not contain " + contact, contains);
	}

	protected void assertHttpResponseStatusCode(
		int expectedHttpResponseStatusCode,
		HttpInvoker.HttpResponse actualHttpResponse) {

		Assert.assertEquals(
			expectedHttpResponseStatusCode, actualHttpResponse.getStatusCode());
	}

	protected void assertEquals(Object contact1, Object contact2) {
		Assert.assertTrue(
			contact1 + " does not equal " + contact2,
			equals(contact1, contact2));
	}

	protected void assertEquals(
		List<Object> contacts1, List<Object> contacts2) {

		Assert.assertEquals(contacts1.size(), contacts2.size());

		for (int i = 0; i < contacts1.size(); i++) {
			Object contact1 = contacts1.get(i);
			Object contact2 = contacts2.get(i);

			assertEquals(contact1, contact2);
		}
	}

	protected void assertEquals(
		ContactConfiguration contactConfiguration1,
		ContactConfiguration contactConfiguration2) {

		Assert.assertTrue(
			contactConfiguration1 + " does not equal " + contactConfiguration2,
			equals(contactConfiguration1, contactConfiguration2));
	}

	protected void assertEqualsIgnoringOrder(
		List<Object> contacts1, List<Object> contacts2) {

		Assert.assertEquals(contacts1.size(), contacts2.size());

		for (Object contact1 : contacts1) {
			boolean contains = false;

			for (Object contact2 : contacts2) {
				if (equals(contact1, contact2)) {
					contains = true;

					break;
				}
			}

			Assert.assertTrue(
				contacts2 + " does not contain " + contact1, contains);
		}
	}

	protected void assertValid(Object contact) throws Exception {
		boolean valid = true;

		for (String additionalAssertFieldName :
				getAdditionalAssertFieldNames()) {

			throw new IllegalArgumentException(
				"Invalid additional assert field name " +
					additionalAssertFieldName);
		}

		Assert.assertTrue(valid);
	}

	protected void assertValid(Page<Object> page) {
		boolean valid = false;

		java.util.Collection<Object> contacts = page.getItems();

		int size = contacts.size();

		if ((page.getLastPage() > 0) && (page.getPage() > 0) &&
			(page.getPageSize() > 0) && (page.getTotalCount() > 0) &&
			(size > 0)) {

			valid = true;
		}

		Assert.assertTrue(valid);
	}

	protected void assertValid(ContactConfiguration contactConfiguration) {
		boolean valid = true;

		for (String additionalAssertFieldName :
				getAdditionalContactConfigurationAssertFieldNames()) {

			if (Objects.equals("syncAllAccounts", additionalAssertFieldName)) {
				if (contactConfiguration.getSyncAllAccounts() == null) {
					valid = false;
				}

				continue;
			}

			if (Objects.equals("syncAllContacts", additionalAssertFieldName)) {
				if (contactConfiguration.getSyncAllContacts() == null) {
					valid = false;
				}

				continue;
			}

			if (Objects.equals(
					"syncedAccountGroupIds", additionalAssertFieldName)) {

				if (contactConfiguration.getSyncedAccountGroupIds() == null) {
					valid = false;
				}

				continue;
			}

			if (Objects.equals(
					"syncedOrganizationIds", additionalAssertFieldName)) {

				if (contactConfiguration.getSyncedOrganizationIds() == null) {
					valid = false;
				}

				continue;
			}

			if (Objects.equals(
					"syncedUserGroupIds", additionalAssertFieldName)) {

				if (contactConfiguration.getSyncedUserGroupIds() == null) {
					valid = false;
				}

				continue;
			}

			throw new IllegalArgumentException(
				"Invalid additional assert field name " +
					additionalAssertFieldName);
		}

		Assert.assertTrue(valid);
	}

	protected String[] getAdditionalAssertFieldNames() {
		return new String[0];
	}

	protected String[] getAdditionalContactConfigurationAssertFieldNames() {
		return new String[0];
	}

	protected List<GraphQLField> getGraphQLFields() throws Exception {
		List<GraphQLField> graphQLFields = new ArrayList<>();

		return graphQLFields;
	}

	protected List<GraphQLField> getGraphQLFields(
			java.lang.reflect.Field... fields)
		throws Exception {

		List<GraphQLField> graphQLFields = new ArrayList<>();

		for (java.lang.reflect.Field field : fields) {
			com.liferay.portal.vulcan.graphql.annotation.GraphQLField
				vulcanGraphQLField = field.getAnnotation(
					com.liferay.portal.vulcan.graphql.annotation.GraphQLField.
						class);

			if (vulcanGraphQLField != null) {
				Class<?> clazz = field.getType();

				if (clazz.isArray()) {
					clazz = clazz.getComponentType();
				}

				List<GraphQLField> childrenGraphQLFields = getGraphQLFields(
					getDeclaredFields(clazz));

				graphQLFields.add(
					new GraphQLField(field.getName(), childrenGraphQLFields));
			}
		}

		return graphQLFields;
	}

	protected String[] getIgnoredEntityFieldNames() {
		return new String[0];
	}

	protected boolean equals(Object contact1, Object contact2) {
		if (contact1 == contact2) {
			return true;
		}

		for (String additionalAssertFieldName :
				getAdditionalAssertFieldNames()) {

			throw new IllegalArgumentException(
				"Invalid additional assert field name " +
					additionalAssertFieldName);
		}

		return true;
	}

	protected boolean equals(
		Map<String, Object> map1, Map<String, Object> map2) {

		if (Objects.equals(map1.keySet(), map2.keySet())) {
			for (Map.Entry<String, Object> entry : map1.entrySet()) {
				if (entry.getValue() instanceof Map) {
					if (!equals(
							(Map)entry.getValue(),
							(Map)map2.get(entry.getKey()))) {

						return false;
					}
				}
				else if (!Objects.deepEquals(
							entry.getValue(), map2.get(entry.getKey()))) {

					return false;
				}
			}

			return true;
		}

		return false;
	}

	protected boolean equals(
		ContactConfiguration contactConfiguration1,
		ContactConfiguration contactConfiguration2) {

		if (contactConfiguration1 == contactConfiguration2) {
			return true;
		}

		for (String additionalAssertFieldName :
				getAdditionalContactConfigurationAssertFieldNames()) {

			if (Objects.equals("syncAllAccounts", additionalAssertFieldName)) {
				if (!Objects.deepEquals(
						contactConfiguration1.getSyncAllAccounts(),
						contactConfiguration2.getSyncAllAccounts())) {

					return false;
				}

				continue;
			}

			if (Objects.equals("syncAllContacts", additionalAssertFieldName)) {
				if (!Objects.deepEquals(
						contactConfiguration1.getSyncAllContacts(),
						contactConfiguration2.getSyncAllContacts())) {

					return false;
				}

				continue;
			}

			if (Objects.equals(
					"syncedAccountGroupIds", additionalAssertFieldName)) {

				if (!Objects.deepEquals(
						contactConfiguration1.getSyncedAccountGroupIds(),
						contactConfiguration2.getSyncedAccountGroupIds())) {

					return false;
				}

				continue;
			}

			if (Objects.equals(
					"syncedOrganizationIds", additionalAssertFieldName)) {

				if (!Objects.deepEquals(
						contactConfiguration1.getSyncedOrganizationIds(),
						contactConfiguration2.getSyncedOrganizationIds())) {

					return false;
				}

				continue;
			}

			if (Objects.equals(
					"syncedUserGroupIds", additionalAssertFieldName)) {

				if (!Objects.deepEquals(
						contactConfiguration1.getSyncedUserGroupIds(),
						contactConfiguration2.getSyncedUserGroupIds())) {

					return false;
				}

				continue;
			}

			throw new IllegalArgumentException(
				"Invalid additional assert field name " +
					additionalAssertFieldName);
		}

		return true;
	}

	protected java.lang.reflect.Field[] getDeclaredFields(Class clazz)
		throws Exception {

		Stream<java.lang.reflect.Field> stream = Stream.of(
			ReflectionUtil.getDeclaredFields(clazz));

		return stream.filter(
			field -> !field.isSynthetic()
		).toArray(
			java.lang.reflect.Field[]::new
		);
	}

	protected java.util.Collection<EntityField> getEntityFields()
		throws Exception {

		if (!(_contactResource instanceof EntityModelResource)) {
			throw new UnsupportedOperationException(
				"Resource is not an instance of EntityModelResource");
		}

		EntityModelResource entityModelResource =
			(EntityModelResource)_contactResource;

		EntityModel entityModel = entityModelResource.getEntityModel(
			new MultivaluedHashMap());

		Map<String, EntityField> entityFieldsMap =
			entityModel.getEntityFieldsMap();

		return entityFieldsMap.values();
	}

	protected List<EntityField> getEntityFields(EntityField.Type type)
		throws Exception {

		java.util.Collection<EntityField> entityFields = getEntityFields();

		Stream<EntityField> stream = entityFields.stream();

		return stream.filter(
			entityField ->
				Objects.equals(entityField.getType(), type) &&
				!ArrayUtil.contains(
					getIgnoredEntityFieldNames(), entityField.getName())
		).collect(
			Collectors.toList()
		);
	}

	protected String getFilterString(
		EntityField entityField, String operator, Object contact) {

		StringBundler sb = new StringBundler();

		String entityFieldName = entityField.getName();

		sb.append(entityFieldName);

		sb.append(" ");
		sb.append(operator);
		sb.append(" ");

		throw new IllegalArgumentException(
			"Invalid entity field " + entityFieldName);
	}

	protected String invoke(String query) throws Exception {
		HttpInvoker httpInvoker = HttpInvoker.newHttpInvoker();

		httpInvoker.body(
			JSONUtil.put(
				"query", query
			).toString(),
			"application/json");
		httpInvoker.httpMethod(HttpInvoker.HttpMethod.POST);
		httpInvoker.path("http://localhost:8080/o/graphql");
		httpInvoker.userNameAndPassword("test@liferay.com:test");

		HttpInvoker.HttpResponse httpResponse = httpInvoker.invoke();

		return httpResponse.getContent();
	}

	protected JSONObject invokeGraphQLMutation(GraphQLField graphQLField)
		throws Exception {

		GraphQLField mutationGraphQLField = new GraphQLField(
			"mutation", graphQLField);

		return JSONFactoryUtil.createJSONObject(
			invoke(mutationGraphQLField.toString()));
	}

	protected JSONObject invokeGraphQLQuery(GraphQLField graphQLField)
		throws Exception {

		GraphQLField queryGraphQLField = new GraphQLField(
			"query", graphQLField);

		return JSONFactoryUtil.createJSONObject(
			invoke(queryGraphQLField.toString()));
	}

	protected ContactConfiguration randomContactConfiguration()
		throws Exception {

		return new ContactConfiguration() {
			{
				syncAllAccounts = RandomTestUtil.randomBoolean();
				syncAllContacts = RandomTestUtil.randomBoolean();
			}
		};
	}

	protected ContactResource contactResource;
	protected Group irrelevantGroup;
	protected Company testCompany;
	protected Group testGroup;

	protected static class BeanTestUtil {

		public static void copyProperties(Object source, Object target)
			throws Exception {

			Class<?> sourceClass = _getSuperClass(source.getClass());

			Class<?> targetClass = target.getClass();

			for (java.lang.reflect.Field field :
					sourceClass.getDeclaredFields()) {

				if (field.isSynthetic()) {
					continue;
				}

				Method getMethod = _getMethod(
					sourceClass, field.getName(), "get");

				Method setMethod = _getMethod(
					targetClass, field.getName(), "set",
					getMethod.getReturnType());

				setMethod.invoke(target, getMethod.invoke(source));
			}
		}

		public static boolean hasProperty(Object bean, String name) {
			Method setMethod = _getMethod(
				bean.getClass(), "set" + StringUtil.upperCaseFirstLetter(name));

			if (setMethod != null) {
				return true;
			}

			return false;
		}

		public static void setProperty(Object bean, String name, Object value)
			throws Exception {

			Class<?> clazz = bean.getClass();

			Method setMethod = _getMethod(
				clazz, "set" + StringUtil.upperCaseFirstLetter(name));

			if (setMethod == null) {
				throw new NoSuchMethodException();
			}

			Class<?>[] parameterTypes = setMethod.getParameterTypes();

			setMethod.invoke(bean, _translateValue(parameterTypes[0], value));
		}

		private static Method _getMethod(Class<?> clazz, String name) {
			for (Method method : clazz.getMethods()) {
				if (name.equals(method.getName()) &&
					(method.getParameterCount() == 1) &&
					_parameterTypes.contains(method.getParameterTypes()[0])) {

					return method;
				}
			}

			return null;
		}

		private static Method _getMethod(
				Class<?> clazz, String fieldName, String prefix,
				Class<?>... parameterTypes)
			throws Exception {

			return clazz.getMethod(
				prefix + StringUtil.upperCaseFirstLetter(fieldName),
				parameterTypes);
		}

		private static Class<?> _getSuperClass(Class<?> clazz) {
			Class<?> superClass = clazz.getSuperclass();

			if ((superClass == null) || (superClass == Object.class)) {
				return clazz;
			}

			return superClass;
		}

		private static Object _translateValue(
			Class<?> parameterType, Object value) {

			if ((value instanceof Integer) &&
				parameterType.equals(Long.class)) {

				Integer intValue = (Integer)value;

				return intValue.longValue();
			}

			return value;
		}

		private static final Set<Class<?>> _parameterTypes = new HashSet<>(
			Arrays.asList(
				Boolean.class, Date.class, Double.class, Integer.class,
				Long.class, Map.class, String.class));

	}

	protected class GraphQLField {

		public GraphQLField(String key, GraphQLField... graphQLFields) {
			this(key, new HashMap<>(), graphQLFields);
		}

		public GraphQLField(String key, List<GraphQLField> graphQLFields) {
			this(key, new HashMap<>(), graphQLFields);
		}

		public GraphQLField(
			String key, Map<String, Object> parameterMap,
			GraphQLField... graphQLFields) {

			_key = key;
			_parameterMap = parameterMap;
			_graphQLFields = Arrays.asList(graphQLFields);
		}

		public GraphQLField(
			String key, Map<String, Object> parameterMap,
			List<GraphQLField> graphQLFields) {

			_key = key;
			_parameterMap = parameterMap;
			_graphQLFields = graphQLFields;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder(_key);

			if (!_parameterMap.isEmpty()) {
				sb.append("(");

				for (Map.Entry<String, Object> entry :
						_parameterMap.entrySet()) {

					sb.append(entry.getKey());
					sb.append(": ");
					sb.append(entry.getValue());
					sb.append(", ");
				}

				sb.setLength(sb.length() - 2);

				sb.append(")");
			}

			if (!_graphQLFields.isEmpty()) {
				sb.append("{");

				for (GraphQLField graphQLField : _graphQLFields) {
					sb.append(graphQLField.toString());
					sb.append(", ");
				}

				sb.setLength(sb.length() - 2);

				sb.append("}");
			}

			return sb.toString();
		}

		private final List<GraphQLField> _graphQLFields;
		private final String _key;
		private final Map<String, Object> _parameterMap;

	}

	private static final com.liferay.portal.kernel.log.Log _log =
		LogFactoryUtil.getLog(BaseContactResourceTestCase.class);

	private static DateFormat _dateFormat;

	@Inject
	private com.liferay.analytics.settings.rest.resource.v1_0.ContactResource
		_contactResource;

}