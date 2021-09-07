package org.rspspin.lang.cqels;

import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.own.query.RSPQLQueryVisitor;
import org.apache.jena.sparql.core.Prologue;
import org.apache.jena.sparql.serializer.FmtExprSPARQL;
import org.apache.jena.sparql.serializer.QuerySerializerFactory;
import org.apache.jena.sparql.serializer.SerializationContext;
import org.apache.jena.sparql.serializer.SerializerRegistry;
import org.apache.jena.sparql.util.NodeToLabelMapBNode;
import org.rspspin.lang.RSPQLSyntax;

public class ParserCQELS  {
	final static public RSPQLSyntax syntax = new RSPQLSyntax("cqels", "http://deri.org/2011/05/query/CQELS_01");



	/** Registers serializer */
	static public void register() {

		// Register serializer
		QuerySerializerFactory factory = new QuerySerializerFactory() {

			@Override
			public RSPQLQueryVisitor create(org.apache.jena.query.Syntax syntax, Prologue prologue, IndentedWriter writer) {
				// For the query pattern
				SerializationContext cxt1 = new SerializationContext(prologue, new NodeToLabelMapBNode("b", false));
				// For the construct pattern
				SerializationContext cxt2 = new SerializationContext(prologue, new NodeToLabelMapBNode("c", false));

				return new CQELSQLSerializer(writer, new FormatterElement(writer, cxt1), new FmtExprSPARQL(writer, cxt1),
						new FmtTemplate(writer, cxt2));
			}

			@Override
			public RSPQLQueryVisitor create(org.apache.jena.query.Syntax syntax, SerializationContext context,
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