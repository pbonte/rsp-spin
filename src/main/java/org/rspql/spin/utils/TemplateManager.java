package org.rspql.spin.utils;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;

import org.rspql.lang.rspql.ParserRSPQL;
import org.topbraid.spin.arq.ARQ2SPIN;
import org.topbraid.spin.arq.ARQFactory;
import org.topbraid.spin.model.Template;
import org.topbraid.spin.system.SPINModuleRegistry;
import org.topbraid.spin.vocabulary.ARG;
import org.topbraid.spin.vocabulary.SP;
import org.topbraid.spin.vocabulary.SPIN;
import org.topbraid.spin.vocabulary.SPL;

import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolutionMap;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

public class TemplateManager {
	public Model model = ModelFactory.createDefaultModel();
	public String NS = "http://w3id.org/rsp/spin/template#";
	public SimpleDateFormat df = new SimpleDateFormat("YYYY-MM-dd'T'hh:mm:ss");

	/**
	 * Setup a new template manager
	 * 
	 * @param loadTemplates
	 */
	public TemplateManager() {
		// Initialize
		SPINModuleRegistry.get().init();
		ParserRSPQL.register();
		ARQFactory.get().setSyntax(ParserRSPQL.rspqlSyntax);
		// Eager literal validation active, required
		com.hp.hpl.jena.shared.impl.JenaParameters.enableEagerLiteralValidation = true;
	}

	/**
	 * Load templates from input stream.
	 * 
	 * @param is
	 * @param format
	 */
	public void loadTemplates(InputStream is, String format) {
		model.read(is, format);
	}

	/**
	 * Set the default namespace.
	 * 
	 * @param ns
	 */
	public void setNS(String ns) {
		this.NS = ns;
	}

	/**
	 * Add a new query template.
	 * 
	 * @param qString
	 * @param handle
	 * @param comment
	 */
	public Template createTemplate(String qString, String handle, String comment) {
		handle = handle.startsWith("http://") ? handle : NS + handle;

		Query query = QueryFactory.create(qString, ParserRSPQL.rspqlSyntax);
		Resource queryType;
		switch (query.getQueryType()) {
		case Query.QueryTypeAsk:
			// TODO: Not tested
			queryType = SPIN.AskTemplate;
			break;
		case Query.QueryTypeConstruct:
			queryType = SPIN.ConstructTemplate;
			break;
		case Query.QueryTypeSelect:
			queryType = SPIN.SelectTemplate;
			break;
		default:
			System.err.println("Unrecognized");
			return null;
		}

		ARQ2SPIN arq2SPIN = new ARQ2SPIN(model);
		org.topbraid.spin.model.Query spinQuery = arq2SPIN.createQuery(query, null);

		// Create a template
		Template template = model.createResource(handle, queryType).as(Template.class);
		template.addProperty(SPIN.body, spinQuery);
		template.addProperty(RDFS.comment, comment);
		return template;
	}

	/**
	 * Create a template argument.
	 * 
	 * @param template
	 * @param varName
	 * @param valueType
	 * @param defaultValue
	 * @param optional
	 * @param comment
	 * @return arg
	 */
	public Resource createArgument(Template template, String varName, RDFNode valueType, RDFNode defaultValue,
			boolean optional, String comment) {
		Model model = template.getModel();
		Resource arg = model.createResource(SPL.Argument);
		template.addProperty(SPIN.constraint, arg);

		arg.addProperty(SPL.predicate, model.getProperty(ARG.NS + varName));
		arg.addProperty(SPL.valueType, valueType);
		if (defaultValue != null) {
			arg.addProperty(SPL.defaultValue, defaultValue);
		}
		arg.addProperty(SPL.optional, model.createTypedLiteral(optional));
		arg.addProperty(RDFS.comment, comment);
		return arg;
	}

