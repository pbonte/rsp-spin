package example;

import org.rspql.lang.ParserRSPQL;
import org.topbraid.spin.arq.ARQ2SPIN;
import org.topbraid.spin.system.SPINModuleRegistry;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class TestRSPQLSPIN {
	public static void main(String[] args) {
		// Initialize SPIN system functions and templates
		SPINModuleRegistry.get().init();

		// Register the RSP-QL parser
		ParserRSPQL.register();
		
		String q = "PREFIX : <http://example.org#> "
				+ "REGISTER STREAM :w AS "
				+ "CONSTRUCT ISTREAM { ?a ?b ?c } "
				+ "FROM NAMED WINDOW :w ON :s [RANGE PT1H STEP PT1M] "
				+ "WHERE { "
				+ "   GRAPH :g { ?a ?b ?c } "
				+ "   WINDOW :w { GRAPH { ?a ?b ?c }  } "
				+ "}";
		
		
		Query query = QueryFactory.create(q, ParserRSPQL.rspqlSPARQLSyntax);
		System.out.println(query);
		
		Model model = ModelFactory.createDefaultModel();
		ARQ2SPIN arq2SPIN = new ARQ2SPIN(model);
		org.topbraid.spin.model.Query spinQuery = arq2SPIN.createQuery(query, null);
		
		System.out.println(spinQuery);
	}
}
