package org.rspql.tests.csrbench;

import static org.junit.Assert.assertEquals;
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

public class TestRSPQLCsrbenchParsing {

	public static void main(String[] args) {
		TestRSPQLCsrbenchParsing c = new TestRSPQLCsrbenchParsing();
		c.csrbenchQuery1();
		c.csrbenchQuery2();
		c.csrbenchQuery3();
		c.csrbenchQuery4();
		c.csrbenchQuery5();
		c.csrbenchQuery6();
		c.csrbenchQuery7();
	}

	@Test
	public void csrbenchQuery1() {
		test("./queries/csrbench/rspql/query1.rspql", "CSRBench query1");
	}

	@Test
	public void csrbenchQuery2() {
		test("./queries/csrbench/rspql/query2.rspql", "CSRBench query2");
	}

	@Test
	public void csrbenchQuery3() {
		test("./queries/csrbench/rspql/query3.rspql", "CSRBench query3");
	}

	@Test
	public void csrbenchQuery4() {
		test("./queries/csrbench/rspql/query4.rspql", "CSRBench query4");
	}

	@Test
	public void csrbenchQuery5() {
		test("./queries/csrbench/rspql/query5.rspql", "CSRBench query5");
	}

	@Test
	public void csrbenchQuery6() {
		test("./queries/csrbench/rspql/query6.rspql", "CSRBench query6");
	}

	@Test
	public void csrbenchQuery7() {
		test("./queries/csrbench/rspql/query7.rspql", "CSRBench query7");
	}

	public void test(String path, String message) {
		SPINModuleRegistry.get().init();
		ParserRSPQL.register();
		try {
			String original = new String(Files.readAllBytes(Paths.get(path)));
			String reparsed = reparseFromTemplate(original);
			
			// Original without prefixes
			Query query = QueryFactory.create(original, ParserRSPQL.rspqlSyntax);
			query.setPrefixMapping(null);
			String originalNoPrefix = query.toString();
			
			assertEquals(message, originalNoPrefix, reparsed);
		} catch (IOException e) {
			System.err.println("ERROR: " + e.getMessage());
		}
	}

	public String reparseFromTemplate(String queryString) throws IOException {
		Query query = QueryFactory.create(queryString, ParserRSPQL.rspqlSyntax);
		query.setPrefixMapping(null);
		
		// Setup RSP-SPIN and model
		Model model = ModelFactory.createDefaultModel();
		ARQ2SPIN arq2SPIN = new ARQ2SPIN(model);
		
		// Create SPIN template
		String handle = "http://test";
		org.topbraid.spin.model.Query spinQuery = arq2SPIN.createQuery(query, null);
		Template template = model.createResource(handle, SPIN.Template).as(Template.class);
		template.addProperty(SPIN.body, spinQuery);

		// Read the template back from the model
		ARQFactory.get().setSyntax(ParserRSPQL.rspqlSyntax);
		Template t = model.createResource(handle, SPIN.Template).as(Template.class);
		Query reparsedQuery = ARQFactory.get().createQuery((org.topbraid.spin.model.Query) t.getBody());
		
		// RSP-QL
		reparsedQuery.setSyntax(ParserRSPQL.rspqlSyntax);
		return reparsedQuery.toString();
	}
}
