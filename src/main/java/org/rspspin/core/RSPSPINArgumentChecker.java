package org.rspspin.core;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;
import org.apache.log4j.Logger;
import org.topbraid.spin.model.Argument;
import org.topbraid.spin.model.Module;
import org.topbraid.spin.system.SPINArgumentChecker;
import org.topbraid.spin.util.JenaUtil;

/**
 * A singleton that is used by SPINARQFunction to check whether all supplied
 * arguments match the definition of the declared spl:Arguments.
 * 
 * When triggered to validate bindings against a template explicitly (i.e. not
 * as part of SPIN rule execution etc.) the method does extended validation.
 */
public class RSPSPINArgumentChecker extends SPINArgumentChecker {
	private final Logger logger = Logger.getLogger(SPINArgumentChecker.class);
	private boolean strict = true;
	private static RSPSPINArgumentChecker singleton = null;
	// The model used to check indirect typing for value types, and if set
	// should contain the relevant ontology/ontologies
	private Model ontModel = ModelFactory.createDefaultModel();

	@Override
	protected void handleErrors(Module module, QuerySolutionMap bindings, List<String> errors) {
		StringJoiner sj = new StringJoiner("\n");
		errors.forEach(sj::add);
		logger.error(sj.toString());
	}

	public static RSPSPINArgumentChecker get() {
		if (singleton == null)
			singleton = new RSPSPINArgumentChecker();
		return singleton;
	}

	public void setStrict(boolean strict) {
		this.strict = strict;
	}

	public void setOntModel(Model ontModel) {
		this.ontModel = ontModel;
	}

	public Model getOntModel() {
		return ontModel;
	}

	public void validate(Module module, QuerySolutionMap bindings) throws ArgumentConstraintException {
		List<String> errors = new LinkedList<String>();

		// Raise error on undefined parameters
		if (strict) {
			Map<String, Argument> argMap = module.getArgumentsMap();
			bindings.varNames().forEachRemaining((v) -> {
				if (!argMap.containsKey(v)) {
					errors.add(String.format("Variable '%s' is not a parameter in the template", v));
				}
			});
		}

		// Check that each binding meets the argument constraint
		for (Argument arg : module.getArguments(false)) {
			String varName = arg.getVarName();
			RDFNode value = bindings.get(varName);
			Resource valueType = arg.getValueType();

			// If null and not optional
			if (value == null) {
				if (!arg.isOptional()) {
					errors.add(String.format("Missing required parameter ", varName));
					continue;
				}
				value = arg.getDefaultValue();
			}

			// If value is null here continue
			if (value == null)
				continue;

			// If the parameter represents a list validate each member
			String e = validateBinding(varName, value, valueType);
			if (e != null)
				errors.add(e);

		}

		// Throw exception if errors were present
		if (!errors.isEmpty())
			throw new ArgumentConstraintException(errors);
	}

	/**
	 * Validate a binding. This validation supports typed and indirect types for
	 * resources provided that the information is present in the ontModel.
	 * 
	 * @param varName
	 * @param value
	 * @param valueType
	 * @return
	 */
	private String validateBinding(String varName, RDFNode value, Resource valueType) {
		if (value.isResource()) {
			// Validate URI
			if (!RSPSPINUtils.isValidUri(value.toString()))
				return String.format("Value '%s' for parameter '%s' is not a valid URI", value, varName);

			// Return if value type is null or rdfs:Resource
			if (valueType == null || RDFS.Resource.equals(valueType))
				return null;

			// Value is instance of valueType (in ontModel)
			// Associate value and value type with ontModel
			value = value.inModel(ontModel);
			valueType = valueType.inModel(ontModel);
			if (JenaUtil.hasIndirectType((Resource) value, valueType)) {
				return String.format("Resource '%s' for parameter '%s' must have direct/indirect type '%s'", value,
						varName, valueType);
			}
		} else {
			// Return if value type is rdfs:Literal
			if (RDFS.Literal.equals(valueType))
				return null;

			// Validate typed literal
			String datatypeURI = value.asLiteral().getDatatypeURI();
			if (value.asLiteral().getLanguage().length() > 0) {
				datatypeURI = XSD.xstring.getURI();
			}
			if (!valueType.getURI().equals(datatypeURI)) {
				return String.format("Literal '%s' for parameter '%s' must have datatype '%s'", value, varName,
						valueType);
			}
		}
		return null;
	}
}