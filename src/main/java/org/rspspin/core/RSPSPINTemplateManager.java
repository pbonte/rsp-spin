package org.rspspin.core;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.apache.jena.query.ARQ;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.Syntax;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.SysRIOT;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.rspspin.lang.cqels.ParserCQELS;
import org.rspspin.lang.csparql.ParserCSPARQL;
import org.rspspin.lang.rspql.ParserRSPQL;
import org.rspspin.lang.sparqlstream.ParserSPARQLStream;
import org.rspspin.vocabulary.RSPSPIN;
import org.topbraid.spin.arq.ARQ2SPIN;
import org.topbraid.spin.arq.ARQFactory;
import org.topbraid.spin.model.Argument;
import org.topbraid.spin.model.Command;
import org.topbraid.spin.model.Template;
import org.topbraid.spin.system.SPINArgumentChecker;
import org.topbraid.spin.system.SPINModuleRegistry;
import org.topbraid.spin.vocabulary.ARG;
import org.topbraid.spin.vocabulary.SPIN;
import org.topbraid.spin.vocabulary.SPL;

/**
 * The template manager is designed to simplify working with SPIN templates. It
 * can either be run in local mode (requiring no writable endpoint) or remote
 * mode (loading templates from an endpoint).
 */
public class RSPSPINTemplateManager {
	private String templateNs;
	private LibraryEndpointServiceImpl library = null;
	private HashMap<String, Template> templates = new HashMap<String, Template>();
	private Properties props = null;
	private final Logger logger = Logger.getLogger(RSPSPINTemplateManager.class);

	/**
	 * Initialize template manager from default properties file
	 * (spinext.properties)
	 */
	public RSPSPINTemplateManager() {
		this("rspql.properties");
	}

	/**
	 * Initialize the template manager
	 */
	public RSPSPINTemplateManager(String propsPath) {
		ParserRSPQL.register();
		ParserCQELS.register();
		ParserCSPARQL.register();
		ParserSPARQLStream.register();
		
		SPINModuleRegistry.get().init();
		Syntax.defaultQuerySyntax = ParserRSPQL.syntax;
		ARQFactory.setSyntax(ParserRSPQL.syntax);
		SysRIOT.setStrictMode(true);
		// Install the argument checker for the internal SPIN checks. Extend
		// this as per your requirements (e.g. add extra injection controls).
		// Validation of template bindings should also be made by calling the
		// validation methods explicitly.
		SPINArgumentChecker.set(RSPSPINArgumentChecker.get());
		loadProperties(propsPath);
		templateNs = props.getProperty("library.template.namespace");
		try {
			library = new LibraryEndpointServiceImpl(propsPath);
		} catch (IOException e) {
			logger.error(e.getMessage());
		}
	}

	/**
	 * Load properties
	 * 
	 * @param path
	 */
	private void loadProperties(String propsPath) {
		try {
			props = new Properties();
			props.load(Thread.currentThread().getContextClassLoader().getResourceAsStream(propsPath));
		} catch (IOException e) {
			logger.error(e.getMessage());
		}
	}

	/**
	 * Load templates from the library
	 * 
	 * @throws RSPSPINException
	 */
	public void loadTemplates() throws RSPSPINException {
		if (library == null)
			throw new RSPSPINException("Remote library has not been initialized");
		ArrayList<Template> list = library.loadTemplates();
		list.forEach((t) -> {
			t.getModel().setNsPrefixes(RSPSPINUtils.getDefaultPrefixMapping());
			templates.put(t.getURI(), t);
		});
	}

	/**
	 * Reset the set of locally stored templates
	 */
	public void clearTemplates(boolean remote) {
		templates.clear();
		if (remote && library != null)
			library.clearTemplateGraph();
	}

	/**
	 * Create a new SPIN template
	 * 
	 * @param templateId
	 * @param queryString
	 * @return
	 * @throws RSPSPINException
	 */
	public Template createTemplate(String templateId, String queryString) throws RSPSPINException {
		// Create the new template in a clean model
		Model model = RSPSPINUtils.createDefaultModel();
		ARQ2SPIN arq2spin = new ARQ2SPIN(model);
		Template template = model.createResource(templateId, SPIN.Template).as(Template.class);

		// SELECT, CONSTRUCT, ASK
		Query query = QueryFactory.create(queryString);
		switch (query.getQueryType()) {
		case Query.QueryTypeSelect:
			template.addProperty(RDF.type, RSPSPIN.SelectTemplate);
			break;
		case Query.QueryTypeConstruct:
			template.addProperty(RDF.type, RSPSPIN.ConstructTemplate);
			break;
		case Query.QueryTypeAsk:
			template.addProperty(RDF.type, RSPSPIN.AskTemplate);
			break;
		default:
			throw new RSPSPINException("Unsupported template type");
		}
		template.addProperty(SPIN.body, arq2spin.createQuery(query, null));

		return template;
	}

