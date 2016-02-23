package org.rspql.spin.utils;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.jena.riot.system.PrefixMapFactory;
import org.topbraid.spin.model.Argument;
import org.topbraid.spin.model.Query;
import org.topbraid.spin.model.Template;
import org.topbraid.spin.system.SPINLabels;
import org.topbraid.spin.util.JenaUtil;
import org.topbraid.spin.vocabulary.ARG;
import org.topbraid.spin.vocabulary.SP;
import org.topbraid.spin.vocabulary.SPIN;
import org.topbraid.spin.vocabulary.SPL;

import com.hp.hpl.jena.query.QuerySolutionMap;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;

public class TemplateUtils {
	// Print error messages to System.err as default
	private static PrintWriter writer = new PrintWriter(System.err);

	/**
	 * Get the most common set of prefixes.
	 * 
	 * @return prefixMap
	 */
	public static Map<String, String> getCommonPrefixes() {
		Map<String, String> map = new HashMap<String, String>();
		map.put("sp", SP.NS);
		map.put("spl", SPL.NS);
		map.put("spin", SPIN.NS);
		map.put("rsp", SP.RSP);
		map.put("rdf", RDF.getURI());
		map.put("rdfs", RDFS.getURI());
		map.put("xsd", "http://www.w3.org/2001/XMLSchema#");
		return map;
	}

	/**
	 * Set the output stream for messages.
	 * 
	 * @param out
	 */
	public static void setOutputStream(OutputStream out) {
		TemplateUtils.writer = new PrintWriter(out);
	}

	/**
	 * Create template from SPIN query. The template is added to the model
	 * associated with the query.
	 * 
	 * @param handle
	 * @param query
	 * @return template
	 */
	public static Template createTemplate(String handle, Query query) {
		Model model = query.getModel();

		// Create a template
		Template template = model.createResource(handle, SPIN.Template).as(Template.class);
		template.addProperty(SPIN.body, query);
		return template;
	}

	/**
	 * Get a new argument for the template. Used to set constraints.
	 * 
	 * @param template
	 * @return argument
	 */
	public static Resource getArgument(Template template) {
		// Define spl:Argument at the template
		Model model = template.getModel();
		Resource argument = model.createResource(SPL.Argument);
		template.addProperty(SPIN.constraint, argument);
		return argument;
	}

	/**
	 * Create argument.
	 * 
	 * @param template
	 * @param varName
	 * @param valueType
	 * @param optional
	 * @param comment
	 */
	public static void createArgument(Template template, String varName, RDFNode valueType, boolean optional,
			String comment) {
		Resource arg = TemplateUtils.getArgument(template);
		arg.addProperty(SPL.predicate, template.getModel().getProperty(ARG.NS + varName));
		arg.addProperty(SPL.valueType, valueType);
		arg.addProperty(SPL.optional, template.getModel().createTypedLiteral(optional));
		arg.addProperty(RDFS.comment, "Range must be a time, e.g. PT10s");
	}

	/**
	 * This method is an adaptation of
	 * org.topbraid.spin.system.SPINArgumentChecker The method checks if the
	 * bindings match the required arguments.
	 * 
	 * @param args
	 * @param bindings
	 */
	public static boolean check(List<Argument> args, QuerySolutionMap bindings) {
		List<String> errors = new LinkedList<String>();
		for (Argument arg : args) {
			String varName = arg.getVarName();
			RDFNode value = bindings.get(varName);
			if (!arg.isOptional() && value == null) {
				errors.add("Missing required argument '" + varName + "'");
			} else if (value != null) {
				Resource valueType = arg.getValueType();
				if (valueType == null) {
					continue;
				}
				if (value.isResource()) {
					if (!RDFS.Resource.equals(valueType)
							&& !JenaUtil.hasIndirectType((Resource) value, valueType.inModel(value.getModel()))) {
						StringBuffer sb = new StringBuffer("Resource ");
						sb.append(SPINLabels.get().getLabel((Resource) value));
						sb.append(" for argument ");
						sb.append(varName);
						sb.append(" must have type ");
						sb.append(SPINLabels.get().getLabel(valueType));
						errors.add(sb.toString());
					}
				} else if (!RDFS.Literal.equals(valueType)) {
					String datatypeURI = value.asLiteral().getDatatypeURI();
					if (datatypeURI == null) {
						datatypeURI = XSD.xstring.getURI();
					}
					if (!valueType.getURI().equals(datatypeURI)) {
						StringBuffer sb = new StringBuffer("Argument '");
						sb.append(varName);
						sb.append("' must be of datatype ");
						sb.append(SPINLabels.get().getLabel(valueType));
						sb.append(" but ");
						sb.append(SPINLabels.get().getLabel(valueType.getModel().getResource(datatypeURI)));
						sb.append(" was found");
						errors.add(sb.toString());
					}
				}
			}
		}
		if (!errors.isEmpty()) {
			handleErrors(errors);
		}
		return errors.isEmpty();
	}

	/**
	 * Print all errors to output stream.
	 * 
	 * @param errors
	 */
	private static void handleErrors(List<String> errors) {
		for (String error : errors) {
			writer.println(error);
		}
		writer.flush();
	}

	/**
	 * Validate query bindings against template arguments
	 * 
	 * @param template
	 * @param bindings
	 * @return success
	 */
	public static boolean validate(Template template, QuerySolutionMap bindings) {
		List<Argument> arguments = template.getArguments(false);
		return TemplateUtils.check(arguments, bindings);
	}

	/**
	 * Cleans up a query string. Fixes formatting of named window durations.
	 * 
	 * @param query
	 * @return
	 */
	public static String clean(String query) {
		// Fix durations in windows

		query = query.replaceAll(
				"(STEP|RANGE|FROM|TO)(.+?)\"([A-Z0-9]+?)\"\\^\\^<http://www\\.w3\\.org/2001/XMLSchema#duration>",
				"$1$2$3");
		return query;
	}
}
