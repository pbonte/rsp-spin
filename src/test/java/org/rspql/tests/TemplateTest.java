package org.rspql.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.Test;
import org.rspql.spin.utils.ArgumentHandler;
import org.rspql.spin.utils.TemplateManager;
import org.rspql.spin.utils.TemplateUtils;
import org.topbraid.spin.arq.ARQFactory;
import org.topbraid.spin.model.Template;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QuerySolutionMap;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;

public class TemplateTest {
	public String NS = "http://w3id.org/rsp/spin#";
	public TemplateManager tm = new TemplateManager();
	
	public static void main(String[] args){
		TemplateTest ts = new TemplateTest();
		
		ts.testTemplate1();
		ts.testTemplate2();
		ts.testTemplate3();
		ts.testTemplate4();
		
		//ts.tm.model.setNsPrefixes(TemplateUtils.getCommonPrefixes());
		//ts.tm.model.write(System.out, "TTL");
	}
	
	/**
	 * Test template with rdsf:Resource argument.
	 */
	@Test
	public void testTemplate1(){
		tm = new TemplateManager();
		
		String q = "PREFIX : <http://test#>"
				+ "REGISTER STREAM :t AS "
				+ "SELECT RSTREAM * "
				+ "FROM NAMED WINDOW :w ON :s [RANGE PT1M] "
				+ "WHERE { "
				+ "   WINDOW :w { "
				+ "      ?a ?b ?c "
				+ "   } "
				+ "}";
		Query query = ARQFactory.get().createQuery(q);
		query.setPrefixMapping(null);
		String queryIn = query.toString();
		String handle = "test1";
		Template template = tm.createTemplate(queryIn, handle, "This template tests the use of an RDFS.Resource argument");
		tm.createArgument(template, "a", RDFS.Resource, null, false, "This should be a resource.");
		
		// Get template
		Template t = tm.getTemplate(handle);
		
		// Create solution mapping against the template
		HashMap<String, String> map = new HashMap<>();
		map.put("a", NS + "Some_Value");
		List<String> bindingErrors = new ArrayList<String>();
		QuerySolutionMap solution = ArgumentHandler.createBindings(template, map, bindingErrors);
		
		// Check that bindings are valid for this template
		List<String> validationErrors = new ArrayList<String>();
		ArgumentHandler.check(template, solution, validationErrors);
		
		// Get resulting query
		String queryOut = tm.getQuery(t, solution);
		
		// String replace ?a as test
		queryIn = queryIn.replaceAll("\\?a", String.format("<%sSome_Value>", NS));
		
		// Test
		assertEquals("Binding error" , bindingErrors, new ArrayList<String>());
		assertEquals("Validation error" , validationErrors, new ArrayList<String>());
		assertEquals("The input and output does not match" , queryIn, queryOut);
		
		// Print query
		System.out.println(queryOut);
	}
	
	/**
	 * Test template with xsd:dateTime argument.
	 */
	@Test
	public void testTemplate2(){
		String q = "PREFIX : <http://test#>"
				+ "REGISTER STREAM :t AS "
				+ "CONSTRUCT DSTREAM { ?a ?b ?c } "
				+ "FROM NAMED WINDOW :w ON :s [RANGE PT1M] "
				+ "WHERE { "
				+ "   WINDOW :w { "
				+ "      ?a ?b ?c "
				+ "   } "
				+ "}";
		Query query = ARQFactory.get().createQuery(q);
		query.setPrefixMapping(null);
		String queryIn = query.toString();
		String handle = "test2";
		Template template = tm.createTemplate(queryIn, handle, "This template tests the use of XSD.dateTime as argument");
		tm.createArgument(template, "c", XSD.dateTime, null, false, "This should be an XSD.dateTime");
		
		// Get template
		Template t = tm.getTemplate(handle);
		
		// Create solution mapping against the template
		HashMap<String, String> map = new HashMap<>();
		map.put("c", "2016-06-20T11:48:00");
		List<String> bindingErrors = new ArrayList<String>();
		QuerySolutionMap solution = ArgumentHandler.createBindings(template, map, bindingErrors);
		
		// Check that bindings are valid for this template
		List<String> validationErrors = new ArrayList<String>();
		ArgumentHandler.check(template, solution, validationErrors);
		
		// Get resulting query
		String queryOut = tm.getQuery(t, solution);
		
		// String replace ?c as test
		queryIn = queryIn.replaceAll("\\?c", String.format("\"2016-06-20T11:48:00\"^^<http://www.w3.org/2001/XMLSchema#dateTime>", NS));
		
		// Test
		assertEquals("Binding error" , bindingErrors, new ArrayList<String>());
		assertEquals("Validation error" , validationErrors, new ArrayList<String>());
		assertEquals("The input and output does not match" , queryIn, queryOut);
		
		// Print query
		System.out.println(queryOut);
	}
	
