package org.rspspin.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.Syntax;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;
import org.rspspin.lang.ParserRSPQL;
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
	private Model model = Utils.createDefaultModel();
	private Syntax syntax = ParserRSPQL.syntax;

	/**
	 * Initialize
	 */
	public TemplateManager() {
		ParserRSPQL.register();
		SPINModuleRegistry.get().init();
		ARQFactory.setSyntax(syntax);
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
	 * @param model
	 */
	public void getModel(Model model) {
		this.model = model;
	}

	/**
	 * Create template
	 * 
	 * @param templateUri
	 * @param queryString
	 * @return
	 */
	public Template createTemplate(String templateUri, String queryString) {
		org.topbraid.spin.model.Query spinQuery = ARQ2SPIN.parseQuery(queryString, model);
		// Get template type
		Resource templateType;
		if (spinQuery.hasProperty(RDF.type, SP.Select)) {
			templateType = SPIN.SelectTemplate;
		} else if (spinQuery.hasProperty(RDF.type, SP.Ask)) {
			templateType = SPIN.AskTemplate;
		} else if (spinQuery.hasProperty(RDF.type, SP.Construct)) {
			templateType = SPIN.ConstructTemplate;
		} else {
			return null;
		}
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
	public Resource addArgumentConstraint(String varName, RDFNode valueType, RDFNode defaultValue,
			boolean optional, Template template) throws ArgumentConstraintException {
		// Check if argument is variable in template
		QueryExecution qe = QueryExecutionFactory.create(String.format(""
				+ "PREFIX sp: <http://spinrdf.org/sp#> "
				+ "ASK WHERE { <%s> (!<:>)*/sp:varName \"%s\" }",
				template.getURI(), varName), template.getModel());
		if(!qe.execAsk()){
			template.getModel().write(System.out, "TTL");
			List<String> errors = new ArrayList<String>();
			errors.add("Argument " + varName + " is not a variable in the query");
			throw new ArgumentConstraintException(errors);
		}
		
		// Create argument
		Resource arg = createResource(SPL.Argument);
		arg.addProperty(SPL.predicate, createProperty(ARG.NS + varName));
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
	 * Get a query from a template.
	 * @param template
	 * @return query
	 */
	public Query getQuery(Template template) {
		Query arq;
		if (template.getBody() != null) {
			Command spinQuery = template.getBody();
			arq = ARQFactory.get().createQuery((org.topbraid.spin.model.Query) spinQuery);
		} else {
			arq = ARQFactory.get().createQuery(template.getProperty(SP.text).getObject().toString());
		}
		
		return arq;
	}
	
	/**
	 * Get a query from a template and a set of bindings.
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

	/**
	 * Example
	 * 
	 * @param args
	 * @throws ArgumentConstraintException
	 */
	public static void main(String[] args) throws ArgumentConstraintException {
		// Initialize a new template manager
		TemplateManager tm = new TemplateManager();
		// Use RSP-QL syntax
		ARQFactory.setSyntax(ParserRSPQL.syntax);

		// Create a template
		String queryString = ""
				+ "PREFIX : <http://example.org#> "
				+ "REGISTER STREAM ?out AS "
				+ "SELECT * "
				+ "FROM NAMED WINDOW :w1 ON :s [RANGE ?range STEP ?step] "
				+ "FROM NAMED WINDOW :w2 ON :s [FROM  NOW-?from TO NOW-?to STEP ?step] "
				+ "FROM NAMED WINDOW :w3 ON :s [ITEM ?physicalRange STEP ?physicalStep] "
				+ "WHERE { "
				+ "   WINDOW :w1 { ?s ?p ?o FILTER(\"range\" < ?range)} "
				+ "}";
		Template template = tm.createTemplate("http://example.org/templates/1", queryString);

		// Create argument constraints
		tm.addArgumentConstraint("out", RDFS.Resource, null, false, template);
		tm.addArgumentConstraint("range", XSD.duration, null, false, template);
		tm.addArgumentConstraint("step", XSD.duration, null, false, template);
		tm.addArgumentConstraint("from", XSD.duration, null, false, template);
		tm.addArgumentConstraint("to", XSD.duration, null, false, template);
		tm.addArgumentConstraint("physicalRange", XSD.integer, null, false, template);
		tm.addArgumentConstraint("physicalStep", XSD.integer, null, false, template);
		tm.addArgumentConstraint("s", RDFS.Resource, null, true, template);

		// Create bindings
		QuerySolutionMap bindings = new QuerySolutionMap();
		bindings.add("out", ResourceFactory.createResource("http://ouputstream"));
		bindings.add("range", ResourceFactory.createTypedLiteral("PT2H", XSDDatatype.XSDduration));
		bindings.add("step", ResourceFactory.createTypedLiteral("PT1H", XSDDatatype.XSDduration));
		bindings.add("from", ResourceFactory.createTypedLiteral("PT5H", XSDDatatype.XSDduration));
		bindings.add("to", ResourceFactory.createTypedLiteral("PT3H", XSDDatatype.XSDduration));
		bindings.add("physicalRange", ResourceFactory.createTypedLiteral("10", XSDDatatype.XSDinteger));
		bindings.add("physicalStep", ResourceFactory.createTypedLiteral("5", XSDDatatype.XSDinteger));
		SPINArgumentChecker.get().check(template, bindings);

		// Print query
		//System.out.println(tm.getQuery(template, bindings));
		
		// Try with standard SPARQL
		Query query = tm.getQuery(template);
		System.err.println(query);
		
		tm.setSyntax(Syntax.syntaxARQ);
		Template t = tm.createTemplate("http://example.org/templates/2", "SELECT * WHERE { ?a ?b ?c }");
		System.out.println(tm.getQuery(t));
	}

	/**
	 * Set the syntax used by the template manager.

	 * @param syntax
	 */
	public void setSyntax(Syntax syntax){
		this.syntax = syntax;
		ARQFactory.setSyntax(syntax);
	}
	
	/**
	 * Get the syntax used by the template manager.
	 
	 * @param syntax
	 * @return
	 */
	public Syntax getSyntax(){
		return syntax;
	}
}
