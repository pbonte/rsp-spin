package org.rspspin.lang.rspql.serializer;

import java.io.OutputStream;

import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.graph.Node;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryVisitor;
import org.apache.jena.sparql.serializer.FmtExprSPARQL;
import org.apache.jena.sparql.serializer.FormatterTemplate;
import org.apache.jena.sparql.syntax.Template;
import org.apache.jena.sparql.util.FmtUtils;
import org.rspspin.syntax.ElementLogicalWindow;
import org.rspspin.syntax.ElementPhysicalWindow;
import org.rspspin.syntax.ElementLogicalPastWindow;

/**
 * Serialize a query into RSP-QL or standard SPARQL/ARQ
 */
public class QuerySerializer extends org.apache.jena.sparql.serializer.QuerySerializer implements QueryVisitor {
	public QuerySerializer(OutputStream _out, FormatterElement formatterElement, FmtExprSPARQL formatterExpr,
			FormatterTemplate formatterTemplate) {
		this(new IndentedWriter(_out), formatterElement, formatterExpr, formatterTemplate);
	}

	public QuerySerializer(IndentedWriter iwriter, FormatterElement formatterElement, FmtExprSPARQL formatterExpr,
			FormatterTemplate formatterTemplate) {
		super(iwriter, formatterElement, formatterExpr, formatterTemplate);
	}

	@Override
	public void visitSelectResultForm(Query query) {
		out.print("SELECT ");
		if (query.isDistinct())
			out.print("DISTINCT ");
		if (query.isReduced())
			out.print("REDUCED ");
		if (query.getOutputStreamType() != Query.OutputStreamTypeUnknown) {
			switch (query.getOutputStreamType()) {
			case Query.OutputStreamTypeIstream:
				out.print("ISTREAM ");
				break;
			case Query.OutputStreamTypeRstream:
				out.print("RSTREAM ");
				break;
			case Query.OutputStreamTypeDstream:
				out.print("DSTREAM ");
				break;
			}
		}
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
		if (query.getOutputStreamType() != Query.OutputStreamTypeUnknown) {
			switch (query.getOutputStreamType()) {
			case Query.OutputStreamTypeIstream:
				out.print("ISTREAM ");
				break;
			case Query.OutputStreamTypeRstream:
				out.print("RSTREAM ");
				break;
			case Query.OutputStreamTypeDstream:
				out.print("DSTREAM ");
				break;
			}
		}
		out.incIndent(BLOCK_INDENT);
		out.newline();
		Template t = query.getConstructTemplate();
		fmtTemplate.format(t);
		out.decIndent(BLOCK_INDENT);
	}
	
	@Override
	public void visitWindowDecl(Query query) {
		// Logical windows
		for (ElementLogicalWindow window : query.getLogicalWindows()) {
			out.print("FROM NAMED WINDOW ");
			out.print(FmtUtils.stringForNode(window.getWindowNameNode(), query.getPrefixMapping()));
			out.print(" ON ");
			out.print(FmtUtils.stringForNode(window.getStreamNameNode(), query.getPrefixMapping()));
			out.print(" [");
			out.print("RANGE ");
			out.print(stringForLiteral(window.getRangeNode()));
			if (window.getStepNode() != null) {
				out.print(" STEP ");
				out.print(stringForLiteral(window.getStepNode()));
			}
			out.print("]");
			out.newline();
		}
		// Logical past windows
		for (

		ElementLogicalPastWindow window : query.getLogicalPastWindows()) {
			out.print("FROM NAMED WINDOW ");
			out.print(FmtUtils.stringForNode(window.getWindowNameNode(), query.getPrefixMapping()));
			out.print(" ON ");
			out.print(FmtUtils.stringForNode(window.getStreamNameNode(), query.getPrefixMapping()));
			out.print(" [");
			out.print("FROM ");
			out.print(stringForPastDuration(window.getFromNode()));
			out.print(" TO ");
			out.print(stringForPastDuration(window.getToNode()));
			if(window.getStepNode() != null){
				out.print(" STEP ");
				out.print(stringForLiteral(window.getStepNode()));
			}
			out.print("]");
			out.newline();
		}
		// Physical windows
		for (ElementPhysicalWindow window : query.getPhysicalWindows()) {
			out.print("FROM NAMED WINDOW ");
			out.print(FmtUtils.stringForNode(window.getWindowNameNode(), query.getPrefixMapping()));
			out.print(" ON ");
			out.print(FmtUtils.stringForNode(window.getStreamNameNode(), query.getPrefixMapping()));
			out.print(" [");
			out.print("ITEM ");
			out.print(stringForLiteral(window.getRangeNode()));
			if (window.getStepNode() != null) {
				out.print(" STEP ");
				out.print(stringForLiteral(window.getStepNode()));
			}
			out.print("]");
			out.newline();
		}
	}

	public String stringForLiteral(Node n) {
		if (n.isVariable()) {
			return n.toString();
		}
		return n.getLiteral().getValue().toString();
	}

	public String stringForPastDuration(Node n) {
		if (n.isVariable()) {
			return "NOW-" + n.toString();
		}
		return "NOW-" + n.getLiteral().getValue().toString();
	}

	public void visitOutputStreamDecl(Query query) {
		if (query.getOutputStream() != null) {
			out.print("REGISTER STREAM ");
			out.print(FmtUtils.stringForNode(query.getOutputStream()));
			out.print(" AS");
			out.newline();
			out.newline();
		}
	}
}
