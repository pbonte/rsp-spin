package org.rspql.lang.csparql;

public class ParserCSPARQL {
	final static public Syntax csparqlSyntax = new Syntax("csparql", "http://eu.larkc.csparql");

	static class Syntax extends com.hp.hpl.jena.query.Syntax {
		protected Syntax(String lookupName, String uri) {
			super(uri);
			querySyntaxNames.put(lookupName, this);
		}
	}
}