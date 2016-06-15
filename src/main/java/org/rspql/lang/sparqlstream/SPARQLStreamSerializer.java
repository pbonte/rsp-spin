/**
 * @author Robin Keskisarkka (https://github.com/keski)
 */

package org.rspql.lang.sparqlstream;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;

import org.apache.jena.atlas.io.IndentedWriter;
import org.rspql.syntax.ElementLogicalPastWindow;
import org.rspql.syntax.ElementLogicalWindow;
import org.rspql.syntax.ElementNamedWindow;
import org.rspql.syntax.ElementPhysicalWindow;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryVisitor;
import com.hp.hpl.jena.query.SortCondition;
import com.hp.hpl.jena.sparql.core.Prologue;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.core.VarExprList;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.expr.Expr;
import com.hp.hpl.jena.sparql.serializer.FmtExprSPARQL;
import com.hp.hpl.jena.sparql.serializer.FormatterTemplate;
import com.hp.hpl.jena.sparql.serializer.PrologueSerializer;
import com.hp.hpl.jena.sparql.serializer.SerializationContext;
import com.hp.hpl.jena.sparql.syntax.Element;
import com.hp.hpl.jena.sparql.syntax.ElementGroup;
import com.hp.hpl.jena.sparql.util.FmtUtils;

/**
 * Serialize a query into CQELS-QL. Disclaimer: This is meant as a proof of
 * concept only. If you find any bugs or errors please use the issue tracker.
 * 
 * Known bugs: STREAM vs QUERY is not defined
 */

public class SPARQLStreamSerializer implements QueryVisitor {
	static final int BLOCK_INDENT = 2;
	protected FormatterTemplate fmtTemplate;
	protected FormatterElement fmtElement;
	protected FmtExprSPARQL fmtExpr;
	protected IndentedWriter out = null;

	public SPARQLStreamSerializer(IndentedWriter iwriter, FormatterElement formatterElement,
			FmtExprSPARQL formatterExpr, FormatterTemplate formatterTemplate) {
		out = iwriter;
		fmtTemplate = formatterTemplate;
		fmtElement = formatterElement;
		fmtExpr = formatterExpr;
	}

	@Override
	public void startVisit(Query query) {
		// Add a query reference in the formatter element
		fmtElement.setQuery(query);
	}

	@Override
	public void visitResultForm(Query query) {
	}

	@Override
	public void visitPrologue(Prologue prologue) {
		int row1 = out.getRow();
		PrologueSerializer.output(out, prologue);
		int row2 = out.getRow();
		if (row1 != row2)
			out.newline();
	}

	@Override
	public void visitRegisterAs(Query query) {
		Node n = query.getRegisterAs();
		if (n == null)
			return;
		System.err.println("WARNING: REGISTER AS is not supported in SPARQLStream and will be omitted.");
	}

	@Override
	public void visitSelectResultForm(Query query) {
		out.print("SELECT ");
		if (query.getStreamType() != null) {
			out.print(query.getStreamType() + " ");
		}
		if (query.isDistinct())
			out.print("DISTINCT ");
		if (query.isReduced())
			out.print("REDUCED ");
		out.print(" "); // Padding

		if (query.isQueryResultStar())
			out.print("*");
		else
			appendNamedExprList(query, out, query.getProject());
		out.newline();
	}

	@Override
	public void visitConstructResultForm(Query query) {
		out.print("CONSTRUCT ");
		if (query.getStreamType() != null) {
			out.print(query.getStreamType() + " ");
		}

		if (query.getStreamType() != null) {
			out.print(query.getStreamType() + " ");
		}
		out.incIndent(BLOCK_INDENT);
		out.newline();
		Element el = query.getConstructGraphTemplate();

		if (el instanceof ElementGroup) {
			fmtElement.visitResultGroup((ElementGroup) el);
		} else {
			fmtElement.visitAsGroup(el);
		}
		out.decIndent(BLOCK_INDENT);
	}

	@Override
	public void visitDescribeResultForm(Query query) {
		System.err.println("Error: DESCRIBE queries are not supported in C-SPARQL");
		return;
	}

	@Override
	public void visitAskResultForm(Query query) {
		System.err.println("Error: ASK queries are currently not supported");
		return;
	}