	/**
	 * Create a SPIN template from a JSON object
	 * 
	 * @param jsonTemplate
	 * @return
	 * @throws RSPSPINException
	 */
	public Template createTemplateFromJson(JSONObject jsonTemplate) throws RSPSPINException {
		// Required
		if (!jsonTemplate.has("id"))
			throw new RSPSPINException("Missing required value for 'id'");
		if (!jsonTemplate.has("query"))
			throw new RSPSPINException("Missing required value for 'query'");

		// Check id
		String id = jsonTemplate.getString("id");
		if (!RSPSPINUtils.isValidTemplateId(id))
			throw new RSPSPINException("Template id contains illegal character(s)");

		// Create template id
		String templateId = templateNs + id;

		// Create template
		Template template = createTemplate(templateId, jsonTemplate.getString("query"));
		// Add label
		if (jsonTemplate.has("label"))
			template.addLiteral(SPIN.labelTemplate, jsonTemplate.getString("label"));
		// Add comment
		if (jsonTemplate.has("comment"))
			template.addLiteral(RDFS.comment, jsonTemplate.getString("comment"));

		// Parse parameters
		if (jsonTemplate.has("parameters")) {
			JSONArray params = jsonTemplate.getJSONArray("parameters");
			for (int i = 0; i < params.length(); i++) {
				JSONObject param = params.getJSONObject(i);
				// Read fields
				String varName = param.getString("varName");
				String valueType = param.has("valueType") ? param.getString("valueType") : null;
				String defaultValue = param.has("defaultValue") ? param.getString("defaultValue") : null;
				boolean optional = param.getBoolean("optional");

				// Create argument
				Argument argument = createArgument(varName, valueType, defaultValue, optional, template.getModel());

				// Add label and comment for argument
				if (param.has("label"))
					argument.addLiteral(RDFS.label, param.getString("label"));
				if (param.has("comment"))
					argument.addLiteral(RDFS.comment, param.getString("comment"));
				addArgumentConstraint(argument, template);
			}
		}
		return template;
	}

	/**
	 * Create a new template argument
	 * 
	 * @param varName
	 * @param valueType
	 * @param defaultValue
	 * @param optional
	 * @param isArray
	 * @param model
	 * @return
	 * @throws RSPSPINException
	 */
	public Argument createArgument(String varName, String valueType, String defaultValue, boolean optional, Model model)
			throws RSPSPINException {
		if (!RSPSPINUtils.isValidVarName(varName))
			throw new RSPSPINException(String.format("%s is an illegal variable name", varName));

		// Create argument
		Argument argument = model.createResource(SPL.Argument).as(Argument.class);
		argument.addProperty(SPL.predicate, model.createResource(ARG.NS + varName));
		if (valueType != null)
			argument.addProperty(SPL.valueType, RSPSPINUtils.createResource(valueType));
		if (defaultValue != null)
			argument.addProperty(SPL.defaultValue, RSPSPINUtils.createRDFNode(defaultValue, valueType));
		argument.addProperty(SPL.optional, model.createTypedLiteral(optional));
		return argument;
	}

	/**
	 * Add an argument constraint to a template
	 * 
	 * @param argument
	 * @param template
	 * @throws RSPSPINException
	 */
	public void addArgumentConstraint(Argument argument, Template template) throws RSPSPINException {
		// Check if the referenced variable exists
		String varName = argument.getVarName();
		String query = String.format("PREFIX : <http://spinrdf.org/sp#> ASK WHERE { <%s> (!:)*/:varName \"%s\" }",
				template.getURI(), varName);
		QueryExecution qe = QueryExecutionFactory.create(query, template.getModel());
		if (!qe.execAsk())
			throw new RSPSPINException(String.format("'%s' is not a variable in the query", varName));

		// Check if the referenced variable is a projected variable
		query = String.format(
				"PREFIX : <http://spinrdf.org/sp#> ASK WHERE { <%s> (!:)*/:resultVariables/(!:)*/:varName \"%s\" }",
				template.getURI(), varName);
		qe = QueryExecutionFactory.create(query, template.getModel());
		if (qe.execAsk())
			throw new RSPSPINException(String.format("Projected variable '%s' cannot be a parameter", varName));

		// Add argument constraint to template
		template.addProperty(SPIN.constraint, argument);
		template.getModel().add(argument.getModel());
	}

