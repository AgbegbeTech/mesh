package com.gentics.mesh.parameter.impl;

import java.util.HashMap;
import java.util.Map;

import org.raml.model.ParamType;
import org.raml.model.parameter.QueryParameter;

import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.parameter.AbstractParameters;
import com.gentics.mesh.parameter.PublishParameters;

/**
 * @see PublishParameters
 */
public class PublishParametersImpl extends AbstractParameters implements PublishParameters {

	public PublishParametersImpl(ActionContext ac) {
		super(ac);
	}

	public PublishParametersImpl() {
	}

	@Override
	public void validate() {
		// TODO validate query parameter value
	}

	@Override
	public String getName() {
		return "Publishing parameters";
	}

	@Override
	public Map<? extends String, ? extends QueryParameter> getRAMLParameters() {
		Map<String, QueryParameter> parameters = new HashMap<>();

		// recursive
		QueryParameter recursiveParameter = new QueryParameter();
		recursiveParameter.setDefaultValue("false");
		recursiveParameter.setDescription("Specifiy whether the invoked action should be applied recursively.");
		recursiveParameter.setExample("true");
		recursiveParameter.setRequired(false);
		recursiveParameter.setType(ParamType.BOOLEAN);
		parameters.put(RECURSIVE_PARAMETER_KEY, recursiveParameter);

		return parameters;
	}

}
