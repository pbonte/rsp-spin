package a.example;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.Syntax;

public class DevARQ {
	public static void main(String[] args) {
		String qString = "PREFIX : <http://test> "
				+ "SELECT * FROM NAMED :static "
				+ "FROM NAMED WINDOW :w1 ON :stream1 [RANGE PT60S STEP PT20S]"
				+ "WHERE { ?a ?b ?c. }";
		Query q = QueryFactory.create(qString, Syntax.syntaxARQ);
		System.out.println(q);
	}
}
