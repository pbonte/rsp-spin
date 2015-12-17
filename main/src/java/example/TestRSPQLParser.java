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
				+ "CONSTRUCT ISTREAM { ?a :foo ?c } \n"
				+ "FROM NAMED WINDOW :w1 ON :s1 [RANGE PT1m10s STEP PT30s] \n"
				+ "FROM NAMED WINDOW :w2 ON :s2 [RANGE PT1m10s] \n"
				+ "FROM NAMED WINDOW :w3 ON ?s [RANGE ?range STEP ?step] \n"
				+ "FROM NAMED WINDOW :w4 ON :s1 [ITEM 10 STEP 5] \n"
				+ "FROM NAMED WINDOW :w5 ON :s2 [ITEM 10] \n"
				+ "FROM NAMED WINDOW :w6 ON ?s [ITEM ?r STEP ?s] \n"
				+ "FROM NAMED :g \n"
				+ "FROM :default \n"
				+ "WHERE { \n"
				+ "   {WINDOW :w1 { \n"
				+ "      ?a :foo ?c . \n"
				+ "      GRAPH ?g { ?a :foo :Bar } "
				+ "      FILTER regex(str(?a), 'abc', 'i')\n"
				+ "   }}"
				+ "   UNION \n"
				+ "   {WINDOW :w2 { \n"
				+ "      ?a :foo ?c . \n"
				+ "      GRAPH :g { ?a ?b ?c } \n"
				+ "      FILTER (?b != :foo)\n"
				+ "   }}"
				+ "   UNION \n"
				+ "   {GRAPH :g { ?a :foo :Bar }}\n"
				+ "}\n"
				+ "LIMIT 10";
		Query query = QueryFactory.create(q, ParserRSPQL.rspqlSPARQLSyntax);
		System.out.println(query);
		
	}
}
