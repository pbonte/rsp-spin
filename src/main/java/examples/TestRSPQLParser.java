package examples;

import org.rspql.lang.rspql.ParserRSPQL;

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
				+ "PREFIX : <http://example.org#>\n"
				+ "CONSTRUCT { ?foo :isa ?bar }\n"
				+ "FROM NAMED WINDOW :w ON ?stream [FROM NOW-?old TO NOW-?new STEP ?step]\n"
				+ "WHERE {\n"
				+ "   WINDOW :w { GRAPH ?g { ?foo :isa ?bar } }\n" 
				+ "}";
		Query query = QueryFactory.create(q, ParserRSPQL.rspqlSyntax);
		System.out.println(query);
		
	}
}