	/**
	 * Test template with required argument.
	 */
	@Test
	public void testTemplate3(){
		String q = "PREFIX : <http://test#>"
				+ "REGISTER STREAM :t AS "
				+ "CONSTRUCT ISTREAM { ?a ?b ?c } "
				+ "FROM NAMED WINDOW :w ON :s [RANGE PT1M] "
				+ "WHERE { "
				+ "   WINDOW :w { "
				+ "      ?a ?b ?c "
				+ "   } "
				+ "}";
		Query query = ARQFactory.get().createQuery(q);
		query.setPrefixMapping(null);
		String queryIn = query.toString();
		String handle = "test3";
		Template template = tm.createTemplate(queryIn, handle, "This template tests the use of required argument");
		tm.createArgument(template, "c", XSD.dateTime, null, false, "This should be an XSD.dateTime");
		
		// Get template
		Template t = tm.getTemplate(handle);
		
		// Create solution mapping against the template
		HashMap<String, String> map = new HashMap<>();
		List<String> bindingErrors = new ArrayList<String>();
		QuerySolutionMap solution = ArgumentHandler.createBindings(template, map, bindingErrors);
		
		// Check that bindings are valid for this template
		List<String> validationErrors = new ArrayList<String>();
		ArgumentHandler.check(template, solution, validationErrors);
		
		// Get resulting query
		String queryOut = tm.getQuery(t, solution);
		
		// Test
		assertEquals("Binding error" , bindingErrors.size(), 0); // not a binding error
		assertNotEquals("Validation error" , validationErrors.size(), 0); // but should be a validation error
		assertEquals("The input and output does not match" , queryIn, queryOut);
		
		// Print query
		System.out.println(queryOut);
	}
	
	/**
	 * Test template with default value for argument.
	 */
	@Test
	public void testTemplate4(){
		String q = "PREFIX : <http://test#>"
				+ "REGISTER STREAM :t AS "
				+ "CONSTRUCT ISTREAM { ?a ?b ?c } "
				+ "FROM NAMED WINDOW :w ON :s [RANGE PT1M] "
				+ "WHERE { "
				+ "   WINDOW :w { "
				+ "      ?a ?b ?c "
				+ "   } "
				+ "}";
		Query query = ARQFactory.get().createQuery(q);
		query.setPrefixMapping(null);
		String queryIn = query.toString();
		String handle = "test4";
		Template template = tm.createTemplate(queryIn, handle, "This template tests the use of default value argument");
		List<String> rdfNodeErrors = new ArrayList<String>();
		RDFNode value = ArgumentHandler.createRDFNode("2016-06-20T11:48:00", XSD.dateTime, rdfNodeErrors);
		tm.createArgument(template, "c", XSD.dateTime, value, false, "This should be an optional xsd:dateTime");
		
		// Get template
		Template t = tm.getTemplate(handle);
		
		// Create solution mapping against the template
		HashMap<String, String> map = new HashMap<>();
		List<String> bindingErrors = new ArrayList<String>();
		QuerySolutionMap solution = ArgumentHandler.createBindings(template, map, bindingErrors);
		
		// Check that bindings are valid for this template
		List<String> validationErrors = new ArrayList<String>();
		ArgumentHandler.check(template, solution, validationErrors);
		
		// Get resulting query
		String queryOut = tm.getQuery(t, solution);
		
		// String replace ?c as test
		queryIn = queryIn.replaceAll("\\?c", String.format("\"2016-06-20T11:48:00\"^^<http://www.w3.org/2001/XMLSchema#dateTime>", NS));

		// Test
		assertEquals("RDFNode error" , rdfNodeErrors.size(), 0);
		assertEquals("Binding error" , bindingErrors.size(), 0);
		assertEquals("Validation error" , validationErrors.size(), 0);
		assertEquals("The input and output does not match" , queryIn, queryOut);
		
		// Print query
		System.out.println(queryOut);
	}
}
