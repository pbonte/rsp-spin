package org.rspspin.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.Syntax;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.rspspin.lang.rspql.ParserRSPQL;
import org.topbraid.spin.arq.ARQ2SPIN;
import org.topbraid.spin.arq.ARQFactory;
import org.topbraid.spin.model.Command;
import org.topbraid.spin.model.Module;
import org.topbraid.spin.model.Template;
import org.topbraid.spin.system.SPINArgumentChecker;
import org.topbraid.spin.system.SPINModuleRegistry;
import org.topbraid.spin.vocabulary.ARG;
import org.topbraid.spin.vocabulary.SP;
import org.topbraid.spin.vocabulary.SPIN;
import org.topbraid.spin.vocabulary.SPL;

public class TemplateManager {
	private static TemplateManager instance = null;
	private Model model;
	private Syntax syntax = ParserRSPQL.syntax;
	private ARQ2SPIN arq2spin;

	/**
	 * Initialize
	 */
	private TemplateManager() {
		ParserRSPQL.register();
		SPINModuleRegistry.get().init();
		ARQFactory.setSyntax(syntax);
		model = Utils.createDefaultModel();
		arq2spin = new ARQ2SPIN(model, true);
		// Install the argument checker
		SPINArgumentChecker.set(new SPINArgumentChecker() {
			@Override
			public void handleErrors(Module module, QuerySolutionMap bindings, List<String> errors)
					throws ArgumentConstraintException {
				throw new ArgumentConstraintException(errors);
			}
		});
	}

	/**
	 * Get singleton.
	 */
	public static TemplateManager get() {
		if (instance == null) {
			instance = new TemplateManager();
		}
		return instance;
	}

	/**
	 * Set template manager model.
	 * 
	 * @param model
	 */
	public void setModel(Model model) {
		this.model = model;
	}

	/**
	 * Get template manager model.
	 * 
	 * @return
	 */
	public Model getModel() {
		return model;
	}

	/**
	 * Create template
	 * 
	 * @param templateUri
	 * @param queryString
	 * @return
	 */
	public Template createTemplate(String templateUri, String queryString) {
		if (templateUri == null) {
			System.err.println("Template identifier must be a valid URI");
			return null;
		}
		Query arqQuery = QueryFactory.create(queryString, ParserRSPQL.syntax);

		// Get template type
		Resource templateType;
		if (arqQuery.isSelectType()) {
			templateType = SPIN.SelectTemplate;
		} else if (arqQuery.isAskType()) {
			templateType = SPIN.AskTemplate;
		} else if (arqQuery.isConstructType()) {
			templateType = SPIN.ConstructTemplate;
		} else {
			System.err.println("Invalid query type for template: " + arqQuery.getQueryType());
			return null;
		}

		// Use a blank node identifier for the query
		org.topbraid.spin.model.Query spinQuery = arq2spin.createQuery(arqQuery, null);
		Template template = createResource(templateUri, templateType).as(Template.class);
		template.addProperty(SPIN.body, spinQuery);
		return template;
	}

	/**
	 * Create a resource.
	 * 
	 * @param uri
	 * @return
	 */
	public Resource createResource(String uri) {
		return model.createResource(uri);
	}

	/**
	 * Create a typed resource.
	 * 
	 * @param type
	 * @return
	 */
	public Resource createResource(Resource type) {
		return model.createResource(type);
	}

	/**
	 * Create a typed resource.
	 * 
	 * @param uri
	 * @return
	 */
	public Resource createResource(String uri, Resource type) {
		return model.createResource(uri, type);
	}

	/**
	 * Create a property.
	 * 
	 * @param uri
	 * @return
	 */
	public Property createProperty(String uri) {
		return model.createProperty(uri);
	}

	/**
	 * Create an argument constraint.
	 * 
	 * @param varName
	 *            Name of variable
	 * @param valueType
	 *            Value type of variable binding
	 * @param defaultValue
	 *            Default value for variable (or null)
	 * @param optional
	 *            States whether the argument is required
	 * @return argument
	 * @throws ArgumentConstraintException
	 */
	public Resource addArgumentConstraint(String varName, RDFNode valueType, RDFNode defaultValue, boolean optional,
			Template template) throws ArgumentConstraintException {
		// Check if argument is variable in template
		QueryExecution qe = QueryExecutionFactory.create(String.format(
				"" + "PREFIX sp: <http://spinrdf.org/sp#> " + "ASK WHERE { <%s> (!<:>)*/sp:varName \"%s\" }",
				template.getURI(), varName), template.getModel());
		if (!qe.execAsk()) {
			template.getModel().write(System.out, "TTL");
			List<String> errors = new ArrayList<String>();
			errors.add("Argument " + varName + " is not a variable in the query");
			throw new ArgumentConstraintException(errors);
		}

		// Create argument
		Resource arg = createResource(SPL.Argument);
		arg.addProperty(SPL.predicate, createResource(ARG.NS + varName));
		if (valueType != null)
			arg.addProperty(SPL.valueType, valueType);
		if (defaultValue != null)
			arg.addProperty(SPL.defaultValue, defaultValue);
		arg.addProperty(SPL.optional, model.createTypedLiteral(optional));

		// Add constraint to template
		template.addProperty(SPIN.constraint, arg);
		return arg;
	}
	
	/**
	 * Get template from the current model based on a URI identifier.
	 * @return
	 */
	public Template getTemplate(String uri) {
		// Find template type
		NodeIterator iter = model.listObjectsOfProperty(model.createResource(uri), RDF.type);
		if (iter.hasNext()) {
			Resource r = iter.next().asResource();
			if (r.equals(SPIN.SelectTemplate)) {
				return model.createResource(uri, SPIN.SelectTemplate).as(Template.class);
			} else if (r.equals(SPIN.ConstructTemplate)) {
				return model.createResource(uri, SPIN.ConstructTemplate).as(Template.class);
			} else if (r.equals(SPIN.AskTemplate)) {
				return model.createResource(uri, SPIN.AskTemplate).as(Template.class);
			} else {
				return model.createResource(uri, SPIN.Template).as(Template.class);
			}
		}
		return null;
	}

	/**
	 * Get a query from a template and a set of bindings.
	 * 
	 * @param template
	 * @param bindings
	 * @return query
	 */
	public Query getQuery(Template template, QuerySolutionMap bindings) {
		Query arq;
		if (template.getBody() != null) {
			Command spinQuery = template.getBody();
			arq = ARQFactory.get().createQuery((org.topbraid.spin.model.Query) spinQuery);
		} else {
			arq = ARQFactory.get().createQuery(template.getProperty(SP.text).getObject().toString());
		}

		// Parameterized
		ParameterizedSparqlString pss = new ParameterizedSparqlString(arq.toString(), bindings);
		return pss.asQuery(syntax);
	}

	public void check(Template template, QuerySolutionMap bindings) throws ArgumentConstraintException {
		SPINArgumentChecker.get().check(template, bindings);
	}

	/**
	 * Set the syntax used by the template manager.
	 * 
	 * @param syntax
	 */
	public void setSyntax(Syntax syntax) {
		this.syntax = syntax;
		ARQFactory.setSyntax(syntax);
	}

	/**
	 * Get the syntax used by the template manager.
	 * 
	 * @param syntax
	 * @return
	 */
	public Syntax getSyntax() {
		return syntax;
	}
}
