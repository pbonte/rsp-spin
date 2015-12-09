package a.example;

import org.topbraid.spin.arq.ARQ2SPIN;
import org.topbraid.spin.model.SPINFactory;
import org.topbraid.spin.system.SPINModuleRegistry;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class DevARQ {
	public static void main(String[] args) {
		// Initialize system functions and templates
		SPINModuleRegistry.get().init();

		String qString = "" + "PREFIX : <http://test#> " + "REGISTER STREAM ?generatedStream AS "
				+ "CONSTRUCT RSTREAM { ?a ?b ?c } " + "FROM NAMED <http://test> FROM NAMED WINDOW :w ON :s [RANGE PT10s STEP PT1s] " + "WHERE { "
				+ "   {SELECT * WHERE { ?a ?b ?c }}" + "   WINDOW :w { " + "      GRAPH ?g { ?a ?b ?c }"
				+ "      ?a ?b ?c . " + "   } " + "}";

		Model model = ModelFactory.createDefaultModel();
		model.setNsPrefix("rsp", "https://w3id.org/rsp/spin#");
		model.setNsPrefix("sp", "http://spinrdf.org/sp#");
		model.setNsPrefix("xsd", "http://www.w3.org/2001/XMLSchema#");
		model.setNsPrefix("rdfs", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
		model.setNsPrefix("", "http://example.org/");

		// The model contains the queries
		System.out.println("Parsing query as ARQ query");
		Query query = QueryFactory.create(qString, Syntax.syntaxARQ);
		System.out.println("\nParsed as ARQ:");
		System.out.println(query);
		System.out.println();

		System.out.println("\nConverting ARQ query to SPIN");
		ARQ2SPIN arq2SPIN = new ARQ2SPIN(model);
		arq2SPIN.createQuery(query, "http://query1");
		model.write(System.out, "TTL");
		System.err.println();

		// Get the query from the model
		System.out.println("Get query from model");
		model.removeNsPrefix("");
		org.topbraid.spin.model.Query q = SPINFactory.asQuery(model.getResource("http://query1"));
		System.out.println(q.toString());
		System.out.println();

		System.out.println("Try parsing it back into ARQ");
		Query secondParse = QueryFactory.create(q.toString(), Syntax.syntaxARQ);
		secondParse.setPrefix("", "http://test#");
		System.out.println(secondParse);

	}
}
