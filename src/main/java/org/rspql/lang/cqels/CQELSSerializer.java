/**
 * @author Robin Keskisarkka (https://github.com/keski)
 */

package org.rspql.lang.cqels;

import java.util.List;

import org.apache.jena.atlas.io.IndentedWriter;

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
 * Note that the CQELS keywords 'NOW' and 'ALL' for window range definitions is
 * not supported by RSP-QL.
 */

public class CQELSSerializer implements QueryVisitor {
	static final int BLOCK_INDENT = 2;
	protected FormatterTemplate fmtTemplate;
	protected FormatterElement fmtElement;
	protected FmtExprSPARQL fmtExpr;
	protected IndentedWriter out = null;

	public CQELSSerializer(IndentedWriter iwriter, FormatterElement formatterElement, FmtExprSPARQL formatterExpr,
			FormatterTemplate formatterTemplate) {
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
		System.err.println("WARNING: REGISTER AS is not supported in CQELS-QL and will be omitted.");
	}

	@Override
	public void visitSelectResultForm(Query query) {
		out.print("SELECT ");
		if (query.getStreamType() != null) {
			String type = query.getStreamType().toUpperCase();
			if (!type.equals("ISTREAM")) {
				System.err.println(String.format(
						"WARNING: %s is not supported in CQELS-QL. Implicit ISTREAM will be used instead. ", type));
			}
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
			String type = query.getStreamType().toUpperCase();
			if (!type.equals("ISTREAM")) {
				System.err.println(String.format(
						"WARNING: %s is not supported in CQELS-QL. Implicit ISTREAM will be used instead. ", type));
			}
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
		System.err.println("Error: DESCRIBE queries are not supported in CQELS-QL");
		return;
	}

	@Override
	public void visitAskResultForm(Query query) {
		System.err.println("Error: ASK queries are not supported in CQELS-QL");
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
}
