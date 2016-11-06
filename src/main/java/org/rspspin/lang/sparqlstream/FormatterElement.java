package org.rspspin.lang.sparqlstream;

import java.util.List;

import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.query.QueryException;
import org.apache.jena.sparql.serializer.SerializationContext;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementNamedGraph;
import org.rspspin.syntax.ElementLogicalPastWindow;
import org.rspspin.syntax.ElementLogicalWindow;
import org.rspspin.syntax.ElementPhysicalWindow;
import org.rspspin.syntax.ElementWindowGraph;

public class FormatterElement extends org.apache.jena.sparql.serializer.FormatterElement {
	boolean strict = false;
	List<ElementLogicalWindow> logicalWindows;
	List<ElementLogicalPastWindow> logicalPastWindows;
	List<ElementPhysicalWindow> physicalWindows;

	public FormatterElement(IndentedWriter out, SerializationContext context) {
		super(out, context);
	}

	@Override
	public void visit(ElementWindowGraph el) {
		visitStreamPattern(el.getElement());
	}

	protected void visitStreamPattern(Element subElement) {
		visitStreamBlock(subElement, true);
	}

	public void visitStreamBlock(Element el, boolean first) {
		if (el instanceof ElementNamedGraph) {
			if(strict)
				throw new QueryException("ERROR: SPARQLStream does not support named graphs in streams.");
			else
				System.err.println("WARNING: SPARQLStream does not support named graphs in streams. Triples are collapsed into the default graph.");
		}

		// Take apart groups
		if (el.getClass().equals(ElementGroup.class)) {
			List<Element> elements = ((ElementGroup) el).getElements();
			for (Element e : elements) {
				visitStreamBlock(e, false);
			}
		} else if (el.getClass().equals(ElementNamedGraph.class)) {
			visitStreamBlock(((ElementNamedGraph) el).getElement(), false);
		} else {
			if(!first)
				out.println();
			el.visit(this);
			out.print(" .");
		}
	}

}