	@Override
	public void visitDatasetDecl(Query query) {
		if (query.getGraphURIs() != null && query.getGraphURIs().size() != 0) {
			for (String uri : query.getGraphURIs()) {
				out.print("FROM ");
				out.print(FmtUtils.stringForURI(uri, query));
				out.newline();
			}
		}
		if (query.getNamedGraphURIs() != null && query.getNamedGraphURIs().size() != 0) {
			for (String uri : query.getNamedGraphURIs()) {
				// One per line
				out.print("FROM NAMED ");
				out.print(FmtUtils.stringForURI(uri, query));
				out.newline();
			}
		}

		// SPARQLStream only allows a stream to be defined once, consequently
		// there can be only a single window over a stream. This limitation is
		// not
		// present in RSP-QL. If multiple logical windows are defined over the
		// same
		// stream the current approach identifies the outer bounds of all
		// windows defined over a query and takes the smallest step size found
		// and uses this to define a new stream definition.
		//
		// There is some confusion regarding some of the constructs and keywords
		// in SPARQLStream.
		HashMap<String, Stream> streams = new HashMap<>();
		for (ElementNamedWindow window : query.getNamedWindows()) {
			Node stream = (Node) window.getStream();
			String streamIri = stream.toString();
			if (stream.isURI()) {
				streamIri = FmtUtils.stringForURI(stream.getURI(), query);
			}

			Stream s = streams.containsKey(streamIri) ? streams.get(streamIri) : new Stream();
			streams.put(streamIri, s);
			if (window instanceof ElementLogicalPastWindow) {
				if (s.type != "") {
					System.err.println(
							String.format("WARNING: A %s window is already defined for %s (skipping)", s.type, streamIri));
					continue;
				}

				ElementLogicalPastWindow w = (ElementLogicalPastWindow) window;
				if (w.getStep() != null)
					s.setStep(w.getStep().toString());
				s.setFrom(w.getFrom().toString());
				s.setTo(w.getTo().toString());
				s.setType("past");
			} else if (window instanceof ElementPhysicalWindow) {
				System.err.println("WARNING: Physical windows is not supported (skipping).");
				continue;
			} else if (window instanceof ElementLogicalWindow) {
				if (s.isPhysical()) {
					System.err.println(String.format(
							"WARNING: A physical window is already defined for the stream %s (overriding)", streamIri));
					s = new Stream();
					streams.put(streamIri, s);
				}
				s.type = "logical";
				s.setRange(((ElementLogicalWindow) window).getRange().toString());
				s.setStep(((ElementLogicalWindow) window).getStep().toString());
			}
		}

		// Print all streams
		for (String streamIri : streams.keySet()) {
			Stream stream = streams.get(streamIri);
			out.print(String.format("FROM STREAM %s ", streamIri));

			// Logical or physical window
			if (stream.isLogical()) {
				String range = stream.range.toString();
				Object step = stream.step;
				if (step != null) {
					out.print(
							String.format("[NOW-%s SLIDE %s]", formatDuration(range), formatDuration(step.toString())));
				} else {
					out.print(String.format("[NOW-%s SLIDE]", formatDuration(range)));
				}
			} else if (stream.isPhysical()) {
				System.out.println(
						"WARNING: It's unclear how SPARQLStream supports tuple streams in the current version (skipping).");
			} else {
				out.print(String.format("[NOW-%s TO NOW-%s SLIDE", formatDuration(stream.from),
						formatDuration(stream.to)));
				if (stream.step.isEmpty()) {
					out.print("]");
				} else {
					out.print(String.format(" %s]", formatDuration(stream.step)));
				}
			}
			out.newline();
		}
	}

	@Override
	public void visitQueryPattern(Query query) {
		if (query.getQueryPattern() != null) {
			out.print("WHERE");
			out.incIndent(BLOCK_INDENT);
			out.newline();

			Element el = query.getQueryPattern();
			fmtElement.visitAsGroup(el);
			// el.visit(fmtElement) ;
			out.decIndent(BLOCK_INDENT);
			out.newline();
		}
	}

	@Override
	public void visitGroupBy(Query query) {
		if (query.hasGroupBy()) {
			// Can have an empty GROUP BY list if the groupin gis implicit
			// by use of an aggregate in the SELECT clause.
			if (!query.getGroupBy().isEmpty()) {
				out.print("GROUP BY ");
				appendNamedExprList(query, out, query.getGroupBy());
				out.println();
			}
		}
	}

	@Override
	public void visitHaving(Query query) {
		if (query.hasHaving()) {
			out.print("HAVING");
			for (Expr expr : query.getHavingExprs()) {
				out.print(" ");
				fmtExpr.format(expr);
			}
			out.println();
		}
	}

	@Override
	public void visitOrderBy(Query query) {
		if (query.hasOrderBy()) {
			out.print("ORDER BY ");
			boolean first = true;
			for (SortCondition sc : query.getOrderBy()) {
				if (!first)
					out.print(" ");
				sc.format(fmtExpr, out);
				first = false;
			}
			out.println();
		}
	}

	@Override
	public void visitLimit(Query query) {
		if (query.hasLimit()) {
			out.print("LIMIT   " + query.getLimit());
			out.newline();
		}
	}

	@Override
	public void visitOffset(Query query) {
		if (query.hasOffset()) {
			out.print("OFFSET  " + query.getOffset());
			out.newline();
		}
	}

	@Override
	public void visitValues(Query query) {
		if (query.hasValues()) {
			outputDataBlock(out, query.getValuesVariables(), query.getValuesData(), query);
			out.newline();
		}
	}

