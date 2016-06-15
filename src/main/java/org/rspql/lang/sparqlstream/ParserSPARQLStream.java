package org.rspql.lang.sparqlstream;

public class ParserSPARQLStream {
	final static public Syntax sparqlStreamSyntax = new Syntax("csparql", "http://es.upm.fi.oeg.morph/sparql-stream");

	static class Syntax extends com.hp.hpl.jena.query.Syntax {
		protected Syntax(String lookupName, String uri) {
			super(uri);
			querySyntaxNames.put(lookupName, this);
		}
	}
}