/**
 * SPDX-FileCopyrightText: (c) 2023 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.analytics.machine.learning.recommendation.test;

import com.liferay.analytics.machine.learning.content.UserContentRecommendation;
import com.liferay.analytics.machine.learning.content.UserContentRecommendationManager;
import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.petra.function.transform.TransformUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.search.test.util.IdempotentRetryAssert;
import com.liferay.portal.test.rule.FeatureFlags;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Riccardo Ferrari
 */
@FeatureFlags("LRAC-14771")
@RunWith(Arquillian.class)
public class UserContentRecommendationManagerTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new LiferayIntegrationTestRule();

	@Before
	public void setUp() throws Exception {
		_userContentRecommendations = _addUserContentRecommendations();
	}

	@Test
	public void testGetUserContentRecommendations() throws Exception {
		UserContentRecommendation userContentRecommendation =
			_userContentRecommendations.get(
				RandomTestUtil.randomInt(
					0, _userContentRecommendations.size() - 1));

		Comparator<UserContentRecommendation>
			userContentRecommendationComparator = Comparator.comparingDouble(
				UserContentRecommendation::getScore);

		IdempotentRetryAssert.retryAssert(
			3, TimeUnit.SECONDS,
			() -> {
				_assetResultEquals(
					userContentRecommendation.getEntryClassPK(), null,
					ListUtil.sort(
						ListUtil.filter(
							_userContentRecommendations,
							recommendation ->
								recommendation.getEntryClassPK() ==
									userContentRecommendation.
										getEntryClassPK()),
						userContentRecommendationComparator.reversed()));

				return null;
			});
	}

	@Test
	public void testGetUserContentRecommendationsContextAware()
		throws Exception {

		UserContentRecommendation userContentRecommendation =
			_userContentRecommendations.get(
				RandomTestUtil.randomInt(
					0, _userContentRecommendations.size() - 1));

		Comparator<UserContentRecommendation>
			userContentRecommendationComparator = Comparator.comparingDouble(
				UserContentRecommendation::getScore);

		IdempotentRetryAssert.retryAssert(
			3, TimeUnit.SECONDS,
			() -> {
				_assetResultEquals(
					userContentRecommendation.getEntryClassPK(),
					userContentRecommendation.getAssetCategoryIds(),
					ListUtil.sort(
						TransformUtil.transform(
							_userContentRecommendations,
							recommendation -> {
								if ((recommendation.getEntryClassPK() !=
										userContentRecommendation.
											getEntryClassPK()) ||
									!_filterAssetCategories(
										recommendation.getAssetCategoryIds(),
										userContentRecommendation.
											getAssetCategoryIds())) {

									return null;
								}

								return recommendation;
							}),
						userContentRecommendationComparator.reversed()));

				return null;
			});
	}

	private List<UserContentRecommendation> _addUserContentRecommendations()
		throws Exception {

		List<UserContentRecommendation> userContentRecommendations =
			new ArrayList<>();

		for (int i = 0; i < _USER_COUNT; i++) {
			long entryClassPK = RandomTestUtil.randomLong();

			for (int j = 0; j < _RECOMMENDATION_COUNT; j++) {
				Set<Long> assetCategoryIds = new HashSet<>();

				int assetCategoryIdsSize = RandomTestUtil.randomInt(
					1, _MAX_ASSET_CATEGORY_COUNT);

				float score = 1.0F - (j / (float)_RECOMMENDATION_COUNT);

				for (int k = 0; k <= assetCategoryIdsSize; k++) {
					assetCategoryIds.add(
						(long)RandomTestUtil.randomInt(
							1, _MAX_ASSET_CATEGORY_COUNT));
				}

				userContentRecommendations.add(
					_createUserContentRecommendation(
						ArrayUtil.toLongArray(assetCategoryIds), entryClassPK,
						score));
			}
		}

		Collections.shuffle(userContentRecommendations);

		for (UserContentRecommendation userContentRecommendation :
				userContentRecommendations) {

			_userContentRecommendationManager.addUserContentRecommendation(
				userContentRecommendation);
		}

		return userContentRecommendations;
	}

	private void _assetResultEquals(
			long expectedEntryClassPK, long[] expectedAssetCategoryIds,
			List<UserContentRecommendation> expectedUserContentRecommendations)
		throws PortalException {

		List<UserContentRecommendation> userContentRecommendations =
			_userContentRecommendationManager.getUserContentRecommendations(
				expectedAssetCategoryIds, TestPropsValues.getCompanyId(),
				expectedEntryClassPK);

		int expectedRecommendationsSize = Math.min(
			10, expectedUserContentRecommendations.size());

		Assert.assertEquals(
			"Recommendation list size", expectedRecommendationsSize,
			userContentRecommendations.size());

		for (int i = 0; i < expectedRecommendationsSize; i++) {
			UserContentRecommendation expectedUserContentRecommendation =
				expectedUserContentRecommendations.get(i);

			UserContentRecommendation userContentRecommendation =
				userContentRecommendations.get(i);

			Assert.assertEquals(
				expectedUserContentRecommendation.getEntryClassPK(),
				userContentRecommendation.getEntryClassPK());

			Assert.assertEquals(
				expectedUserContentRecommendation.getRecommendedEntryClassPK(),
				userContentRecommendation.getRecommendedEntryClassPK());

			Assert.assertEquals(
				expectedUserContentRecommendation.getScore(),
				userContentRecommendation.getScore(), 0.0);
		}
	}

	private UserContentRecommendation _createUserContentRecommendation(
			long[] assetCategoryIds, long entryClassPK, float score)
		throws Exception {

		UserContentRecommendation userContentRecommendation =
			_userContentRecommendationManager.create();

		userContentRecommendation.setAssetCategoryIds(assetCategoryIds);
		userContentRecommendation.setEntryClassPK(entryClassPK);
		userContentRecommendation.setCompanyId(TestPropsValues.getCompanyId());
		userContentRecommendation.setCreateDate(new Date());
		userContentRecommendation.setRecommendedEntryClassPK(
			RandomTestUtil.randomLong());
		userContentRecommendation.setScore(score);

		return userContentRecommendation;
	}

	private boolean _filterAssetCategories(
		long[] assetCategoryIds, long[] expectedAssetCategoryIds) {

		for (long expectedAssetCategoryId : expectedAssetCategoryIds) {
			if (!ArrayUtil.contains(
					assetCategoryIds, expectedAssetCategoryId)) {

				return false;
			}
		}

		return true;
	}

	private static final int _MAX_ASSET_CATEGORY_COUNT = 5;

	private static final int _RECOMMENDATION_COUNT = 11;

	private static final int _USER_COUNT = 4;

	@Inject
	private UserContentRecommendationManager _userContentRecommendationManager;

	private List<UserContentRecommendation> _userContentRecommendations;

}