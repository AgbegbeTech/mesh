package com.gentics.cailun.core.rest.content.response;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gentics.cailun.core.rest.common.response.AbstractRestModel;
import com.gentics.cailun.core.rest.user.response.UserResponse;

public class ContentResponse extends AbstractRestModel {

	private UserResponse author;
	private Map<String, Map<String, String>> properties = new HashMap<>();
	private String type;

	private long order = 0;

	public ContentResponse() {
	}

	public UserResponse getAuthor() {
		return author;
	}

	public void setAuthor(UserResponse author) {
		this.author = author;
	}

	public Map<String, Map<String, String>> getProperties() {
		return properties;
	}

	public void addProperty(String languageKey, String key, String value) {
		Map<String, String> map = properties.get(languageKey);
		if (map == null) {
			map = new HashMap<>();
			properties.put(languageKey, map);
		}
		if (value != null) {
			map.put(key, value);
		}
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public long getOrder() {
		return order;
	}

	public void setOrder(long order) {
		this.order = order;
	}

}
