package org.rspql.api;

import com.hp.hpl.jena.query.Query;

public class Test {

	public static void main(String[] args) {
		RSPQL api = RSPQL.get();
		Query query = api.parseQuery(""
				+ "PREFIX : <http://example.org#> "
				+ "SELECT * "
				+ "FROM NAMED WINDOW :w ON ?s [RANGE ?s] "
				+ "WHERE { ?a ?b ?c }" );
		System.out.println(query);
		String spin = api.queryToRdf(query, "http://test");
		System.out.println(spin);
		Query query2 = api.queryFromRdf(spin, "http://test");
		System.out.println(query2);
	}

}
