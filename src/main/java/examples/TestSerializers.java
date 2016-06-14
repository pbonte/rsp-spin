package examples;

import org.rspql.lang.rspql.ParserRSPQL;
import org.rspql.lang.cqels.ParserCQELS;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;

public class TestSerializers {

	public static void main(String[] args) {
		new TestSerializers().run();
	}

	public void run() {
		// Register the parser
		ParserRSPQL.register();

		String q = ""
				+ "PREFIX : <http://test#> \n"
				+ "REGISTER STREAM :s1 AS \n"
				+ "CONSTRUCT RSTREAM { ?g :observedAt ?time . GRAPH ?g { ?a :foo ?c } } \n"
				+ "FROM NAMED WINDOW :w1 ON :s1 [RANGE PT72h STEP PT30s] \n"
				+ "FROM NAMED WINDOW :w2 ON :s1 [ITEM 10 STEP 1] \n"
				+ "FROM NAMED :g \n"
				+ "FROM :default \n"
				+ "WHERE { \n"
				+ "   WINDOW :w1 { \n"
				+ "      ?a :foo ?c . \n"
				+ "      FILTER regex(str(?a), 'abc', 'i')\n"
				+ "   }"
				+ "   WINDOW :w2 { \n"
				+ "      GRAPH ?g { ?a :foo ?c .} \n"
				+ "      "
				+ "      FILTER (?b != :foo)\n"
				+ "   }"
				+ "}\n"
				+ "LIMIT 10";
		Query query = QueryFactory.create(q, ParserRSPQL.rspqlSyntax);
		
		//System.out.println("RSP-QL:");
		//System.out.println(query);
		
		query.setSyntax(ParserCQELS.cqelsSyntax);
		System.out.println("\nCQELS-QL: " + query.getSyntax());
		System.out.println(query);
		
	}
}
