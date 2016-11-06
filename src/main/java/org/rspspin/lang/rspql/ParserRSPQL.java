package org.rspspin.lang;

import java.io.Reader;
import java.io.StringReader;

import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.atlas.logging.Log;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryException;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.query.QueryVisitor;
import org.apache.jena.shared.JenaException;
import org.apache.jena.sparql.core.Prologue;
import org.apache.jena.sparql.lang.SPARQLParser;
import org.apache.jena.sparql.lang.SPARQLParserFactory;
import org.apache.jena.sparql.lang.SPARQLParserRegistry;
import org.apache.jena.sparql.serializer.FmtExprSPARQL;
import org.apache.jena.sparql.serializer.FmtTemplate;
import org.apache.jena.sparql.serializer.QuerySerializerFactory;
import org.apache.jena.sparql.serializer.SerializationContext;
import org.apache.jena.sparql.serializer.SerializerRegistry;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.Template;
import org.apache.jena.sparql.util.NodeToLabelMapBNode;
import org.rspspin.lang.rspql.RSPQLParser;
import org.rspspin.lang.rspql.serializer.FormatterElement;
import org.rspspin.lang.rspql.serializer.QuerySerializer;

public class ParserRSPQL extends SPARQLParser {

	final static public Syntax syntax = new Syntax("rspql", "https://w3id.org/rsp/rspql");

	public static class Syntax extends org.apache.jena.query.Syntax {
		protected Syntax(String lookupName, String uri) {
			super(uri);
			querySyntaxNames.put(lookupName, this);
		}
	}

	/** Registers parser factory and serializer */
	static public void register() {

		SPARQLParserRegistry.addFactory(ParserRSPQL.syntax, new SPARQLParserFactory() {
			public boolean accept(org.apache.jena.query.Syntax syntax) {
				return syntax.equals(syntax);
			}

			public SPARQLParser create(org.apache.jena.query.Syntax syntax) {
				return new ParserRSPQL();
			}
		});

		// Register standard serializers
		QuerySerializerFactory factory = new QuerySerializerFactory() {

			@Override
			public QueryVisitor create(org.apache.jena.query.Syntax syntax, Prologue prologue, IndentedWriter writer) {
				// For the query pattern
				SerializationContext cxt1 = new SerializationContext(prologue, new NodeToLabelMapBNode("b", false));
				// For the construct pattern
				SerializationContext cxt2 = new SerializationContext(prologue, new NodeToLabelMapBNode("c", false));

				return new QuerySerializer(writer, new FormatterElement(writer, cxt1), new FmtExprSPARQL(writer, cxt1),
						new FmtTemplate(writer, cxt2));
			}

			@Override
			public QueryVisitor create(org.apache.jena.query.Syntax syntax, SerializationContext context,
					IndentedWriter writer) {
				return new QuerySerializer(writer, new FormatterElement(writer, context),
						new FmtExprSPARQL(writer, context), new FmtTemplate(writer, context));
			}

			@Override
			public boolean accept(org.apache.jena.query.Syntax syntax) {
				return ParserRSPQL.syntax.equals(syntax);
			}
		};
		SerializerRegistry.get().addQuerySerializer(syntax, factory);
	}

	private interface Action {
		void exec(RSPQLParser parser) throws Exception;
	}

	@Override
	protected Query parse$(final Query query, String queryString) {
		query.setSyntax(syntax);

		Action action = new Action() {
			@Override
			public void exec(RSPQLParser parser) throws Exception {
				parser.QueryUnit();
			}
		};

		perform(query, queryString, action);
		validateParsedQuery(query);
		return query;
	}

	public static Element parseElement(String string) {
		final Query query = new Query();
		Action action = new Action() {
			@Override
			public void exec(RSPQLParser parser) throws Exception {
				Element el = parser.GroupGraphPattern();
				query.setQueryPattern(el);
			}
		};
		perform(query, string, action);
		return query.getQueryPattern();
	}

	public static Template parseTemplate(String string) {
		final Query query = new Query();
		Action action = new Action() {
			@Override
			public void exec(RSPQLParser parser) throws Exception {
				Template t = parser.ConstructTemplate();
				query.setConstructTemplate(t);
			}
		};
		perform(query, string, action);
		return query.getConstructTemplate();
	}

	// All throwable handling.
	private static void perform(Query query, String string, Action action) {
		Reader in = new StringReader(string);
		RSPQLParser parser = new RSPQLParser(in);

		try {
			query.setStrict(true);
			parser.setQuery(query);
			action.exec(parser);
		} catch (org.apache.jena.sparql.lang.arq.ParseException ex) {
			throw new QueryParseException(ex.getMessage(), ex.currentToken.beginLine, ex.currentToken.beginColumn);
		} catch (org.apache.jena.sparql.lang.arq.TokenMgrError tErr) {
			// Last valid token : not the same as token error message - but this
			// should not happen
			int col = parser.token.endColumn;
			int line = parser.token.endLine;
			throw new QueryParseException(tErr.getMessage(), line, col);
		}

		catch (QueryException ex) {
			throw ex;
		} catch (JenaException ex) {
			throw new QueryException(ex.getMessage(), ex);
		} catch (Error err) {
			// The token stream can throw errors.
			throw new QueryParseException(err.getMessage(), err, -1, -1);
		} catch (Throwable th) {
			Log.warn(ParserRSPQL.class, "Unexpected throwable: ", th);
			throw new QueryException(th.getMessage(), th);
		}
	}
}
