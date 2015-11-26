package a.example;


import org.topbraid.spin.arq.ARQ2SPIN;
import org.topbraid.spin.arq.ARQFactory;
import org.topbraid.spin.model.SPINFactory;
import org.topbraid.spin.system.SPINModuleRegistry;

import com.hp.hpl.jena.query.Query;
//import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class DevARQ {
	public static void main(String[] args) {
		// Initialize system functions and templates
		SPINModuleRegistry.get().init();
		
		String qString = "PREFIX : <http://test#> "
				+ "CONSTRUCT {?a ?b ?c } "
				+ "FROM NAMED <http://static1> "
				+ "FROM NAMED WINDOW :w1 ON :stream1 [RANGE ?theRange STEP ?theStep] "
				+ "WHERE { "
				+ "WINDOW :w1 { "
				+ "   ?a ?b ?c . "
				+ "} "
				+ "}";

		
		
		Model model = ModelFactory.createDefaultModel();
		model.setNsPrefix("sp", "http://spinrdf.org/sp#");
		model.setNsPrefix("xsd", "http://www.w3.org/2001/XMLSchema#");
		
		// The model contains the queries
		System.out.println("Parsing query as ARQ query");
		Query query = QueryFactory.create(qString, Syntax.syntaxARQ);
		System.out.println("\nParsed as ARQ:");
		System.out.println(query.toString());
		System.out.println();
		
		
		System.out.println("\nConverting ARQ query to SPIN");
		ARQ2SPIN arq2SPIN = new ARQ2SPIN(model);
		arq2SPIN.createQuery(query, "http://query1");
		model.write(System.out,"TTL");
		//System.out.println();
		
		// Get the query from the model
		System.out.println("Get query from model");
		org.topbraid.spin.model.Query q = SPINFactory.asQuery(model.getResource("http://query1"));
		System.out.println(q.toString());
		System.out.println();
		
		System.out.println("Try parsing it back into ARQ");
		Query secondParse = QueryFactory.create(q.toString(), Syntax.syntaxARQ);
		secondParse.setPrefix("", "http://test#");
		System.out.println(secondParse);
		
		
		// Next 1: add support for in line references to window. This should be almost identical to named graphs in the where clause.
		// Works: Next 2: check if it works also for construct and ask.
		// Next 3: consider adding the beginning part of query from the RSP page "<... PREFIXES ...> REGISTER STREAM :query AS <... query ...>"
		// Next 4: consider adding "CONSTRUCT ISTREAM" and "CONSTRUCT DSTREAM"
		// Next: check if subqueries can be supported. Not supported with "FROM" so this should be fairly simple
	}
}
