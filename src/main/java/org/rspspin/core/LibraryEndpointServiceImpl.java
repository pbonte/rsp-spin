package org.rspspin.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Properties;

import org.apache.jena.atlas.web.auth.HttpAuthenticator;
import org.apache.jena.atlas.web.auth.SimpleAuthenticator;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.vocabulary.RDF;
import org.topbraid.spin.model.Template;
import org.topbraid.spin.vocabulary.SPIN;

/**
 * This class provides methods for managing templates persisted in a triple
 * store. The service supports triple stores protected by basic user/password
 * authentication.
 */

public class LibraryEndpointServiceImpl implements LibraryEndpointService {
	private String graph;
	private String username;
	private String password;
	private String queryEndpoint;
	private String updateEndpoint;

	public LibraryEndpointServiceImpl(String propsPath) throws IOException {
		Properties props = new Properties();
		props.load(Thread.currentThread().getContextClassLoader().getResourceAsStream(propsPath));
		graph = props.getProperty("library.endpoint.graph");
		username = props.getProperty("library.endpoint.username");
		password = props.getProperty("library.endpoint.password");
		queryEndpoint = props.getProperty("library.endpoint.query");
		updateEndpoint = props.getProperty("library.endpoint.update");
	}

	/**
	 * Store a template in library. Replace indicates that the new template should completely replace the existing one.
	 * 
	 * @param template
	 * @param template
	 * @throws RSPSPINException
	 * @throws Exception
	 */
	public void storeTemplate(Template template) throws RSPSPINException {
		// Copy the template model
		Model model = ModelFactory.createDefaultModel();
		model.add(template.getModel().listStatements());

		// Write the BGPs to baos
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(baos);
		model.write(ps, "TTL");
		ps.flush();
		
		// Check if exists
		if(existsInLibrary(template))
			throw new RSPSPINException(String.format("Template URI '%s' already in use", template.getURI()));

		// Create update
		UpdateRequest update = UpdateFactory
				.create(String.format(""
						+ "PREFIX spin: <http://spinrdf.org/spin#> "
						+ "INSERT {"
						+ "   GRAPH <%s> { %s }"
						+ "}"
						+ "WHERE {}", graph, baos.toString(), graph, template.getURI()));
		if (username != null && password != null) {
			HttpAuthenticator authenticator = new SimpleAuthenticator(username, password.toCharArray());
			UpdateProcessor proc = UpdateExecutionFactory.createRemote(update, updateEndpoint, authenticator);
			proc.execute();
		} else {
			UpdateProcessor proc = UpdateExecutionFactory.createRemote(update, updateEndpoint);
			proc.execute();
		}
	}

	/**
	 * Check if template exists in library.
	 * 
	 * @param template
	 * @return
	 */
	private boolean existsInLibrary(Template template) {
		// Check if exists
		String queryString = String.format(""
						+ "PREFIX spin: <http://spinrdf.org/spin#> "
						+ "ASK "
						+ "WHERE {"
						+ "   GRAPH <%s> { <%s> a spin:Template }"
						+ "}", graph, template.getURI());
		System.out.println(queryString);
		Query query = QueryFactory.create(queryString);
		// Use authentication if user/password provided
		QueryExecution qe;
		if (username != null && password != null) {
			HttpAuthenticator authenticator = new SimpleAuthenticator(username, password.toCharArray());
			qe = QueryExecutionFactory.sparqlService(queryEndpoint, query, authenticator);
		} else {
			qe = QueryExecutionFactory.sparqlService(queryEndpoint, query);
		}
		return qe.execAsk();
	}

	/**
	 * Set endpoint credentials programmatically.
	 * 
	 * @param username
	 */
	public void setAuthentication(String username, String password) {
		this.username = username;
		this.password = password;
	}

	/**
	 * Set the template graph programmatically.
	 * 
	 * @param user
	 */
	public void setGraph(String graph) {
		this.graph = graph;
	}

	/**
	 * Set query endpoint programmatically.
	 * 
	 * @param queryEndoint
	 */
	public void setQueryEndpoint(String queryEndpoint) {
		this.queryEndpoint = queryEndpoint;
	}

	/**
	 * Set update endpoint URL.
	 * 
	 * @param updateEndpoint
	 */
	public void setUpdateEndpoint(String updateEndpoint) {
		this.updateEndpoint = updateEndpoint;
	}

	/**
	 * Get update endpoint URL.
	 * 
	 * @return endpoint
	 */
	public String getUpdateEndpoint() {
		return updateEndpoint;
	}

