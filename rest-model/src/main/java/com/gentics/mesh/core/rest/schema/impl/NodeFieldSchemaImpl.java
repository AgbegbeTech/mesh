package com.gentics.mesh.core.rest.schema.impl;

import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.ALLOW_KEY;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.NodeFieldSchema;

/**
 * @see NodeFieldSchema
 */
public class NodeFieldSchemaImpl extends AbstractFieldSchema implements NodeFieldSchema {

	@JsonProperty("allow")
	private String[] allowedSchemas;

	@Override
	public String[] getAllowedSchemas() {
		return allowedSchemas;
	}

	@Override
	public NodeFieldSchema setAllowedSchemas(String... allowedSchemas) {
		this.allowedSchemas = allowedSchemas;
		return this;
	}

	@Override
	public String getType() {
		return FieldTypes.NODE.toString();
	}

	@Override
	public Map<String, Object> getAllChangeProperties() {
		Map<String, Object> map = super.getAllChangeProperties();
		map.put(ALLOW_KEY, getAllowedSchemas());
		return map;
	}

	@Override
	public void apply(Map<String, Object> fieldProperties) {
		super.apply(fieldProperties);
		if (fieldProperties.get(ALLOW_KEY) != null) {
			setAllowedSchemas((String[]) fieldProperties.get(ALLOW_KEY));
		}
	}
}
