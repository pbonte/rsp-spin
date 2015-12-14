package example;

import org.rspql.lang.ParserRSPQL;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;

public class TestRSPQLParser {

	public static void main(String[] args) {
		new TestRSPQLParser().run();
	}

	public void run() {
		// Register the parser
		ParserRSPQL.register();

		String q = ""
				+ "PREFIX : <http://test#> \n"
				+ "REGISTER STREAM :s1 AS \n"
				+ "CONSTRUCT ISTREAM { ?a ?b ?c } \n"
				+ "FROM NAMED WINDOW :w ON :s [RANGE PT1m] \n"
				+ "WHERE { \n"
				+ "   WINDOW :w { \n"
				+ "      ?a ?b ?c . \n"
				+ "      WINDOW :w { ?a ?b ?c } \n"
				+ "      GRAPH :g { ?a ?b ?c . }\n"
				+ "   }\n"
				+ "}";
		Query query = QueryFactory.create(q, ParserRSPQL.rspqlSPARQLSyntax);
		System.out.println(query);
		
	}
}
