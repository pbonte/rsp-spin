package org.rspspin.lang.cqels;

import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryVisitor;
import org.apache.jena.sparql.serializer.FmtExprSPARQL;
import org.apache.jena.sparql.serializer.FormatterTemplate;
import org.apache.jena.sparql.syntax.Template;

public class CQELSQLSerializer extends org.apache.jena.sparql.serializer.QuerySerializer implements QueryVisitor {

	public CQELSQLSerializer(IndentedWriter iwriter, FormatterElement formatterElement, FmtExprSPARQL formatterExpr,
			FormatterTemplate formatterTemplate) {
		super(iwriter, formatterElement, formatterExpr, formatterTemplate);
	}

	@Override
	public void visitWindowDecl(Query query) {
		((FormatterElement) fmtElement).setLogicalWindows(query.getLogicalWindows());
		((FormatterElement) fmtElement).setLogicalPastWindows(query.getLogicalPastWindows());
		((FormatterElement) fmtElement).setPhysicalWindows(query.getPhysicalWindows());
	}

	@Override
	public void visitSelectResultForm(Query query) {
		if (query.getOutputStreamType() != Query.OutputStreamTypeUnknown) {
			if (query.getOutputStreamType() != Query.OutputStreamTypeIstream) {
				System.err.println("WARNING: CQELS-QL only supports implicit Istream as the output stream operator.");
			}
		}
		super.visitSelectResultForm(query);
	}

	@Override
	public void visitConstructResultForm(Query query) {
		if (query.getOutputStreamType() != Query.OutputStreamTypeUnknown) {
			if (query.getOutputStreamType() != Query.OutputStreamTypeIstream) {
				System.err.println("WARNING: CQELS-QL only supports implicit Istream as the output stream operator.");
			}
		}
		out.print("CONSTRUCT ");
		out.incIndent(BLOCK_INDENT);
		out.newline();
		Template t = query.getConstructTemplate();
		fmtTemplate.format(t);
		out.decIndent(BLOCK_INDENT);
		out.incIndent();
	}

	@Override
	public void visitOutputStreamDecl(Query query) {
		if (query.getOutputStream() == null)
			return;
		System.err.println("WARNING: CQELS-QL does not support naming of the output stream.");
	}

	@Override
	public void visitDescribeResultForm(Query query) {
		System.err.println("Error: CQELS-QL does not support DESCRIBE queries.");
	}

	@Override
	public void visitAskResultForm(Query query) {
		System.err.println("Error: CQELS-QL does not support ASK queries.");
	}
}
