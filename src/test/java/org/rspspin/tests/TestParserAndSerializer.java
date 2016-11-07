package org.rspspin.tests;

import static org.junit.Assert.*;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.junit.Test;
import org.rspspin.lang.rspql.ParserRSPQL;

public class TestParserAndSerializer {

	/* Output stream name as a variable */
	//@Test
	public void registerAs1() {
		ParserRSPQL.register();
		String query = "" + "REGISTER STREAM ?out AS SELECT * WHERE { ?a ?b ?c }";
		Query q = QueryFactory.create(query, ParserRSPQL.syntax);
		assertEquals(compress(q.toString()), compress(query));
	}

	/* Output stream name as a URI */
	//@Test
	public void registerAs2() {
		ParserRSPQL.register();
		String query = "" + "REGISTER STREAM <http://example/stream> AS SELECT * WHERE { ?a  ?b  ?c }";
		Query q = QueryFactory.create(query, ParserRSPQL.syntax);
		assertEquals(compress(q.toString()), compress(query));
	}

	/* Window stream name as a variable */
	//@Test
	public void physicalWindow1() {
		ParserRSPQL.register();
		String query = ""
				+ "REGISTER STREAM ?out AS "
				+ "SELECT * "
				+ "FROM NAMED WINDOW <http://window> ON ?stream [ITEM 10 STEP 10] "
				+ "WHERE { WINDOW <http://window> { ?a ?b ?c } }";
		Query q = QueryFactory.create(query, ParserRSPQL.syntax);
		
		assertEquals(compress(q.toString()), compress(query));
	}
	
	/* Input stream as a URL */
	//@Test
	public void physicalWindow2() {
		ParserRSPQL.register();
		String query = "" 
				+ "REGISTER STREAM ?out AS "
				+ "SELECT * "
				+ "FROM NAMED WINDOW <http://window> ON <http://stream> [ITEM 10 STEP 10] "
				+ "WHERE { WINDOW <http://window> { ?a ?b ?c } }";
		Query q = QueryFactory.create(query, ParserRSPQL.syntax);
		
		assertEquals(compress(q.toString()), compress(query));
	}
	
	/* Window range and step as variables */
	//@Test
	public void physicalWindow3() {
		ParserRSPQL.register();
		String query = "" 
				+ "REGISTER STREAM ?out AS "
				+ "SELECT * "
				+ "FROM NAMED WINDOW <http://window> ON <http://stream> [ITEM ?range STEP ?step] "
				+ "WHERE { WINDOW <http://window> { ?a ?b ?c } }";
		Query q = QueryFactory.create(query, ParserRSPQL.syntax);
		assertEquals(compress(q.toString()), compress(query));
	}
	
	/* Only window range */
	//@Test
	public void physicalWindow4() {
		ParserRSPQL.register();
		String query = "" 
				+ "REGISTER STREAM ?out AS "
				+ "SELECT * "
				+ "FROM NAMED WINDOW <http://window> ON <http://stream> [ITEM 10] "
				+ "WHERE { WINDOW <http://window> { ?a ?b ?c } }";
		Query q = QueryFactory.create(query, ParserRSPQL.syntax);
		assertEquals(compress(q.toString()), compress(query));
	}

	/* Window stream name as a variable */
	//@Test
	public void logicalWindow1() {
		ParserRSPQL.register();
		String query = ""
				+ "REGISTER STREAM ?out AS "
				+ "SELECT * "
				+ "FROM NAMED WINDOW <http://window> ON ?stream [RANGE PT1H STEP PT1H] "
				+ "WHERE { WINDOW <http://window> { ?a ?b ?c } }";
		Query q = QueryFactory.create(query, ParserRSPQL.syntax);
		assertEquals(compress(q.toString()), compress(query));
	}
	
	/* Input stream as a URL */
	//@Test
	public void logicalWindow2() {
		ParserRSPQL.register();
		String query = "" 
				+ "REGISTER STREAM ?out AS "
				+ "SELECT * "
				+ "FROM NAMED WINDOW <http://window> ON <http://stream> [RANGE PT1H STEP PT1H] "
				+ "WHERE { WINDOW <http://window> { ?a ?b ?c } }";
		Query q = QueryFactory.create(query, ParserRSPQL.syntax);
		assertEquals(compress(q.toString()), compress(query));
	}
	
