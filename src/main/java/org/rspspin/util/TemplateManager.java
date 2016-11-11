package org.rspspin.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.Syntax;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.update.Update;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.vocabulary.RDF;
import org.rspspin.lang.rspql.ParserRSPQL;
import org.topbraid.spin.arq.ARQ2SPIN;
import org.topbraid.spin.arq.ARQFactory;
import org.topbraid.spin.model.Argument;
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
	private Map<String, Template> templates = new HashMap<String, Template>();

	/**
	 * Initialize
	 */
	private TemplateManager() {
		ParserRSPQL.register();
		SPINModuleRegistry.get().init();
		ARQFactory.setSyntax(syntax);
		model = Utils.createDefaultModel();
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
		synchronizeTemplatesWithModel();
	}

	/**
	 * Reload the currently active templates from the TemplateHandler model.
	 */
	public void synchronizeTemplatesWithModel() {
		templates.clear();
		ResIterator iter = model.listResourcesWithProperty(RDF.type, SPIN.Template);
		while (iter.hasNext()) {
			Template t = iter.next().as(Template.class);
			templates.put(t.getURI(), t);
		}
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
	 * @throws Exception
	 */
	public Template createTemplate(String templateUri, String queryString) throws Exception {
		if (!Utils.validateUri(templateUri)) {
			throw new Exception("Template identifier must be a valid URI");
		} else if (templates.containsKey(templateUri)) {
			throw new Exception("This template URI is already in use");
		}

		// Use a blank model
		Model model = ModelFactory.createDefaultModel();
		ARQ2SPIN arq2spin = new ARQ2SPIN(model);
		// Create template
		Template template = model.createResource(templateUri, SPIN.Template).as(Template.class);

		try {
			Query query = QueryFactory.create(queryString, ParserRSPQL.syntax);
			if (query.isSelectType()) {
				template.addProperty(RDF.type, SPIN.SelectTemplate);
			} else if (query.isAskType()) {
				template.addProperty(RDF.type, SPIN.AskTemplate);
			} else if (query.isConstructType()) {
				template.addProperty(RDF.type, SPIN.ConstructTemplate);
			} else {
				System.err.println("Invalid query type for template: " + query.getQueryType());
				return null;
			}
			template.addProperty(SPIN.body, arq2spin.createQuery(query, null));
		} catch (Exception e) {
			UpdateRequest updateRequest = UpdateFactory.create(queryString, Syntax.syntaxARQ);
			Update update = updateRequest.iterator().next();
			template.addProperty(SPIN.body, arq2spin.createUpdate(update, null));
			template.addProperty(RDF.type, SPIN.UpdateTemplate);
		}
		return template;
	}

	/**
	 * Add a template to the current set of of templates, and add the associated
	 * template model to the TemplateManager model.
	 * 
	 * @param template
	 */
	public void addTemplate(Template template) {
		model.add(template.getModel());
		synchronizeTemplatesWithModel();
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
	 * @return
	 * @throws ArgumentConstraintException
	 */
	public void addArgumentConstraint(Argument argument, Template template) throws ArgumentConstraintException {
		// Check if argument varName is variable in template
		String varName = argument.getVarName();
		QueryExecution qe = QueryExecutionFactory
				.create(String.format("PREFIX : <http://spinrdf.org/sp#> ASK WHERE { <%s> (!<:>)*/:varName \"%s\" }",
						template.getURI(), varName), template.getModel());
		if (!qe.execAsk()) {
			List<String> errors = new ArrayList<String>();
			errors.add("Variable '" + varName + "' is not a variable in the query");
			throw new ArgumentConstraintException(errors);
		}
		// Add constraint to template
		template.addProperty(SPIN.constraint, argument);
		template.getModel().add(argument.getModel());
	}

	/**
	 * Create an argument.
	 * 
	 * @param varName
	 * @param valueType
	 * @param defaultValue
	 * @param optional
	 * @return
	 * @throws ArgumentConstraintException
	 */
	public Argument createArgumentConstraint(String varName, RDFNode valueType, RDFNode defaultValue, boolean optional)
			throws ArgumentConstraintException {
		if (varName == null) {
			ArrayList<String> errors = new ArrayList<>();
			errors.add("Variable '" + varName + "' is not a valid variable name");
			throw new ArgumentConstraintException(errors);
		}

		// Create argument
		Argument argument = ModelFactory.createDefaultModel().createResource(SPL.Argument).as(Argument.class);
		argument.addProperty(SPL.predicate, createResource(ARG.NS + varName));
		if (valueType != null)
			argument.addProperty(SPL.valueType, valueType);
		if (defaultValue != null)
			argument.addProperty(SPL.defaultValue, defaultValue);
		argument.addProperty(SPL.optional, model.createTypedLiteral(optional));
		return argument;
	}

	/**
	 * Get template from the current model based on a URI identifier.
	 * 
	 * @return
	 */
	public Template getTemplate(String uri) {
		return templates.get(uri);
	}

	/**
	 * Get all templates.
	 * 
	 * @return
	 */
	public Map<String, Template> getTemplates() {
		return templates;
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

		// Add default values
		for (Argument arg : template.getArguments(false)) {
			if (!bindings.contains(arg.getVarName()) && arg.getDefaultValue() != null) {
				bindings.add(arg.getVarName(), arg.getDefaultValue());
			}
		}

		// Parameterized
		ParameterizedSparqlString pss = new ParameterizedSparqlString(arq.toString(), bindings);
		return pss.asQuery(syntax);
	}

	/**
	 * Get a query from a template and a set of bindings.
	 * 
	 * @param template
	 * @param bindings
	 * @return
	 */
	public UpdateRequest getUpdate(Template template, QuerySolutionMap bindings) {
		org.topbraid.spin.model.update.Update spinQuery = (org.topbraid.spin.model.update.Update) template.getBody();
		UpdateRequest arq = ARQFactory.get().createUpdateRequest(spinQuery);

		// Add default values
		for (Argument arg : template.getArguments(false)) {
			if (!bindings.contains(arg.getVarName()) && arg.getDefaultValue() != null) {
				bindings.add(arg.getVarName(), arg.getDefaultValue());
			}
		}

		// Parameterized
		ParameterizedSparqlString pss = new ParameterizedSparqlString(arq.toString(), bindings);
		return pss.asUpdate();
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
