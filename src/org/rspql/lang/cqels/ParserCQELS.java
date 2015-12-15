package org.rspql.lang.cqels;

public class ParserCQELS {
	final static public Syntax cqelsSyntax = new Syntax("cqels", "http://deri.org/2011/05/query/CQELS_01");

	static class Syntax extends com.hp.hpl.jena.query.Syntax {
		protected Syntax(String lookupName, String uri) {
			super(uri);
			querySyntaxNames.put(lookupName, this);
		}
	}
}