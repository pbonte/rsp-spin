package org.rspql.tests.csrbench;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;
import org.rspql.lang.cqels.ParserCQELS;
import org.rspql.lang.csparql.ParserCSPARQL;
import org.rspql.lang.rspql.ParserRSPQL;
import org.rspql.lang.sparqlstream.ParserSPARQLStream;
import org.topbraid.spin.arq.ARQ2SPIN;
import org.topbraid.spin.arq.ARQFactory;
import org.topbraid.spin.model.Template;
import org.topbraid.spin.system.SPINModuleRegistry;
import org.topbraid.spin.vocabulary.SPIN;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class CsrbenchParseTest {
	private Model model = ModelFactory.createDefaultModel();
	
	@Test
	public void csrbenchQuery1() {
		test("./queries/csrbench/query1.rspql");
	}
	
	@Test
	public void csrbenchQuery2() {
		test("./queries/csrbench/query2.rspql");
	}
	
	@Test
	public void csrbenchQuery3() {
		test("./queries/csrbench/query3.rspql");
	}
	
	@Test
	public void csrbenchQuery4() {
		test("./queries/csrbench/query4.rspql");
	}
	
	@Test
	public void csrbenchQuery5() {
		test("./queries/csrbench/query5.rspql");
	}
	
	@Test
	public void csrbenchQuery6() {
		test("./queries/csrbench/query6.rspql");
	}

	@Test
	public void csrbenchQuery7() {
		test("./queries/csrbench/query7.rspql");
	}
	
	public void test(String path) {
		boolean valid = false;
		try {
			parse(path, false);
			valid = true;
		} catch (IOException e) {
			System.err.println("ERROR: " + e.getMessage());
		}
		assertTrue(valid);
	}

	public void parse(String path, boolean print) throws IOException {
		String queryString = new String(Files.readAllBytes(Paths.get(path)));
		System.out.println("\n" + path);

		// Register the parser
		ParserRSPQL.register();
		Query query = QueryFactory.create(queryString, ParserRSPQL.rspqlSyntax);
		// Setup RSP-SPIN and model
		SPINModuleRegistry.get().init();
		ARQ2SPIN arq2SPIN = new ARQ2SPIN(model);

		// Convert query to SPIN
		String[] p = path.split("/");
		String handle = "http://" + p[p.length - 1];
		org.topbraid.spin.model.Query spinQuery = arq2SPIN.createQuery(query, null);
		Template template = model.createResource(handle, SPIN.Template).as(Template.class);
		template.addProperty(SPIN.body, spinQuery);

		// Read the template back from the model
		ARQFactory.get().setSyntax(ParserRSPQL.rspqlSyntax);
		Template t = model.createResource(handle, SPIN.Template).as(Template.class);
		Query reparsedQuery = ARQFactory.get().createQuery((org.topbraid.spin.model.Query) t.getBody());

		// Add back the original query prefixes which are lost in the SPIN
		// representation
		reparsedQuery.setPrefixMapping(query.getPrefixMapping());

		// RSP-QL
		System.out.println("### RSP-QL ###");
		reparsedQuery.setSyntax(ParserRSPQL.rspqlSyntax);
		reparsedQuery.toString();
		// System.out.println(reparsedQuery);

		// CQELS
		System.out.println("### CQELS-QL ###");
		reparsedQuery.setSyntax(ParserCQELS.cqelsSyntax);
		reparsedQuery.toString();
		// System.out.println(reparsedQuery);

		// CSPARQL
		System.out.println("### C-SPARQL ###");
		reparsedQuery.setSyntax(ParserCSPARQL.csparqlSyntax);
		reparsedQuery.toString();
		// System.out.println(reparsedQuery);

		// SAPRQLStream
		System.out.println("### SPARQLStream ###");
		reparsedQuery.setSyntax(ParserSPARQLStream.sparqlStreamSyntax);
		reparsedQuery.toString();
		// System.out.println(reparsedQuery);
	}
}
