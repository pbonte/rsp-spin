package org.rspql.spin.utils;

import java.util.List;
import java.util.Map;

import org.topbraid.spin.model.Argument;
import org.topbraid.spin.model.Template;
import org.topbraid.spin.system.SPINLabels;
import org.topbraid.spin.util.JenaUtil;

import com.hp.hpl.jena.datatypes.DatatypeFormatException;
import com.hp.hpl.jena.query.QuerySolutionMap;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;

public class ArgumentHandler {
	/**
	 * Validate a set of bindings against a template.
	 * 
	 * @param template
	 * @param bindings
	 * @param validationErrors
	 */

	public static void check(Template template, QuerySolutionMap bindings, List<String> validationErrors) {
		for (Argument arg : template.getArguments(false)) {
			String varName = arg.getVarName();
			RDFNode value = bindings.get(varName);
			if (!arg.isOptional() && value == null) {
				validationErrors.add("Missing required argument " + varName);
			} else if (value != null) {
				Resource valueType = arg.getValueType();
				if (valueType != null) {
					if (value.isResource()) {
						if (!RDFS.Resource.equals(valueType)
								&& !JenaUtil.hasIndirectType((Resource) value, valueType.inModel(value.getModel()))) {
							StringBuffer sb = new StringBuffer("Resource ");
							sb.append(SPINLabels.get().getLabel((Resource) value));
							sb.append(" for argument ");
							sb.append(varName);
							sb.append(" must have type ");
							sb.append(SPINLabels.get().getLabel(valueType));
							validationErrors.add(sb.toString());
						}
					} else if (!RDFS.Literal.equals(valueType)) {
						String datatypeURI = value.asLiteral().getDatatypeURI();
						if (value.asLiteral().getLanguage().length() > 0) {
							datatypeURI = XSD.xstring.getURI();
						}
						if (!valueType.getURI().equals(datatypeURI)) {
							StringBuffer sb = new StringBuffer("Literal ");
							sb.append(value.asLiteral().getLexicalForm());
							sb.append(" for argument ");
							sb.append(varName);
							sb.append(" must have datatype ");
							sb.append(SPINLabels.get().getLabel(valueType));
							validationErrors.add(sb.toString());
						}
					}
				}
			}
		}
	}

	/**
	 * Generate a set of typed bindings for a template based on a map of
	 * strings. Warnings and errors are appended to the errors list. This also
	 * adds default values for unspecified parameters (if available in template).
	 * 
	 * @param template
	 * @param parameters
	 * @param errors
	 * @return bindings
	 */
	public static QuerySolutionMap createBindings(Template template, Map<String, String> parameters,
			List<String> errors) {
		QuerySolutionMap bindings = new QuerySolutionMap();

		Map<String, Argument> arguments = template.getArgumentsMap();
		for (Map.Entry<String, String> entry : parameters.entrySet()) {
			String name = entry.getKey();
			String value = entry.getValue();
			// Skip variables in the template that are not defined
			if (!arguments.containsKey(entry.getKey())) {
				errors.add(String.format("Binding warning: Variable %s is not defined for the template", name));
				continue;
			}

			// Create binding
			Resource valueType = arguments.get(name).getValueType();
			if (valueType.equals(RDFS.Resource)) {
				if (!value.startsWith("http://")) {
					errors.add(String.format("Binding error: Value %s is not a resource (e.g. http://example.org/)",
							value));
				} else {
					bindings.add(name, ResourceFactory.createResource(value));
				}
			} else {
				// Attempt to create typed literal
				Literal literal = null;
				try {
					String datatypeURI = valueType.getURI();
					literal = template.getModel().createTypedLiteral(value, datatypeURI);
				} catch (DatatypeFormatException e) {
					errors.add(String.format("Binding error: %s", e.getMessage()));
					continue;
				}
				if (literal != null) {
					bindings.add(name, literal);
				}
			}
		}
		
		// Set default values
		for (Argument arg : template.getArguments(false)) {
			if(!bindings.contains(arg.getVarName()) && arg.getDefaultValue() != null){
				bindings.add(arg.getVarName(), arg.getDefaultValue());
			}
		}
		
		return bindings;
	}

	/**
	 * Create an RDF node from a string value.
	 * 
	 * @param value
	 * @param valueType
	 * @param errors
	 * @return rdfNode
	 */
	public static RDFNode createRDFNode(String value, RDFNode valueType, List<String> errors) {
		if (value == null) {
			return null;
		}

		if (valueType.equals(RDFS.Resource)) {
			if (!value.startsWith("http://")) {
				errors.add(String.format("Create RDFNode error: Value %s is not a resource (e.g. http://example.org/)",
						value));
				return null;
			} else {
				return ResourceFactory.createResource(value);
			}
		} else {
			// Attempt to create typed literal
			Literal literal = null;
			try {
				String datatypeURI = valueType.asResource().getURI();
				literal = ModelFactory.createDefaultModel().createTypedLiteral(value, datatypeURI);
			} catch (DatatypeFormatException e) {
				errors.add(String.format("Binding error: %s", e.getMessage()));
			}
			return literal;
		}
	}
}