	/* Window range and step as variables */
	//@Test
	public void logicalWindow3() {
		ParserRSPQL.register();
		String query = "" 
				+ "REGISTER STREAM ?out AS "
				+ "SELECT * "
				+ "FROM NAMED WINDOW <http://window> ON <http://stream> [RANGE ?range STEP ?step] "
				+ "WHERE { WINDOW <http://window> { ?a ?b ?c } }";
		Query q = QueryFactory.create(query, ParserRSPQL.syntax);
		
		assertEquals(compress(q.toString()), compress(query));
	}
	
	/* Only window range */
	//@Test
	public void logicalWindow4() {
		ParserRSPQL.register();
		String query = "" 
				+ "REGISTER STREAM ?out AS "
				+ "SELECT * "
				+ "FROM NAMED WINDOW <http://window> ON <http://stream> [RANGE PT1H] "
				+ "WHERE { WINDOW <http://window> { ?a ?b ?c } }";
		Query q = QueryFactory.create(query, ParserRSPQL.syntax);
		assertEquals(compress(q.toString()), compress(query));
	}
	
	/* Window stream name as a variable */
	//@Test
	public void logicalPastWindow1() {
		ParserRSPQL.register();
		String query = ""
				+ "REGISTER STREAM ?out AS "
				+ "SELECT * "
				+ "FROM NAMED WINDOW <http://window> ON ?stream [FROM NOW-PT2H TO NOW-PT1H STEP PT1H] "
				+ "WHERE { WINDOW <http://window> { ?a ?b ?c } }";
		Query q = QueryFactory.create(query, ParserRSPQL.syntax);
		assertEquals(compress(q.toString()), compress(query));
	}
	
	/* Input stream as a URL */
	//@Test
	public void logicalPastWindow2() {
		ParserRSPQL.register();
		String query = "" 
				+ "REGISTER STREAM ?out AS "
				+ "SELECT * "
				+ "FROM NAMED WINDOW <http://window> ON <http://stream> [FROM NOW-PT2H TO NOW-PT1H STEP PT1H] "
				+ "WHERE { WINDOW <http://window> { ?a ?b ?c } }";
		Query q = QueryFactory.create(query, ParserRSPQL.syntax);
		assertEquals(compress(q.toString()), compress(query));
	}
	
	/* Window from, to and step as variables */
	//@Test
	public void logicalPastWindow3() {
		ParserRSPQL.register();
		String query = "" 
				+ "REGISTER STREAM ?out AS "
				+ "SELECT * "
				+ "FROM NAMED WINDOW <http://window> ON <http://stream> [FROM NOW-?from TO NOW-?to STEP ?step] "
				+ "WHERE { WINDOW <http://window> { ?a ?b ?c } }";
		Query q = QueryFactory.create(query, ParserRSPQL.syntax);
		assertEquals(compress(q.toString()), compress(query));
	}
	
	/* Window no step */
	//@Test
	public void logicalPastWindow4() {
		ParserRSPQL.register();
		String query = "" 
				+ "REGISTER STREAM ?out AS "
				+ "SELECT * "
				+ "FROM NAMED WINDOW <http://window> ON <http://stream> [FROM NOW-PT2H TO NOW-PT1H] "
				+ "WHERE { WINDOW <http://window> { ?a ?b ?c } }";
		Query q = QueryFactory.create(query, ParserRSPQL.syntax);
		assertEquals(compress(q.toString()), compress(query));
	}
	
	
	/* Test illegal duration format */
	//@Test
	public void illegalDuration() {
		ParserRSPQL.register();
		Query q = null;
		String query = "" 
				+ "REGISTER STREAM ?out AS "
				+ "SELECT * "
				+ "FROM NAMED WINDOW <http://window> ON <http://stream> [RANGE PT1M1H] "
				+ "WHERE { WINDOW <http://window> { ?a ?b ?c } }";
		try {
			q = QueryFactory.create(query, ParserRSPQL.syntax);
		} catch (Exception e) {}
		assertEquals(q, null);
	}

	public String compress(String s) {
		return s.replaceAll("\\s+", " ").trim();
	}
}
