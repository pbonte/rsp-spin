package example;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import org.rsp.lang.ParserRSPQL;

public class TestRSPQLParser {

	public static void main(String[] args) {
		new TestRSPQLParser().run();
	}

	public void run() {
		// Register the parser
		ParserRSPQL.register();

		String q = ""
				+ "PREFIX : <http://test#> "
				+ "REGISTER STREAM :s1 AS "
				+ "CONSTRUCT ISTREAM { ?a ?b ?c } "
				+ "FROM NAMED WINDOW :w ON :s [RANGE PT1m] "
				+ "WHERE { "
				+ "   GRAPH :g { ?a ?b ?c } "
				+ "   WINDOW :w { "
				+ "      GRAPH { ?a ?b ?c } "
				+ "   }"
				+ "}";
		Query query = QueryFactory.create(q, ParserRSPQL.rspqlSPARQLSyntax);
		//System.out.println(query);
		
	}
}
