package a.example;


import org.topbraid.spin.arq.ARQ2SPIN;
import org.topbraid.spin.arq.ARQFactory;
import org.topbraid.spin.model.SPINFactory;

import com.hp.hpl.jena.query.Query;
//import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class DevARQ {
	public static void main(String[] args) {
		String qString = "PREFIX : <http://test#> "
				+ "SELECT * "
				+ "FROM NAMED :static1 "
				+ "FROM NAMED :static2 "
				+ "FROM NAMED WINDOW :w1 ON :stream1 [RANGE PT60S STEP PT20S] "
				+ "FROM NAMED WINDOW :w2 ON :stream2 [RANGE PT60S STEP PT20S] "
				+ "WHERE { ?a ?b ?c. }";
		
		Model model = ModelFactory.createDefaultModel();
		model.setNsPrefix("sp", "http://spinrdf.org/sp#");
		model.setNsPrefix("xsd", "http://www.w3.org/2001/XMLSchema#");
		
		// The model contains the queries
		System.out.println("Parsing query as ARQ query");
		Query query = QueryFactory.create(qString, Syntax.syntaxARQ);
		System.out.println(query);
		System.out.println();
		
		System.out.println("Converting ARQ query to SPIN");
		ARQ2SPIN arq2SPIN = new ARQ2SPIN(model);
		arq2SPIN.createQuery(query, "http://query1");
		model.write(System.out,"TTL");
		System.out.println();
		
		// Get the query from the model
		System.out.println("Get query from model");
		org.topbraid.spin.model.Query q = SPINFactory.asQuery(model.getResource("http://query1"));
		System.out.println(q.toString());
		
		
		//Query q = QueryFactory.create(qString, Syntax.syntaxARQ);
		//System.out.println(q);
		
	}
}
