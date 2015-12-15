package org.rspql.lang.cqels;

import java.time.Duration;
import java.util.List;

import org.apache.jena.atlas.io.IndentedWriter;
import org.rspql.syntax.ElementLogicalWindow;
import org.rspql.syntax.ElementNamedWindow;
import org.rspql.syntax.ElementPhysicalWindow;
import org.rspql.syntax.ElementWindow;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.sparql.serializer.SerializationContext;
import com.hp.hpl.jena.sparql.syntax.Element;
import com.hp.hpl.jena.sparql.syntax.ElementGroup;
import com.hp.hpl.jena.sparql.syntax.ElementSubQuery;
import com.hp.hpl.jena.sparql.util.FmtUtils;

public class FormatterElement extends com.hp.hpl.jena.sparql.serializer.FormatterElement {
	Query query = null;

	public FormatterElement(IndentedWriter out, SerializationContext context) {
		super(out, context);
	}

	public void visitAsGroup(Element el, List<ElementNamedWindow> windows) {
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

	public void visit(ElementWindow el) {
		// 2) Print this part instead according to CQELS
		// 3) Raise error on GRAPH?
		ElementNamedWindow window = null;
		for (ElementNamedWindow w : query.getNamedWindows()) {
			if (w.getWindowIri().toString().equals(el.getWindowNameNode().toString())) {
				window = w;
				break;
			}
		}
		if (window == null) {
			System.err.println("Error: No matching stream declared.");
			return;
		}

		//
		// Window iri and stream iri
		Node stream = (Node) window.getStream();
		String streamIri = stream.toString();
		if (stream.isURI()) {
			streamIri = FmtUtils.stringForURI(stream.getURI(), query);
		}
		out.print(String.format("STREAM %s ", streamIri));
		// Logical or physical window
		if (window.getClass().equals(ElementLogicalWindow.class)) {
			ElementLogicalWindow logicalWindow = (ElementLogicalWindow) window;
			String range = logicalWindow.getRange().toString();
			// Duration needs to be converted into an integer (‘d’|‘h’|‘m’|‘s’|‘ms’|‘ns’)
			if(!range.startsWith("?")){
				range =  getCQELSFormattedDuration(range);
			}
			
			if (logicalWindow.getStep() != null) {
				String step = logicalWindow.getStep().toString();
				if(!step.startsWith("?")){
					step =  getCQELSFormattedDuration(step);
				}
				out.print(String.format("[RANGE %s STEP %s]", range, step));
			} else {
				out.print(String.format("[RANGE %s]", range));
			}
		} else if (window.getClass().equals(ElementPhysicalWindow.class)) {
			ElementPhysicalWindow physicalWindow = (ElementPhysicalWindow) window;
			String range = physicalWindow.getSize().toString();
			if (physicalWindow.getStep() != null) {
				System.err.println("WARNING: SLIDE is not supported for logical windows in CQELS-QL and will be omitted ");
			}
			out.print(String.format("[TRIPLES %s]", range));
		}
		out.println();
		visitAsGroup(el.getElement());
	}
	
	public void setQuery(Query query) {
		this.query = query;
	}
	
	public String getCQELSFormattedDuration(String duration){
		Duration d = Duration.parse(duration);
		// Use the largest possible unit
		String unit = "s";
		double time = d.getSeconds();
		if(d.getNano() != 0){
			System.err.println(d.getNano() + "ns");
			unit = "ns";
			time *= 1000000;
			time += d.getNano();
			if(time % 1000000 == 0){
				unit = "ms";
				time /= 1000000;
			}
		} else {
			if(time % 60 == 0){
				unit = "m";
				time = time / 60;
			}
			if(time % 60 == 0){
				unit = "h";
				time = time / 60;
			}
			if(time % 24 == 0){
				unit = "d";
				time = time / 24;
			}
		}
		return Integer.toString((int)time) + unit;
	}
}
