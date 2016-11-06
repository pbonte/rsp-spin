package org.rspspin.demo;

import java.util.List;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.XSD;
import org.rspspin.lang.rspql.ParserRSPQL;
import org.rspspin.util.ArgumentConstraintException;
import org.rspspin.util.TemplateManager;
import org.rspspin.util.Utils;
import org.topbraid.spin.arq.ARQ2SPIN;
import org.topbraid.spin.arq.ARQFactory;
import org.topbraid.spin.model.Module;
import org.topbraid.spin.model.Template;
import org.topbraid.spin.system.SPINArgumentChecker;
import org.topbraid.spin.system.SPINModuleRegistry;
import org.topbraid.spin.vocabulary.ARG;
import org.topbraid.spin.vocabulary.SPIN;
import org.topbraid.spin.vocabulary.SPL;

/**
 * Demonstrate RSP-SPIN API.
 */
public class Example {
	public static void main(String[] args) throws ArgumentConstraintException {
		new Example().test1();
		new Example().test2();
	}
	
	/**
	 * Create template, validate query bindings, and instantiate query using
	 * the TemplateManager.
	 * @throws ArgumentConstraintException 
	 */
	public void test1() throws ArgumentConstraintException{
		String queryString = ""
				+ "PREFIX  :     <http://debs2015.org/streams/> "
				+ "PREFIX  debs: <http://debs2015.org/onto#> "
				+ "REGISTER STREAM :rideCount AS "
				+ "SELECT ISTREAM (count(?ride) AS ?rideCount) "
				+ "FROM NAMED WINDOW :wind ON :trips [RANGE PT1H STEP PT1H] "
				+ "WHERE { "
				+ "   WINDOW :win { "
				+ "      ?ride debs:distance ?distance "
				+ "      FILTER ( ?distance > ?limit ) "
				+ "   }"
				+ "}";
		
		// Create template manager
		TemplateManager tm = new TemplateManager();
		Template template = tm.createTemplate("http://template", queryString);
				
		// Add template argument. Also tests that the argument is a variable.
		tm.addArgumentConstraint("limit", XSD.integer, ResourceFactory.createTypedLiteral("2", XSDDatatype.XSDinteger),
					true, template);
		
		QuerySolutionMap bindings = new QuerySolutionMap();
		bindings.add("limit", ResourceFactory.createTypedLiteral("2", XSDDatatype.XSDinteger));
		
		// Check that bindings are valid
		tm.check(template, bindings);
		
		// Get query from template and bindings
		Query query = tm.getQuery(template, bindings);
		System.out.println(query);
		
		// Print model
		tm.getModel().write(System.out, "TTL");
	}
	
	/**
	 * Create template, validate query bindings, and instantiate query using
	 * standard SPIN API commands.
	 * @throws ArgumentConstraintException 
	 */
	public void test2() throws ArgumentConstraintException{
		String queryString = ""
				+ "PREFIX  :     <http://debs2015.org/streams/> "
				+ "PREFIX  debs: <http://debs2015.org/onto#> "
				+ "REGISTER STREAM :rideCount AS "
				+ "SELECT ISTREAM (count(?ride) AS ?rideCount) "
				+ "FROM NAMED WINDOW :wind ON :trips [RANGE PT1H STEP PT1H] "
				+ "WHERE { "
				+ "   WINDOW :win { "
				+ "      ?ride debs:distance ?distance "
				+ "      FILTER ( ?distance > ?limit ) "
				+ "   }"
				+ "}";
		
		// Register the RSP-QL syntax
		ParserRSPQL.register();
		ARQFactory.setSyntax(ParserRSPQL.syntax);
		SPINModuleRegistry.get().init();
		
		// Install the extended argument checker or one of your choice
		SPINArgumentChecker.set(new SPINArgumentChecker() {
			@Override
			public void handleErrors(Module module, QuerySolutionMap bindings, List<String> errors)
					throws ArgumentConstraintException {
				throw new ArgumentConstraintException(errors);
			}
		});

		// Create template
		Model model = Utils.createDefaultModel();
		ARQ2SPIN arq2spin = new ARQ2SPIN(model);
		Query arqQuery = QueryFactory.create(queryString, ParserRSPQL.syntax);
		org.topbraid.spin.model.Query spinQuery = arq2spin.createQuery(arqQuery, null);
		Template template = model.createResource(null, SPIN.SelectTemplate).as(Template.class);
		template.addProperty(SPIN.body, spinQuery);
		
		// Add template argument
		Resource arg = model.createResource(SPL.Argument);
		arg.addProperty(SPL.predicate, model.createResource(ARG.NS + "limit"));
		arg.addProperty(SPL.valueType, XSD.integer);
		arg.addProperty(SPL.defaultValue, model.createTypedLiteral("2", XSDDatatype.XSDinteger));
		arg.addProperty(SPL.optional, model.createTypedLiteral(true));
		template.addProperty(SPIN.constraint, arg);
		
		
		// Check binding
		QuerySolutionMap bindings = new QuerySolutionMap();
		bindings.add("limit", ResourceFactory.createTypedLiteral("2", XSDDatatype.XSDinteger));
		SPINArgumentChecker.get().check(template, bindings);
		
		// Get ARQ query from template
		Query arq = ARQFactory.get().createQuery((org.topbraid.spin.model.Query) template.getBody());
		ParameterizedSparqlString p = new ParameterizedSparqlString(arq.toString(), bindings);
		System.out.println(p);
		
		// Print model
		model.write(System.out, "TTL");
	}

}
