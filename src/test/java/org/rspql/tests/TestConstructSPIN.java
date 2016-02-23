package org.rspql.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.rspql.lang.rspql.ParserRSPQL;
import org.topbraid.spin.arq.ARQ2SPIN;
import org.topbraid.spin.arq.ARQFactory;
import org.topbraid.spin.system.SPINModuleRegistry;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class TestConstructSPIN {
	// Graph output tests
	/**
	 * Graph query (no variable)
	 */
	@Test
	public void constructGraph1() {
		String q = "" + "PREFIX : <http://example.org#>\n" + "CONSTRUCT { ?a ?b ?c . " + "   GRAPH :g { "
				+ "      ?foo :isa ?bar2 " + "   }\n" + "   ?a ?b ?c ." + "}\n"
				+ "FROM NAMED WINDOW :w ON ?stream [RANGE PT1s]\n" + "WHERE {\n"
				+ "   WINDOW :w { ?foo :isa ?bar1 . }\n" + "}";
		validate(q, "constructGraph1");
	}

	/**
	 * Graph query with variable
	 */
	@Test
	public void constructGraph2() {
		String q = "" + "PREFIX : <http://example.org#>\n" + "CONSTRUCT {\n" + "GRAPH ?bar1 {\n" + "   ?foo :isa ?bar2 "
				+ "}\n" + "?a ?b ?c ." + "}\n" + "FROM NAMED WINDOW :w ON ?stream [RANGE PT1s]\n" + "WHERE {\n"
				+ "   WINDOW :w { ?foo :isa ?bar1, ?bar2 . }\n" + "}";
		validate(q, "constructGraph2");
	}

	// Physical windows tests
	/**
	 * Physical window without step
	 */
	@Test
	public void physicalWindow1() {
		String q = "" + "PREFIX : <http://example.org#>\n" + "CONSTRUCT { ?foo :isa ?bar }\n"
				+ "FROM NAMED WINDOW :w ON :s [ITEM 10]\n" + "WHERE {\n"
				+ "   WINDOW :w { GRAPH ?g { ?foo :isa ?bar } }\n" + "}";
		validate(q, "physicalWindow1");
	}

	/**
	 * Physical window with step
	 */
	@Test
	public void physicalWindow2() {
		String q = "" + "PREFIX : <http://example.org#>\n" + "CONSTRUCT { ?foo :isa ?bar }\n"
				+ "FROM NAMED WINDOW :w ON :s [ITEM 10 STEP 1]\n" + "WHERE {\n"
				+ "   WINDOW :w { GRAPH ?g { ?foo :isa ?bar } }\n" + "}";
		validate(q, "physicalWindow2");
	}

	/**
	 * Physical window with variables
	 */
	@Test
	public void physicalWindow3() {
		String q = "" + "PREFIX : <http://example.org#>\n" + "CONSTRUCT { ?foo :isa ?bar }\n"
				+ "FROM NAMED WINDOW :w ON ?stream [ITEM ?item STEP ?step]\n" + "WHERE {\n"
				+ "   WINDOW :w { GRAPH ?g { ?foo :isa ?bar } }\n" + "}";
		validate(q, "physicalWindow3");
	}

	/**
	 * Physical past window without variables
	 */
	@Test
	public void physicalPastWindow1() {
		String q = "" + "PREFIX : <http://example.org#>\n" + "CONSTRUCT { ?foo :isa ?bar }\n"
				+ "FROM NAMED WINDOW :w ON ?stream [FROM NOW-PT1h TO NOW-PT30m]\n" + "WHERE {\n"
				+ "   WINDOW :w { GRAPH ?g { ?foo :isa ?bar } }\n" + "}";
		validate(q, "physicalPastWindow1");
	}

	/**
	 * Physical past window with variables
	 */
	@Test
	public void physicalPastWindow2() {
		String q = "" + "PREFIX : <http://example.org#>\n" + "CONSTRUCT { ?foo :isa ?bar }\n"
				+ "FROM NAMED WINDOW :w ON ?stream [FROM NOW-?old TO NOW-?new ]\n" + "WHERE {\n"
				+ "   WINDOW :w { GRAPH ?g { ?foo :isa ?bar } }\n" + "}";
		validate(q, "physicalPastWindow2");
	}

	/**
	 * Physical past window with step
	 */
	@Test
	public void physicalPastWindow3() {
		String q = "" + "PREFIX : <http://example.org#>\n" + "CONSTRUCT { ?foo :isa ?bar }\n"
				+ "FROM NAMED WINDOW :w ON ?stream [FROM NOW-?old TO NOW-?new STEP ?step]\n" + "WHERE {\n"
				+ "   WINDOW :w { GRAPH ?g { ?foo :isa ?bar } }\n" + "}";
		validate(q, "physicalPastWindow3");
	}

	// Logical window tests
	/**
	 * Logical window without step
	 */
	@Test
	public void logicalWindow1() {
		String q = "" + "PREFIX : <http://example.org#>\n" + "CONSTRUCT { ?foo :isa ?bar }\n"
				+ "FROM NAMED WINDOW :w ON :s [RANGE PT1H]\n" + "WHERE {\n"
				+ "   WINDOW :w { GRAPH ?g { ?foo :isa ?bar } }\n" + "}";
		validate(q, "logicalWindow1");
	}

	/**
	 * Logical window with step
	 */
	@Test
	public void locicalWindow2() {
		String q = "" + "PREFIX : <http://example.org#>\n" + "CONSTRUCT { ?foo :isa ?bar }\n"
				+ "FROM NAMED WINDOW :w ON :s [RANGE PT1H STEP PT1H]\n" + "WHERE {\n"
				+ "   WINDOW :w { GRAPH ?g { ?foo :isa ?bar } }\n" + "}";
		validate(q, "locicalWindow2");
	}

	/**
	 * Logical window with variables
	 */
	@Test
	public void logicalWindow3() {
		String q = "" + "PREFIX : <http://example.org#>\n" + "CONSTRUCT { ?foo :isa ?bar }\n"
				+ "FROM NAMED WINDOW :w ON ?stream [RANGE ?range STEP ?step]\n" + "WHERE {\n"
				+ "   WINDOW :w { GRAPH ?g { ?foo :isa ?bar } }\n" + "}";
		validate(q, "logicalWindow3");
	}

	/**
	 * Register as without variable
	 */
	@Test
	public void registerAs1() {
		String q = "" + "PREFIX : <http://example.org#>\n" + "REGISTER STREAM :s AS\n" + "CONSTRUCT { ?a ?b ?c }\n"
				+ "FROM NAMED WINDOW :w ON :stream [RANGE PT1H]\n" + "WHERE {\n"
				+ "   WINDOW :w { GRAPH ?g { ?a ?b ?c } }" + "}";
		validate(q, "registerAs1");
	}

	/**
	 * Register as with variable
	 */
	@Test
	public void registerAs2() {
		String q = "" + "PREFIX : <http://example.org#>\n" + "REGISTER STREAM ?s AS\n" + "CONSTRUCT { ?a ?b ?c }\n"
				+ "FROM NAMED WINDOW :w ON :stream [RANGE PT1H]\n" + "WHERE {\n"
				+ "   WINDOW :w { GRAPH ?g {?a ?b ?c} }" + "}";
		validate(q, "registerAs2");
	}

	/**
	 * Istream keyword
	 */
	@Test
	public void windowToStreamOp1() {
		String q = "" + "PREFIX : <http://example.org#>\n" + "REGISTER STREAM :s AS\n"
				+ "CONSTRUCT ISTREAM { ?a ?b ?c }\n" + "FROM NAMED WINDOW :w ON :stream [RANGE PT1H]\n" + "WHERE {\n"
				+ "   WINDOW :w { GRAPH ?g { ?a ?b ?c } }" + "}";
		validate(q, "windowToStreamOp1");
	}

	/**
	 * Rstream keyword
	 */
	@Test
	public void windowToStreamOp2() {
		String q = "" + "PREFIX : <http://example.org#>\n" + "REGISTER STREAM :s AS\n"
				+ "CONSTRUCT RSTREAM { ?a ?b ?c }\n" + "FROM NAMED WINDOW :w ON :stream [RANGE PT1H]\n" + "WHERE {\n"
				+ "   WINDOW :w { GRAPH ?g { ?a ?b ?c } }" + "}";
		validate(q, "windowToStreamOp2");
	}

	/**
	 * Dstream keyword
	 */
	@Test
	public void windowToStreamOp3() {
		String q = "" + "PREFIX : <http://example.org#>\n" + "REGISTER STREAM :s AS\n"
				+ "CONSTRUCT DSTREAM { ?a ?b ?c }\n" + "FROM NAMED WINDOW :w ON :stream [RANGE PT1H]\n" + "WHERE {\n"
				+ "   WINDOW :w { GRAPH ?g { ?a ?b ?c } }" + "}";
		validate(q, "windowToStreamOp3");
	}

	private void validate(String q, String message) {
		// Register the parser
		ParserRSPQL.register();
		Query query = QueryFactory.create(q, ParserRSPQL.rspqlSyntax);
		// Parse to SPIN
		SPINModuleRegistry.get().init();
		Model model = ModelFactory.createDefaultModel();
		ARQ2SPIN arq2SPIN = new ARQ2SPIN(model);
		org.topbraid.spin.model.Query spinQuery = arq2SPIN.createQuery(query, null);
		// Parse ARQ from SPIN
		ARQFactory.get().setSyntax(ParserRSPQL.rspqlSyntax);
		Query reparsedQuery = ARQFactory.get().createQuery(spinQuery);
		reparsedQuery.setPrefixMapping(query.getPrefixMapping());

		assertEquals(message, query.toString(), reparsedQuery.toString());
	}

}
