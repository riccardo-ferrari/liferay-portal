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

package com.liferay.analytics.settings.rest.internal.resource.v1_0;

import com.liferay.account.model.AccountGroup;
import com.liferay.account.model.AccountGroupTable;
import com.liferay.account.service.AccountGroupLocalService;
import com.liferay.analytics.settings.configuration.AnalyticsConfiguration;
import com.liferay.analytics.settings.rest.dto.v1_0.ContactAccountGroup;
import com.liferay.analytics.settings.rest.dto.v1_0.ContactConfiguration;
import com.liferay.analytics.settings.rest.dto.v1_0.ContactOrganization;
import com.liferay.analytics.settings.rest.dto.v1_0.ContactUserGroup;
import com.liferay.analytics.settings.rest.internal.client.AnalyticsCloudClient;
import com.liferay.analytics.settings.rest.internal.dto.v1_0.converter.ContactAccountGroupDTOConverter;
import com.liferay.analytics.settings.rest.internal.dto.v1_0.converter.ContactAccountGroupDTOConverterContext;
import com.liferay.analytics.settings.rest.internal.dto.v1_0.converter.ContactOrganizationDTOConverter;
import com.liferay.analytics.settings.rest.internal.dto.v1_0.converter.ContactOrganizationDTOConverterContext;
import com.liferay.analytics.settings.rest.internal.dto.v1_0.converter.ContactUserGroupDTOConverter;
import com.liferay.analytics.settings.rest.internal.dto.v1_0.converter.ContactUserGroupDTOConverterContext;
import com.liferay.analytics.settings.rest.internal.manager.AnalyticsSettingsManager;
import com.liferay.analytics.settings.rest.resource.v1_0.ContactResource;
import com.liferay.portal.kernel.model.Organization;
import com.liferay.portal.kernel.model.OrganizationConstants;
import com.liferay.portal.kernel.model.OrganizationTable;
import com.liferay.portal.kernel.model.UserGroup;
import com.liferay.portal.kernel.model.UserGroupTable;
import com.liferay.portal.kernel.search.Sort;
import com.liferay.portal.kernel.service.OrganizationLocalService;
import com.liferay.portal.kernel.service.UserGroupLocalService;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.LinkedHashMapBuilder;
import com.liferay.portal.kernel.util.OrderByComparator;
import com.liferay.portal.kernel.util.OrderByComparatorFactoryUtil;
import com.liferay.portal.vulcan.pagination.Page;
import com.liferay.portal.vulcan.pagination.Pagination;

import java.util.LinkedHashMap;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

/**
 * @author Riccardo Ferrari
 */
@Component(
	properties = "OSGI-INF/liferay/rest/v1_0/contact.properties",
	scope = ServiceScope.PROTOTYPE, service = ContactResource.class
)
public class ContactResourceImpl extends BaseContactResourceImpl {

	@Override
	public Page<ContactAccountGroup> getContactAccountGroupsPage(
			Pagination pagination, Sort[] sorts)
		throws Exception {

		AnalyticsConfiguration analyticsConfiguration =
			_analyticsSettingsManager.getAnalyticsConfiguration(
				contextCompany.getCompanyId());

		if (sorts == null) {
			sorts = new Sort[] {new Sort("name", Sort.STRING_TYPE, false)};
		}

		Sort sort = sorts[0];

		OrderByComparator<AccountGroup> orderByComparator =
			OrderByComparatorFactoryUtil.create(
				AccountGroupTable.INSTANCE.getTableName(), sort.getFieldName(),
				!sort.isReverse());

		return Page.of(
			transform(
				_accountGroupLocalService.getAccountGroups(
					contextCompany.getCompanyId(),
					pagination.getStartPosition(), pagination.getEndPosition(),
					orderByComparator),
				accountGroup -> _contactAccountGroupDTOConverter.toDTO(
					new ContactAccountGroupDTOConverterContext(
						accountGroup.getAccountGroupId(),
						contextAcceptLanguage.getPreferredLocale(),
						analyticsConfiguration.syncedAccountGroupIds()),
					accountGroup)),
			pagination,
			_accountGroupLocalService.getAccountGroupsCount(
				contextCompany.getCompanyId()));
	}

	@Override
	public ContactConfiguration getContactConfiguration() throws Exception {
		AnalyticsConfiguration analyticsConfiguration =
			_analyticsSettingsManager.getAnalyticsConfiguration(
				contextCompany.getCompanyId());

		return new ContactConfiguration() {
			{
				syncAllAccounts = analyticsConfiguration.syncAllAccounts();
				syncAllContacts = analyticsConfiguration.syncAllContacts();
				syncedAccountGroupIds =
					analyticsConfiguration.syncedAccountGroupIds();
				syncedOrganizationIds =
					analyticsConfiguration.syncedOrganizationIds();
				syncedUserGroupIds =
					analyticsConfiguration.syncedUserGroupIds();
			}
		};
	}

