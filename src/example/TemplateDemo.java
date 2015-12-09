package example;

import org.topbraid.spin.arq.ARQ2SPIN;
import org.topbraid.spin.system.SPINModuleRegistry;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class TemplateDemo {
	public static void main(String[] args) {
		// Initialize system functions and templates
		SPINModuleRegistry.get().init();
		
		String qString = "PREFIX : <http://test#> "
				+ "CONSTRUCT {?a ?b ?c } "
				+ "FROM NAMED :static1 "
				+ "FROM NAMED WINDOW :w1 ON :stream1 [RANGE PT60S STEP PT20S] "
				+ "WHERE { "
				+ "GRAPH ?graph1 { "
				+ "   GRAPH ?g1 {?a ?b ?c} "
				+ "} "
				+ "WINDOW ?w1 { "
				+ "   GRAPH ?g1 {?a ?b ?c} "
				+ "   GRAPH ?g2 {?a ?b ?c} "
				+ "} "
				+ "FILTER regex(?a, ?b, 'i') "
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
	}
}