	/**
	 * Return a template from the template model or null.
	 * 
	 * @param handle
	 * @return
	 */
	public Template getTemplate(String handle) {
		handle = handle.startsWith("http://") ? handle : NS + handle;

		// Find template type
		NodeIterator iter = model.listObjectsOfProperty(model.createResource(handle), RDF.type);
		if (iter.hasNext()) {
			Resource r = iter.next().asResource();
			if (r.equals(SPIN.SelectTemplate)) {
				return model.createResource(handle, SPIN.SelectTemplate).as(Template.class);
			} else if (r.equals(SPIN.ConstructTemplate)) {
				return model.createResource(handle, SPIN.ConstructTemplate).as(Template.class);
			} else if (r.equals(SPIN.AskTemplate)) {
				// TODO: Not tested
				return model.createResource(handle, SPIN.AskTemplate).as(Template.class);
			} else {
				return model.createResource(handle, SPIN.Template).as(Template.class);
			}
		}
		return null;
	}

	/**
	 * Return a model describing a template or null.
	 * 
	 * @param templateId
	 * @return
	 */
	public Model getTemplateModel(String templateId) {
		templateId = templateId.startsWith("http://") ? templateId : NS + templateId;
		String getTemplate = String.format(""
				+ "PREFIX : <http://ex/> "
				+ "CONSTRUCT {?mid ?p ?end } "
				+ "FROM <http://external/template/graph> "
				+ "WHERE {"
				+ "   <%s> (!:)* ?mid ."
				+ "   OPTIONAL { ?mid ?p ?end }"
				+ "}", templateId);
		try {
			QueryExecution qe = QueryExecutionFactory.create(getTemplate, model);
			model.setNsPrefixes(TemplateUtils.getCommonPrefixes());
			return qe.execConstruct();
		} catch (Exception e) {
			System.err.println(e.getMessage());
			return null;
		}
	}

	/**
	 * Create a query from a query template. Before initiating this call the
	 * query should have been checked for constraint violations.
	 * 
	 * @param template
	 * @param bindings
	 * @return
	 */
	public String getQuery(Template template, QuerySolutionMap bindings) {

		Query arq;
		if (template.getBody() != null) {
			org.topbraid.spin.model.Query spinQuery = (org.topbraid.spin.model.Query) template.getBody();
			arq = ARQFactory.get().createQuery(spinQuery);
		} else {
			arq = ARQFactory.get().createQuery(template.getProperty(SP.text).getObject().toString());
		}

		// Set limit
		if (bindings.contains("limit")) {
			arq.setLimit(bindings.getLiteral("limit").getInt());
		}

		// Set offset
		if (bindings.contains("offset")) {
			arq.setOffset(bindings.getLiteral("offset").getInt());
		}

		// Parameterized
		ParameterizedSparqlString pss = new ParameterizedSparqlString(arq.toString(), bindings);
		return pss.toString();
	}

	/**
	 * Get list of all query templates.
	 * 
	 * @return templates
	 */
	public ArrayList<Template> getTemplateList(Resource type) {
		// Get the template from the model using a template resource handle
		Iterator<Resource> iter = model.listResourcesWithProperty(RDF.type, type);
		ArrayList<Template> templates = new ArrayList<>();
		while (iter.hasNext()) {
			Template template = iter.next().as(Template.class);
			templates.add(template);
		}
		return templates;
	}

	public ArrayList<Template> getTemplateList() {
		// Add subClasses of SPIN template
		ResIterator resIter = SPIN.getModel().listSubjectsWithProperty(RDFS.subClassOf, SPIN.Template);
		ArrayList<Template> templates = new ArrayList<>();
		while (resIter.hasNext()) {
			Iterator<Resource> iter = model.listResourcesWithProperty(RDF.type, resIter.next());
			while (iter.hasNext()) {
				Template template = iter.next().as(Template.class);
				templates.add(template);
			}
		}

		// Add top level SPIN templates
		Iterator<Resource> iter = model.listResourcesWithProperty(RDF.type, SPIN.Template);
		while (iter.hasNext()) {
			Template template = iter.next().as(Template.class);
			templates.add(template);
		}

		return templates;
	}
}
