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

package com.liferay.analytics.settings.rest.client.serdes.v1_0;

import com.liferay.analytics.settings.rest.client.dto.v1_0.Group;
import com.liferay.analytics.settings.rest.client.json.BaseJSONParser;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import javax.annotation.Generated;

/**
 * @author Riccardo Ferrari
 * @generated
 */
@Generated("")
public class GroupSerDes {

	public static Group toDTO(String json) {
		GroupJSONParser groupJSONParser = new GroupJSONParser();

		return groupJSONParser.parseToDTO(json);
	}

	public static Group[] toDTOs(String json) {
		GroupJSONParser groupJSONParser = new GroupJSONParser();

		return groupJSONParser.parseToDTOs(json);
	}

	public static String toJSON(Group group) {
		if (group == null) {
			return "null";
		}

		StringBuilder sb = new StringBuilder();

		sb.append("{");

		if (group.getAttributes() != null) {
			if (sb.length() > 1) {
				sb.append(", ");
			}

			sb.append("\"attributes\": ");

			sb.append(_toJSON(group.getAttributes()));
		}

		if (group.getChannelName() != null) {
			if (sb.length() > 1) {
				sb.append(", ");
			}

			sb.append("\"channelName\": ");

			sb.append("\"");

			sb.append(_escape(group.getChannelName()));

			sb.append("\"");
		}

		if (group.getGroupType() != null) {
			if (sb.length() > 1) {
				sb.append(", ");
			}

			sb.append("\"groupType\": ");

			sb.append("\"");

			sb.append(_escape(group.getGroupType()));

			sb.append("\"");
		}

		if (group.getId() != null) {
			if (sb.length() > 1) {
				sb.append(", ");
			}

			sb.append("\"id\": ");

			sb.append(group.getId());
		}

		if (group.getName() != null) {
			if (sb.length() > 1) {
				sb.append(", ");
			}

			sb.append("\"name\": ");

			sb.append("\"");

			sb.append(_escape(group.getName()));

			sb.append("\"");
		}

		sb.append("}");

		return sb.toString();
	}

	public static Map<String, Object> toMap(String json) {
		GroupJSONParser groupJSONParser = new GroupJSONParser();

		return groupJSONParser.parseToMap(json);
	}

	public static Map<String, String> toMap(Group group) {
		if (group == null) {
			return null;
		}

		Map<String, String> map = new TreeMap<>();

		if (group.getAttributes() == null) {
			map.put("attributes", null);
		}
		else {
			map.put("attributes", String.valueOf(group.getAttributes()));
		}

		if (group.getChannelName() == null) {
			map.put("channelName", null);
		}
		else {
			map.put("channelName", String.valueOf(group.getChannelName()));
		}

		if (group.getGroupType() == null) {
			map.put("groupType", null);
		}
		else {
			map.put("groupType", String.valueOf(group.getGroupType()));
		}

		if (group.getId() == null) {
			map.put("id", null);
		}
		else {
			map.put("id", String.valueOf(group.getId()));
		}

		if (group.getName() == null) {
			map.put("name", null);
		}
		else {
			map.put("name", String.valueOf(group.getName()));
		}

		return map;
	}

	public static class GroupJSONParser extends BaseJSONParser<Group> {

		@Override
		protected Group createDTO() {
			return new Group();
		}

		@Override
		protected Group[] createDTOArray(int size) {
			return new Group[size];
		}

		@Override
		protected void setField(
			Group group, String jsonParserFieldName,
			Object jsonParserFieldValue) {

			if (Objects.equals(jsonParserFieldName, "attributes")) {
				if (jsonParserFieldValue != null) {
					group.setAttributes(
						(Map)GroupSerDes.toMap((String)jsonParserFieldValue));
				}
			}
			else if (Objects.equals(jsonParserFieldName, "channelName")) {
				if (jsonParserFieldValue != null) {
					group.setChannelName((String)jsonParserFieldValue);
				}
			}
			else if (Objects.equals(jsonParserFieldName, "groupType")) {
				if (jsonParserFieldValue != null) {
					group.setGroupType((String)jsonParserFieldValue);
				}
			}
			else if (Objects.equals(jsonParserFieldName, "id")) {
				if (jsonParserFieldValue != null) {
					group.setId(Long.valueOf((String)jsonParserFieldValue));
				}
			}
			else if (Objects.equals(jsonParserFieldName, "name")) {
				if (jsonParserFieldValue != null) {
					group.setName((String)jsonParserFieldValue);
				}
			}
		}

	}

	private static String _escape(Object object) {
		String string = String.valueOf(object);

		for (String[] strings : BaseJSONParser.JSON_ESCAPE_STRINGS) {
			string = string.replace(strings[0], strings[1]);
		}

		return string;
	}

	private static String _toJSON(Map<String, ?> map) {
		StringBuilder sb = new StringBuilder("{");

		@SuppressWarnings("unchecked")
		Set set = map.entrySet();

		@SuppressWarnings("unchecked")
		Iterator<Map.Entry<String, ?>> iterator = set.iterator();

		while (iterator.hasNext()) {
			Map.Entry<String, ?> entry = iterator.next();

			sb.append("\"");
			sb.append(entry.getKey());
			sb.append("\": ");

			Object value = entry.getValue();

			Class<?> valueClass = value.getClass();

			if (value instanceof Map) {
				sb.append(_toJSON((Map)value));
			}
			else if (valueClass.isArray()) {
				Object[] values = (Object[])value;

				sb.append("[");

				for (int i = 0; i < values.length; i++) {
					sb.append("\"");
					sb.append(_escape(values[i]));
					sb.append("\"");

					if ((i + 1) < values.length) {
						sb.append(", ");
					}
				}

				sb.append("]");
			}
			else if (value instanceof String) {
				sb.append("\"");
				sb.append(_escape(entry.getValue()));
				sb.append("\"");
			}
			else {
				sb.append(String.valueOf(entry.getValue()));
			}

			if (iterator.hasNext()) {
				sb.append(", ");
			}
		}

		sb.append("}");

		return sb.toString();
	}

}