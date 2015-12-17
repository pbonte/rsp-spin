package example;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;

import org.topbraid.spin.model.Argument;
import org.topbraid.spin.model.Query;
import org.topbraid.spin.model.Template;
import org.topbraid.spin.system.SPINLabels;
import org.topbraid.spin.util.JenaUtil;
import org.topbraid.spin.vocabulary.SPIN;
import org.topbraid.spin.vocabulary.SPL;

import com.hp.hpl.jena.query.QuerySolutionMap;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;

public class TemplateUtils {
	// Print messages to System.out as default
	private static PrintWriter writer = new PrintWriter(System.err);

	/**
	 * Set the output stream for messages.
	 * @param out
	 */
	public static void setOutputStream(OutputStream out){
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
	 * Get an argument for the template. 
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
	 * This method is an adaptation of org.topbraid.spin.system.SPINArgumentChecker
	 * The method checks if the bindings match the required arguments. 
	 * @param arguments
	 * @param bindings
	 */
	public static boolean check(List<Argument> arguments, QuerySolutionMap bindings) {
		List<String> errors = new LinkedList<String>();
		for(Argument arg : arguments) {
			String varName = arg.getVarName();
			RDFNode value = bindings.get(varName);
			if(!arg.isOptional() && value == null) {
				errors.add("Missing required argument '" + varName + "'");
			}
			else if(value != null) {
				Resource valueType = arg.getValueType();
				if(valueType != null) {
					if(value.isResource()) {
						if(!RDFS.Resource.equals(valueType) && !JenaUtil.hasIndirectType((Resource)value, valueType.inModel(value.getModel()))) {
							StringBuffer sb = new StringBuffer("Resource ");
							sb.append(SPINLabels.get().getLabel((Resource)value));
							sb.append(" for argument ");
							sb.append(varName);
							sb.append(" must have type ");
							sb.append(SPINLabels.get().getLabel(valueType));
							errors.add(sb.toString());
						}
					}
					else if(!RDFS.Literal.equals(valueType)) {
						String datatypeURI = value.asLiteral().getDatatypeURI();
						if(datatypeURI == null) {
							datatypeURI = XSD.xstring.getURI();
						}
						if(!valueType.getURI().equals(datatypeURI)) {
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
		}
		if(!errors.isEmpty()) {
			handleErrors(errors);
		}
		return errors.isEmpty();
	}
	
	/**
	 * Print all errors to output stream.
	 * @param errors
	 */
	private static void handleErrors(List<String> errors){
		for(String error : errors){
			writer.println(error);
		}
		writer.flush();
	}
}