	/**
	 * Add a template to the manager
	 * 
	 * @param template
	 * @param remote
	 * @throws RSPSPINException
	 * @throws URISyntaxException
	 */
	public void addTemplate(Template template, boolean remote, boolean replace)
			throws RSPSPINException, URISyntaxException {
		if (replace)
			deleteTemplate(template.getURI(), remote);

		if (templates.containsKey(template.getURI()))
			throw new RSPSPINException(String.format("Template URI '%s' already in use", template.getURI()));
		templates.put(template.getURI(), template);
		if (remote) {
			if (library == null)
				throw new RSPSPINException("Remote library has not been initialized");
			library.storeTemplate(template);
		}
	}

	/**
	 * Get template from the manager or library. Abbreviated template id:are
	 * expanded
	 * 
	 * @param templateUri
	 * @return
	 * @throws URISyntaxException
	 * @throws RSPSPINException
	 */
	public Template getTemplate(String templateUri, boolean remote) throws URISyntaxException, RSPSPINException {
		// Attempt to expand based on prefix
		PrefixMapping prefixes = RSPSPINUtils.getDefaultPrefixMapping();
		templateUri = prefixes.expandPrefix(templateUri);
		// If there is still no scheme add the template NS
		if (new URI(templateUri).getScheme() == null)
			templateUri = templateNs + templateUri;

		// Retrieve from remote service, slower but always up to date
		if (remote) {
			if (library == null)
				throw new RSPSPINException("Remote library has not been initialized");
			return library.getTemplate(templateUri);
		}
		return templates.get(templateUri);
	}

	/**
	 * Delete a template from and manager and optionally from the library.
	 * 
	 * @param templateUri
	 * @param remote
	 * @throws RSPSPINException
	 * @throws URISyntaxException
	 */
	public void deleteTemplate(String templateUri, boolean remote) throws RSPSPINException, URISyntaxException {
		// Attempt to expand based on prefix
		PrefixMapping prefixes = RSPSPINUtils.getDefaultPrefixMapping();
		templateUri = prefixes.expandPrefix(templateUri);
		// If there is still no scheme add the template NS
		if (new URI(templateUri).getScheme() == null)
			templateUri = templateNs + templateUri;

		// Remove
		templates.remove(templateUri);
		if (remote) {
			if (library == null)
				throw new RSPSPINException("Remote library has not been initialized");
			library.deleteTemplate(templateUri);
		}
	}

	/**
	 * Get a query solution map based on a template and a set of parameters.
	 * 
	 * @param template
	 * @param parameters
	 * @return
	 * @throws RSPSPINException
	 */
	public QuerySolutionMap getBindings(Template template, JSONObject parameters) throws RSPSPINException {
		// Query solution map
		QuerySolutionMap bindings = new QuerySolutionMap();
		for (Argument arg : template.getArguments(false)) {
			String varName = arg.getVarName();
			if (parameters.has(varName)) {
				RDFNode value = RSPSPINUtils.createRDFNode(parameters.get(varName).toString(),
						arg.getValueType().toString());
				bindings.add(varName, value);
			} else if (arg.getDefaultValue() != null) {
				bindings.add(varName, arg.getDefaultValue());
			} else if (!arg.isOptional()) {
				throw new RSPSPINException(String.format("Missing value for required parameter %s", varName));
			}
		}
		// Add all other values as dummy literals (avoids potential injections)
		// Invalid variables names are silently ignored
		parameters.keys().forEachRemaining((key) -> {
			if (!bindings.contains(key) && RSPSPINUtils.isValidVarName(key)) {
				bindings.add(key, ResourceFactory.createPlainLiteral(""));
			}
		});
		return bindings;
	}

	/**
	 * Get an instantiated query from a template and a set of parameter
	 * bindings.
	 * 
	 * @param template
	 * @param parameters
	 * @return
	 * @throws RSPSPINException
	 * @throws ArgumentConstraintException
	 * @throws JSONException
	 */
	public Query instantiateQuery(Template template, QuerySolutionMap bindings)
			throws RSPSPINException, ArgumentConstraintException {
		RSPSPINArgumentChecker.get().validate(template, bindings);
		Command spinQuery = template.getBody();
		Query arq = ARQFactory.get().createQuery((org.topbraid.spin.model.Query) spinQuery);
		arq.setPrefixMapping(RSPSPINUtils.getDefaultPrefixMapping());

		// Parameterized
		ParameterizedSparqlString pss = new ParameterizedSparqlString(arq.toString(), bindings);
		return pss.asQuery();
	}