	/**
	 * Get query endpoint URL.
	 * 
	 * @return
	 */
	public String getQueryEndpoint() {
		return queryEndpoint;
	}

	/**
	 * Drop the library template graph
	 */
	public void clearTemplateGraph() {
		if (updateEndpoint == null)
			return;
		// Create update
		UpdateRequest update = UpdateFactory.create(String.format("DROP GRAPH <%s>", graph));
		UpdateProcessor proc;
		if (username != null && password != null) {
			HttpAuthenticator authenticator = new SimpleAuthenticator(username, password.toCharArray());
			proc = UpdateExecutionFactory.createRemote(update, updateEndpoint, authenticator);
		} else {
			proc = UpdateExecutionFactory.createRemote(update, updateEndpoint);
		}
		proc.execute();
	}

	public Template getTemplate(String uri) {
		String queryString = String.format(""
				+ "PREFIX : <http://spinrdf.org/spin#> "
				+ "CONSTRUCT { ?s ?p ?o } "
				+ "FROM <%s> "
				+ "WHERE { "
				+ "   <%s> a :Template ; "
				+ "        (!:)* ?s . "
				+ "   ?s ?p ?o ."
				+ "}", graph, uri);
		Query query = QueryFactory.create(queryString);
		// Use authentication if user/password provided
		QueryExecution qe;
		if (username != null && password != null) {
			HttpAuthenticator authenticator = new SimpleAuthenticator(username, password.toCharArray());
			qe = QueryExecutionFactory.sparqlService(queryEndpoint, query, authenticator);
		} else {
			qe = QueryExecutionFactory.sparqlService(queryEndpoint, query);
		}
		Model model = qe.execConstruct();
		if(model.isEmpty())
			return null;
		Template template = model.getProperty(uri).as(Template.class);
		template.getModel().setNsPrefixes(RSPSPINUtils.getDefaultPrefixMapping());
		return template;
	}

	public void deleteTemplate(String uri) {
		String queryString = String.format(""
				+ "PREFIX : <http://spinrdf.org/spin#> "
				+ "DELETE { GRAPH <%s> { ?s ?p ?o } } "
				+ "WHERE { "
				+ "   GRAPH <%s> {"
				+ "      <%s> a :Template ; "
				+ "           (!:)* ?s . "
				+ "      ?s ?p ?o ."
				+ "   }"
				+ "}", graph, graph, uri);
		UpdateRequest update = UpdateFactory.create(queryString);
		// Use authentication if user/password provided
		UpdateProcessor up;
		if (username != null && password != null) {
			HttpAuthenticator authenticator = new SimpleAuthenticator(username, password.toCharArray());
			up = UpdateExecutionFactory.createRemote(update, updateEndpoint, authenticator);
		} else {
			up = UpdateExecutionFactory.createRemote(update, updateEndpoint);
		}
		up.execute();
	}

	public ArrayList<Template> loadTemplates() {
		String queryString = String.format(""
				+ "PREFIX : <http://spinrdf.org/spin#> "
				+ "CONSTRUCT { ?s ?p ?o } "
				+ "FROM <%s> "
				+ "WHERE { "
				+ "   [] a :Template ; "
				+ "        (!:)* ?s . "
				+ "   ?s ?p ?o ."
				+ "}", graph);
		Query query = QueryFactory.create(queryString);
		
		// Use authentication if user/password provided
		QueryExecution qe;
		if (username != null && password != null) {
			HttpAuthenticator authenticator = new SimpleAuthenticator(username, password.toCharArray());
			qe = QueryExecutionFactory.sparqlService(queryEndpoint, query, authenticator);
		} else {
			qe = QueryExecutionFactory.sparqlService(queryEndpoint, query);
		}
		Model model = qe.execConstruct();
		
		// Pull out models for each template
		ArrayList<Template> templates = new ArrayList<>();
		model.listSubjectsWithProperty(RDF.type, SPIN.Template).forEachRemaining((uri) -> {
			String q = String.format(""
					+ "PREFIX : <http://spinrdf.org/spin#> "
					+ "CONSTRUCT { ?s ?p ?o } "
					+ "FROM <%s> "
					+ "WHERE { "
					+ "   <%s> a :Template ; "
					+ "        (!:)* ?s . "
					+ "   ?s ?p ?o ."
					+ "}", graph, uri);
			Model m = QueryExecutionFactory.create(q, model).execConstruct();
			templates.add(uri.inModel(m).as(Template.class));
		});
		return templates;
	}
}
