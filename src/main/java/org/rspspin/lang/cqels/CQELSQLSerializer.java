package org.rspspin.lang.cqels;

import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.query.Query;
import org.apache.own.query.RSPQLQuery;
import org.apache.own.query.RSPQLQueryVisitor;
import org.apache.jena.sparql.serializer.FmtExprSPARQL;
import org.apache.jena.sparql.serializer.FormatterTemplate;
import org.apache.jena.sparql.syntax.Template;
import org.apache.own.sparql.serializer.RSPQLQuerySerializer;

public class CQELSQLSerializer extends RSPQLQuerySerializer implements RSPQLQueryVisitor {

	public CQELSQLSerializer(IndentedWriter iwriter, FormatterElement formatterElement, FmtExprSPARQL formatterExpr,
			FormatterTemplate formatterTemplate) {
		super(iwriter, formatterElement, formatterExpr, formatterTemplate);
	}

	@Override
	public void visitWindowDecl(Query inputQuery) {
		RSPQLQuery query = (RSPQLQuery)inputQuery;

		((FormatterElement) fmtElement).setWindows(query);
	}



	@Override
	public void visitSelectResultForm(Query inputQuery) {
		RSPQLQuery query = (RSPQLQuery)inputQuery;
		if (query.getOutputStreamType() != RSPQLQuery.OutputStreamTypeUnknown) {
			if (query.getOutputStreamType() != RSPQLQuery.OutputStreamTypeIstream) {
				System.err.println("WARNING: CQELS-QL only supports implicit Istream as the output stream operator.");
			}
		}
		super.visitSelectResultForm(query);
	}

	@Override
	public void visitConstructResultForm(Query inputQuery) {
		RSPQLQuery query = (RSPQLQuery)inputQuery;

		if (query.getOutputStreamType() != RSPQLQuery.OutputStreamTypeUnknown) {
			if (query.getOutputStreamType() != RSPQLQuery.OutputStreamTypeIstream) {
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
	public void visitOutputStreamDecl(Query inputQuery) {
		RSPQLQuery query = (RSPQLQuery)inputQuery;

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