	/**
	 * Get an instantiated update from a template and a set of parameter
	 * bindings.
	 * 
	 * @param template
	 * @param parameters
	 * @return
	 * @throws RSPSPINException
	 * @throws ArgumentConstraintException
	 * @throws JSONException
	 */
	public UpdateRequest instantiateUpdate(Template template, QuerySolutionMap bindings)
			throws RSPSPINException, ArgumentConstraintException {
		RSPSPINArgumentChecker.get().validate(template, bindings);
		org.topbraid.spin.model.update.Update spinQuery = (org.topbraid.spin.model.update.Update) template.getBody();
		UpdateRequest arq = ARQFactory.get().createUpdateRequest(spinQuery);
		arq.setPrefixMapping(RSPSPINUtils.getDefaultPrefixMapping());

		// Parameterized
		ParameterizedSparqlString pss = new ParameterizedSparqlString(arq.toString(), bindings);
		return pss.asUpdate();
	}

	/**
	 * Get uninstantiated query from template
	 * 
	 * @param template
	 * @param parameters
	 * @return
	 * @throws RSPSPINException
	 */
	public Query getQuery(Template template) {
		Command spinQuery = template.getBody();
		Query arq = ARQFactory.get().createQuery((org.topbraid.spin.model.Query) spinQuery);
		arq.setPrefixMapping(RSPSPINUtils.getDefaultPrefixMapping());
		return arq;
	}

	/**
	 * Get uninstantiated update from template.
	 * 
	 * @param template
	 * @param parameters
	 * @return
	 * @throws RSPSPINException
	 */
	public UpdateRequest getUpdate(Template template) {
		org.topbraid.spin.model.update.Update spinQuery = (org.topbraid.spin.model.update.Update) template.getBody();
		UpdateRequest arq = ARQFactory.get().createUpdateRequest(spinQuery);
		arq.setPrefixMapping(RSPSPINUtils.getDefaultPrefixMapping());
		return arq;
	}

	/**
	 * Get all templates
	 * 
	 * @return
	 */
	public HashMap<String, Template> getTemplates() {
		return templates;
	}

	/**
	 * Get template as a JSON object.
	 * 
	 * @param template
	 * @return
	 * @throws RSPSPINException
	 * @throws JSONException
	 */
	public JSONObject templateToJson(Template template, boolean showQuery) throws JSONException, RSPSPINException {
		if (template == null)
			return null;

		// Create JSON representation
		PrefixMapping prefixes = RSPSPINUtils.getDefaultPrefixMapping();
		JSONObject jsonTemplate = new JSONObject();
		// Abbreviate template id based on template default namespace or
		// specified prefixes
		String id = template.getURI();
		id = id.replaceAll(templateNs, "");
		id = prefixes.shortForm(id);
		jsonTemplate.put("id", id);
		if (template.hasProperty(SPIN.labelTemplate))
			jsonTemplate.put("label", template.getLabelTemplate());
		if (template.hasProperty(RDFS.comment))
			jsonTemplate.put("comment", template.getProperty(RDFS.comment).getObject().toString());
		if (template.hasProperty(RDF.type, SPIN.UpdateTemplate)) {
			UpdateRequest arq = getUpdate(template);
			if (showQuery) {
				arq.setPrefixMapping(RSPSPINUtils.getDefaultPrefixMapping());
				jsonTemplate.put("query", arq.toString());
			}
			jsonTemplate.put("type", "update");
		} else {
			Query arq = getQuery(template);
			if (showQuery) {
				arq.setPrefixMapping(RSPSPINUtils.getDefaultPrefixMapping());
				jsonTemplate.put("query", arq.toString());
			}
			switch (arq.getQueryType()) {
			case Query.QueryTypeSelect:
				jsonTemplate.put("type", "select");
				break;
			case Query.QueryTypeConstruct:
				jsonTemplate.put("type", "construct");
				break;
			case Query.QueryTypeAsk:
				jsonTemplate.put("type", "ask");
				break;
			}
		}

		// Parameters
		List<Argument> args = template.getArguments(false);
		JSONArray params = new JSONArray();
		for (Argument arg : args) {
			JSONObject param = new JSONObject();
			param.put("varName", arg.getVarName());
			if (arg.getValueType() != null)
				param.put("valueType", prefixes.shortForm(arg.getValueType().toString()));
			if (arg.getDefaultValue() != null) {
				RDFNode defaultValue = arg.getDefaultValue();
				if (defaultValue.isLiteral()) {
					param.put("defaultValue", defaultValue.asLiteral().getValue().toString());
				} else {
					param.put("defaultValue", prefixes.shortForm(defaultValue.toString()));
				}
			}
			if (arg.hasProperty(RDFS.label))
				param.put("label", arg.getProperty(RDFS.label).getObject().toString());
			if (arg.hasProperty(RDFS.comment))
				param.put("comment", arg.getProperty(RDFS.comment).getObject().toString());
			param.put("optional", arg.isOptional());
			params.put(param);
		}
		jsonTemplate.put("parameters", params);
		return jsonTemplate;
	}
}