	public static void outputDataBlock(IndentedWriter out, List<Var> variables, List<Binding> values,
			Prologue prologue) {
		out.print("VALUES ");
		if (variables.size() == 1) {
			// Short form.
			out.print("?");
			out.print(variables.get(0).getVarName());
			out.print(" {");
			out.incIndent();
			for (Binding valueRow : values) {
				// A value may be null for UNDEF
				for (Var var : variables) {
					out.print(" ");
					Node value = valueRow.get(var);
					if (value == null)
						out.print("UNDEF");
					else
						out.print(FmtUtils.stringForNode(value, prologue));
				}
			}
			out.decIndent();
			out.print(" }");
			return;
		}
		// Long form.
		out.print("(");
		for (Var v : variables) {
			out.print(" ");
			out.print(v.toString());
		}
		out.print(" )");
		out.print(" {");
		out.incIndent();
		for (Binding valueRow : values) {
			out.println();
			// A value may be null for UNDEF
			out.print("(");
			for (Var var : variables) {
				out.print(" ");
				Node value = valueRow.get(var);
				if (value == null)
					out.print("UNDEF");
				else
					out.print(FmtUtils.stringForNode(value, prologue));
			}
			out.print(" )");
		}
		out.decIndent();
		out.ensureStartOfLine();
		out.print("}");
	}

	@Override
	public void finishVisit(Query query) {
		out.flush();
	}

	void appendVarList(Query query, IndentedWriter sb, List<String> vars) {
		boolean first = true;
		for (String varName : vars) {
			Var var = Var.alloc(varName);
			if (!first)
				sb.print(" ");
			sb.print(var.toString());
			first = false;
		}

	}

	void appendNamedExprList(Query query, IndentedWriter sb, VarExprList namedExprs) {
		boolean first = true;
		for (Var var : namedExprs.getVars()) {
			Expr expr = namedExprs.getExpr(var);
			if (!first)
				sb.print(" ");

			if (expr != null) {
				boolean needParens = true;

				if (expr.isFunction())
					needParens = false;
				else if (expr.isVariable())
					needParens = false;

				if (!Var.isAllocVar(var))
					needParens = true;

				if (needParens)
					out.print("(");
				fmtExpr.format(expr);
				if (!Var.isAllocVar(var)) {
					sb.print(" AS ");
					sb.print(var.toString());
				}
				if (needParens)
					out.print(")");
			} else {
				sb.print(var.toString());
			}
			first = false;
		}
	}

	static void appendURIList(Query query, IndentedWriter sb, List<Node> vars) {
		SerializationContext cxt = new SerializationContext(query);
		boolean first = true;
		for (Node node : vars) {
			if (!first)
				sb.print(" ");
			sb.print(FmtUtils.stringForNode(node, cxt));
			first = false;
		}
	}

	/**
	 * Helper class to manage multiple windows over a stream.
	 */
	public class Stream {
		String type = "";
		String step = "";
		String range = "";
		String from = "";
		String to = "";

		public void setFrom(String from) {
			this.from = from;
		}

		public void setTo(String to) {
			this.to = to;
		}

		public void setType(String type) {
			this.type = type;
		}

		public void setStep(String newStep) {
			if (step.equals("") || step.matches("^\\?")) {
				this.step = newStep;
				return;
			}

			if (isLogical()) {
				// Compare, we want the lowest
				Duration step1 = Duration.parse(step);
				Duration step2 = Duration.parse(newStep);
				if (step2.minus(step1).isNegative()) {
					step = step2.toString();
				}
			} else {
				// Not applicable
			}
		}

		public void setRange(String newRange) {
			if (range.equals("") || range.matches("^\\?")) {
				this.range = newRange;
				return;
			}

			if (isLogical()) {
				// Compare, we want the largest
				Duration range1 = Duration.parse(range);
				Duration range2 = Duration.parse(newRange);
				if (range1.minus(range2).isNegative()) {
					range = range2.toString();
				}
			} else {
				// Compare, we want the largest
				int range1 = Integer.parseInt(range);
				int range2 = Integer.parseInt(newRange);
				if (range1 < range2) {
					range = Integer.toString(range2);
				}
			}
		}

		public boolean isLogical() {
			return type.equals("logical");
		}

		public boolean isPhysical() {
			return type.equals("physical");
		}

		public boolean isPastWindow() {
			return type.equals("past");
		}
	}

	/**
	 * Returns a duration formatted for SPARQLStream. A single integer with the
	 * largest possible.
	 * 
	 * @param duration
	 * @return
	 */
	public String formatDuration(String duration) {
		if (duration.isEmpty()) {
			return "";
		}

		Duration d = Duration.parse(duration);
		// Use the largest possible unit
		String unit = "S";
		double time = d.getSeconds();
		if (d.getNano() != 0) {
			unit = "MS";
			time *= 1000000 + d.getNano();
			time /= 1000000;
		} else {
			if (time % 60 == 0) {
				unit = "MINUTE";
				time = time / 60;
			}
			if (time % 60 == 0) {
				unit = "HOUR";
				time = time / 60;
			}
			if (time % 24 == 0) {
				unit = "DAY";
				time = time / 24;
			}
			if (time % 7 == 0) {
				unit = "WEEK";
				time = time / 24;
			}
		}
		return String.format("%d %s", (int) time, unit);
	}
}

/*
 * Fix format of register stream/query as
 * 
 * 
 */
