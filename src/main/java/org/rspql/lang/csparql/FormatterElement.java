package org.rspql.lang.csparql;

import org.apache.jena.atlas.io.IndentedWriter;
import org.rspql.syntax.ElementWindow;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.sparql.serializer.SerializationContext;
import com.hp.hpl.jena.sparql.syntax.Element;
import com.hp.hpl.jena.sparql.syntax.ElementGroup;
import com.hp.hpl.jena.sparql.syntax.ElementNamedGraph;
import com.hp.hpl.jena.sparql.syntax.ElementPathBlock;
import com.hp.hpl.jena.sparql.syntax.ElementSubQuery;

public class FormatterElement extends com.hp.hpl.jena.sparql.serializer.FormatterElement {
	Query query = null;

	public FormatterElement(IndentedWriter out, SerializationContext context) {
		super(out, context);
	}

	public void visitWindowGroup(ElementGroup elGroup) {
		for (Element el : elGroup.getElements()) {
			if (el instanceof ElementNamedGraph) {
				System.err.println("WARNING: GRAPH blocks in stream will be added directly to the where clause.");
			}
			el.visit(this);
			out.println();
		}
	}

	public void visitResultGroup(ElementGroup elGroup) {
		out.print("{ ");
		out.incIndent(INDENT);

		for (Element el : elGroup.getElements()) {
			if (el instanceof ElementNamedGraph) {
				System.err.println(
						"WARNING: Named graphs in results are not supported in C-SPARQL. The triples will be added directly to the default graph.");

				ElementGroup group = (ElementGroup) ((ElementNamedGraph) el).getElement();
				for (Element e : group.getElements()) {
					e.visit(this);
					if (e instanceof ElementPathBlock) {
						out.print(" .");
						out.println();
					}
				}
			} else {
				el.visit(this);
				out.println();
			}
		}

		out.decIndent(INDENT);
		out.print("}");
		out.println();
	}

	public void visit(ElementWindow el) {
		visitWindowGroup((ElementGroup) el.getElement());
	}

	public void setQuery(Query query) {
		this.query = query;
	}

	public void visitAsGroup(Element el) {
		boolean needBraces = !((el instanceof ElementGroup) || (el instanceof ElementSubQuery));
		
		if (needBraces) {
			out.print("{ ");
			out.incIndent(INDENT);
		}

		el.visit(this);

		if (needBraces) {
			out.decIndent(INDENT);
			out.print("}");
		}
	}
}
