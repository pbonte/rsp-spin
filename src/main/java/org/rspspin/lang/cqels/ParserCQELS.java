package org.rspspin.lang.cqels;

import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.query.QueryVisitor;
import org.apache.jena.sparql.core.Prologue;
import org.apache.jena.sparql.serializer.FmtExprSPARQL;
import org.apache.jena.sparql.serializer.QuerySerializerFactory;
import org.apache.jena.sparql.serializer.SerializationContext;
import org.apache.jena.sparql.serializer.SerializerRegistry;
import org.apache.jena.sparql.util.NodeToLabelMapBNode;

public class ParserCQELS {
	final static public Syntax syntax = new Syntax("cqels", "http://deri.org/2011/05/query/CQELS_01");

	static class Syntax extends org.apache.jena.query.Syntax {
		protected Syntax(String lookupName, String uri) {
			super(uri);
			querySyntaxNames.put(lookupName, this);
		}
	}

	/** Registers serializer */
	static public void register() {

		// Register serializer
		QuerySerializerFactory factory = new QuerySerializerFactory() {

			@Override
			public QueryVisitor create(org.apache.jena.query.Syntax syntax, Prologue prologue, IndentedWriter writer) {
				// For the query pattern
				SerializationContext cxt1 = new SerializationContext(prologue, new NodeToLabelMapBNode("b", false));
				// For the construct pattern
				SerializationContext cxt2 = new SerializationContext(prologue, new NodeToLabelMapBNode("c", false));

				return new CQELSQLSerializer(writer, new FormatterElement(writer, cxt1), new FmtExprSPARQL(writer, cxt1),
						new FmtTemplate(writer, cxt2));
			}

			@Override
			public QueryVisitor create(org.apache.jena.query.Syntax syntax, SerializationContext context,
					IndentedWriter writer) {
				return new CQELSQLSerializer(writer, new FormatterElement(writer, context),
						new FmtExprSPARQL(writer, context), new FmtTemplate(writer, context));
			}

			@Override
			public boolean accept(org.apache.jena.query.Syntax syntax) {
				return ParserCQELS.syntax.equals(syntax);
			}
		};
		SerializerRegistry.get().addQuerySerializer(syntax, factory);
	}
}