	@Override
	public Page<ContactOrganization> getContactOrganizationsPage(
			Pagination pagination, Sort[] sorts)
		throws Exception {

		AnalyticsConfiguration analyticsConfiguration =
			_analyticsSettingsManager.getAnalyticsConfiguration(
				contextCompany.getCompanyId());

		if (sorts == null) {
			sorts = new Sort[] {new Sort("name", Sort.STRING_TYPE, false)};
		}

		Sort sort = sorts[0];

		OrderByComparator<Organization> orderByComparator =
			OrderByComparatorFactoryUtil.create(
				OrganizationTable.INSTANCE.getTableName(), sort.getFieldName(),
				!sort.isReverse());

		return Page.of(
			transform(
				_organizationLocalService.search(
					contextCompany.getCompanyId(),
					OrganizationConstants.ANY_PARENT_ORGANIZATION_ID, null,
					null, null, null, _getParams(),
					pagination.getStartPosition(), pagination.getEndPosition(),
					orderByComparator),
				organization -> _contactOrganizationDTOConverter.toDTO(
					new ContactOrganizationDTOConverterContext(
						organization.getOrganizationId(),
						contextAcceptLanguage.getPreferredLocale(),
						analyticsConfiguration.syncedOrganizationIds()),
					organization)),
			pagination,
			_organizationLocalService.searchCount(
				contextCompany.getCompanyId(),
				OrganizationConstants.ANY_PARENT_ORGANIZATION_ID, null, null,
				null, null, _getParams()));
	}

	@Override
	public Page<ContactUserGroup> getContactUserGroupsPage(
			Pagination pagination, Sort[] sorts)
		throws Exception {

		AnalyticsConfiguration analyticsConfiguration =
			_analyticsSettingsManager.getAnalyticsConfiguration(
				contextCompany.getCompanyId());

		if (sorts == null) {
			sorts = new Sort[] {new Sort("name", Sort.STRING_TYPE, false)};
		}

		Sort sort = sorts[0];

		OrderByComparator<UserGroup> orderByComparator =
			OrderByComparatorFactoryUtil.create(
				UserGroupTable.INSTANCE.getTableName(), sort.getFieldName(),
				!sort.isReverse());

		return Page.of(
			transform(
				_userGroupLocalService.search(
					contextCompany.getCompanyId(), null, _getParams(),
					pagination.getStartPosition(), pagination.getEndPosition(),
					orderByComparator),
				userGroup -> _contactUserGroupDTOConverter.toDTO(
					new ContactUserGroupDTOConverterContext(
						userGroup.getUserGroupId(),
						contextAcceptLanguage.getPreferredLocale(),
						analyticsConfiguration.syncedUserGroupIds()),
					userGroup)),
			pagination,
			_userGroupLocalService.searchCount(
				contextCompany.getCompanyId(), null, _getParams()));
	}

	@Override
	public void postContactConfiguration(
			ContactConfiguration contactConfiguration)
		throws Exception {

		boolean accountsSelected = false;

		if (contactConfiguration.getSyncAllAccounts() ||
			!ArrayUtil.isEmpty(
				contactConfiguration.getSyncedAccountGroupIds())) {

			accountsSelected = true;
		}

		boolean contactsSelected = false;

		if (contactConfiguration.getSyncAllContacts() ||
			!ArrayUtil.isEmpty(
				contactConfiguration.getSyncedOrganizationIds()) ||
			!ArrayUtil.isEmpty(contactConfiguration.getSyncedUserGroupIds())) {

			contactsSelected = true;
		}

		_analyticsCloudClient.updateAnalyticsDataSourceDetails(
			accountsSelected, contextCompany.getCompanyId(), null,
			contactsSelected, null);

		_analyticsSettingsManager.updateCompanyConfiguration(
			contextCompany.getCompanyId(),
			HashMapBuilder.<String, Object>put(
				"syncAllAccounts", contactConfiguration.getSyncAllAccounts()
			).put(
				"syncAllContacts", contactConfiguration.getSyncAllContacts()
			).put(
				"syncedAccountGroupIds",
				contactConfiguration.getSyncedAccountGroupIds()
			).put(
				"syncedOrganizationIds",
				contactConfiguration.getSyncedOrganizationIds()
			).put(
				"syncedUserGroupIds",
				contactConfiguration.getSyncedUserGroupIds()
			).build());
	}

	private LinkedHashMap<String, Object> _getParams() {
		return LinkedHashMapBuilder.<String, Object>put(
			"active", Boolean.TRUE
		).build();
	}

	@Reference
	private AccountGroupLocalService _accountGroupLocalService;

	@Reference
	private AnalyticsCloudClient _analyticsCloudClient;

	@Reference
	private AnalyticsSettingsManager _analyticsSettingsManager;

	@Reference
	private ContactAccountGroupDTOConverter _contactAccountGroupDTOConverter;

	@Reference
	private ContactOrganizationDTOConverter _contactOrganizationDTOConverter;

	@Reference
	private ContactUserGroupDTOConverter _contactUserGroupDTOConverter;

	@Reference
	private OrganizationLocalService _organizationLocalService;

	@Reference
	private UserGroupLocalService _userGroupLocalService;

}