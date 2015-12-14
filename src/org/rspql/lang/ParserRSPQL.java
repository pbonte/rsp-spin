package org.rspql.lang;

import java.io.Reader;
import java.io.StringReader;

import org.apache.jena.atlas.logging.Log;
import org.rspql.lang.rspql.ParseException;
import org.rspql.lang.rspql.RSPQLParser;
import org.rspql.lang.rspql.TokenMgrError;

import com.hp.hpl.jena.query.Query;
//import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryException;
import com.hp.hpl.jena.query.QueryParseException;
import com.hp.hpl.jena.shared.JenaException;
import com.hp.hpl.jena.sparql.lang.ParserARQ;
import com.hp.hpl.jena.sparql.lang.ParserSPARQL11;
import com.hp.hpl.jena.sparql.lang.SPARQLParser;
import com.hp.hpl.jena.sparql.lang.SPARQLParserFactory;
import com.hp.hpl.jena.sparql.lang.SPARQLParserRegistry;
import com.hp.hpl.jena.sparql.syntax.Element;
import com.hp.hpl.jena.sparql.syntax.Template;

public class ParserRSPQL extends ParserARQ {
	final static public Syntax rspqlSPARQLSyntax = new Syntax("rspql", "https://w3id.org/rsp/spin/rspql");

	static class Syntax extends com.hp.hpl.jena.query.Syntax {
		protected Syntax(String lookupName, String uri) {
			super(uri);
			querySyntaxNames.put(lookupName, this);
		}
	}

	/** Registers a parser factory for this parser. */
	static public void register() {
		// register this parser for the tSPARQL syntax
		SPARQLParserRegistry.addFactory(ParserRSPQL.rspqlSPARQLSyntax, new SPARQLParserFactory() {
			public boolean accept(com.hp.hpl.jena.query.Syntax syntax) {
				return syntax.equals(rspqlSPARQLSyntax);
			}

			public SPARQLParser create(com.hp.hpl.jena.query.Syntax syntax) {
				return new ParserRSPQL();
			}
		});
	}

	private interface Action {
		void exec(RSPQLParser parser) throws Exception;
	}

	protected Query parse$(final Query query, String queryString) {
		query.setSyntax(rspqlSPARQLSyntax);

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
		} catch (ParseException ex) {
			throw new QueryParseException(ex.getMessage(), ex.currentToken.beginLine, ex.currentToken.beginColumn);
		} catch (TokenMgrError tErr) {
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
			Log.warn(ParserSPARQL11.class, "Unexpected throwable: ", th);
			throw new QueryException(th.getMessage(), th);
		}
	}
}
