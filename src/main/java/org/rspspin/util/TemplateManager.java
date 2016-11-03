package org.rspspin.util;

import java.util.List;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QuerySolutionMap;
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

	/**
	 * Initialize
	 */
	public TemplateManager() {
		ParserRSPQL.register();
		SPINModuleRegistry.get().init();

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
	 */
	public Resource createArgumentConstraint(String varName, RDFNode valueType, RDFNode defaultValue,
			boolean optional) {
		Resource arg = createResource(SPL.Argument);
		arg.addProperty(SPL.predicate, createProperty(ARG.NS + varName));
		if (valueType != null)
			arg.addProperty(SPL.valueType, valueType);
		if (defaultValue != null)
			arg.addProperty(SPL.defaultValue, defaultValue);
		arg.addProperty(SPL.optional, model.createTypedLiteral(optional));
		return arg;
	}

	/**
	 * Get a query from a template and a set of bindings.
	 * @param template
	 * @param bindings
	 * @return queryString
	 */
	public String getQuery(Template template, QuerySolutionMap bindings) {
		Query arq;
		if (template.getBody() != null) {
			Command spinQuery = template.getBody();
			arq = ARQFactory.get().createQuery((org.topbraid.spin.model.Query) spinQuery);
		} else {
			arq = ARQFactory.get().createQuery(template.getProperty(SP.text).getObject().toString());
		}
		
		System.out.println(arq);
		// Parameterized
		ParameterizedSparqlString pss = new ParameterizedSparqlString(arq.toString(), bindings);
		
		System.err.println(pss.toString());
		return null;//pss.asQuery(ParserRSPQL.syntax).toString();
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
				+ "FROM NAMED WINDOW :w3 ON :s [ITEM ?physical STEP ?physical] "
				+ "WHERE { "
				+ "   WINDOW :w1 { ?s ?p ?o FILTER(\"range\" < ?range)} "
				+ "}";
		Template template = tm.createTemplate("http://example.org/templates/1", queryString);

		// Create argument and add as constraint to template
		Resource arg1 = tm.createArgumentConstraint("out", RDFS.Resource, null, false);
		arg1.addProperty(RDFS.comment, "Set the output stream of this query");
		Resource arg2 = tm.createArgumentConstraint("range", XSD.duration, null, false);
		arg1.addProperty(RDFS.comment, "Set the logical range of the window");
		Resource arg3 = tm.createArgumentConstraint("step", XSD.duration, null, false);
		arg1.addProperty(RDFS.comment, "Set the logical step of the window");
		Resource arg4 = tm.createArgumentConstraint("s", RDFS.Resource, null, true);
		arg1.addProperty(RDFS.comment, "Set the subject of this query");
		Resource arg5 = tm.createArgumentConstraint("physical", XSD.integer, null, true);
		arg1.addProperty(RDFS.comment, "An integer");
		
		template.addProperty(SPIN.constraint, arg1);
		template.addProperty(SPIN.constraint, arg2);
		template.addProperty(SPIN.constraint, arg3);
		template.addProperty(SPIN.constraint, arg4);
		template.addProperty(SPIN.constraint, arg5);

		// Print model
		//tm.model.write(System.out, "TTL");

		// Create bindings
		QuerySolutionMap bindings = new QuerySolutionMap();
		bindings.add("out", ResourceFactory.createResource("http://ouputstream"));
		bindings.add("range", ResourceFactory.createTypedLiteral("PT2H", XSDDatatype.XSDduration));
		bindings.add("step", ResourceFactory.createTypedLiteral("PT1H", XSDDatatype.XSDduration));
		bindings.add("from", ResourceFactory.createTypedLiteral("PT5H", XSDDatatype.XSDduration));
		bindings.add("to", ResourceFactory.createTypedLiteral("PT3H", XSDDatatype.XSDduration));
		bindings.add("physical", ResourceFactory.createTypedLiteral("10", XSDDatatype.XSDinteger));
		bindings.add("p", ResourceFactory.createTypedLiteral("PT3H", XSDDatatype.XSDduration));
		SPINArgumentChecker.get().check(template, bindings);

		// Print query
		System.err.println(tm.getQuery(template, bindings));
	}
}
