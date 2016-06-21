package org.rspql.spin.utils;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.topbraid.spin.model.Argument;
import org.topbraid.spin.model.Template;
import org.topbraid.spin.system.SPINLabels;
import org.topbraid.spin.util.JenaUtil;

import com.hp.hpl.jena.datatypes.DatatypeFormatException;
import com.hp.hpl.jena.query.QuerySolutionMap;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;

public class ArgumentHandler {
	public static Pattern urlPattern = Pattern.compile("^http(s?)://");

	/**
	 * Validate a set of bindings against a template. Type checking is done
	 * against an empty model.
	 * 
	 * @param template
	 * @param bindings
	 * @param validationErrors
	 * @param model
	 */
	public static void check(Template template, QuerySolutionMap bindings, List<String> validationErrors) {
		check(template, bindings, validationErrors, ModelFactory.createDefaultModel());
	}

	/**
	 * Validate a set of bindings against a template. Type checking is done
	 * against the provided model.
	 * 
	 * @param template
	 * @param bindings
	 * @param validationErrors
	 * @param model
	 */

	public static void check(Template template, QuerySolutionMap bindings, List<String> validationErrors, Model model) {
		for (Argument arg : template.getArguments(false)) {
			String varName = arg.getVarName();
			Resource valueType = arg.getValueType();

			if (bindings.contains(varName)) {
				RDFNode value = bindings.get(varName);
				if (value.isLiteral()) {
					if (value.asLiteral().getDatatypeURI().equals(valueType.getURI())) {
						// System.err.println("Literals matched.");
						continue;
					} else {
						StringBuffer sb = new StringBuffer("Validation error: Literal ");
						sb.append(value.asLiteral().getLexicalForm());
						sb.append(" (");
						sb.append(value.asLiteral().getDatatype().getURI().replaceAll(XSD.getURI(), "xsd:"));
						sb.append(") for argument '");
						sb.append(varName);
						sb.append("' must have datatype ");
						sb.append(SPINLabels.get().getLabel(valueType));
						validationErrors.add(sb.toString());
					}
				} else {
					if (valueType.equals(RDFS.Resource) || valueType.equals(RDF.Property)) {
						// System.err.println("Resource matched.");
						continue;
					} else {
						Resource resource = model.getResource(value.toString());
						Property property = model.getProperty(value.toString());
						Property propertyType = model.getProperty(valueType.toString());
						if (resource.equals(valueType) || JenaUtil.hasIndirectType(resource, valueType) || JenaUtil.hasSuperClass(resource, valueType) ||JenaUtil.hasSuperProperty(property, propertyType)){
							StringBuffer sb = new StringBuffer("Type of ");
							sb.append(resource);
							sb.append(" for argument '");
							sb.append(varName);
							sb.append("' is an instance/subclass/subproperty of ");
							sb.append(SPINLabels.get().getLabel(valueType));
							//System.out.println(sb.toString());
							continue;
						} else {
							StringBuffer sb = new StringBuffer("Validation error: Type of ");
							sb.append(resource);
							sb.append(" for argument '");
							sb.append(varName);
							sb.append("' must be of an instance/subclass/subproperty of ");
							sb.append(SPINLabels.get().getLabel(valueType));
							validationErrors.add(sb.toString());
							continue;
						}
					}
				}
			} else if (!arg.isOptional()) {
				validationErrors.add("Validation error: Missing required argument '" + varName + "'");
				continue;
			}
		}
	}

	/**
	 * Generate a set of typed bindings for a template based on a map of
	 * strings. Warnings and errors are appended to the errors list. This also
	 * adds default values for unspecified parameters (if available in
	 * template).
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
				errors.add(String.format("Binding warning: Variable '%s' is not defined for the template", name));
				continue;
			}

			// Get value type for parameter
			Resource valueType = arguments.get(name).getValueType();

			// Check if value type is an XSD data type
			if (valueType.getNameSpace().equals(XSD.getURI())) {
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
			} else {
				// Attempt to create resource
				Matcher m = urlPattern.matcher(value);
				if (m.find()) {
					bindings.add(name, ResourceFactory.createResource(value));
				} else {
					errors.add(String.format("Binding error: Value %s is not a proper URL", value));
				}
			}
		}

		// Set default values
		for (Argument arg : template.getArguments(false)) {
			if (!bindings.contains(arg.getVarName()) && arg.getDefaultValue() != null) {
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
	public static RDFNode createRDFNode(String value, Resource valueType, List<String> errors) {
		// Return null if no value or type given
		if (value == null || valueType == null) {
			return null;
		}

		// Check if value type is an XSD data type
		if (valueType.getNameSpace().equals(XSD.getURI())) {
			// Attempt to create typed literal
			Literal literal = null;
			try {
				String datatypeURI = valueType.getURI();
				literal = ModelFactory.createDefaultModel().createTypedLiteral(value, datatypeURI);
			} catch (DatatypeFormatException e) {
				errors.add(String.format("RDFNode error: %s", e.getMessage()));
				return null;
			}
			return literal;
		} else {
			// Attempt to create resource
			Matcher m = urlPattern.matcher(value);
			if (m.find()) {
				return ResourceFactory.createResource(value);
			} else {
				errors.add(String.format("RDFNode error: Value %s is not a proper URL", value));
				return null;
			}
		}
	}
}
