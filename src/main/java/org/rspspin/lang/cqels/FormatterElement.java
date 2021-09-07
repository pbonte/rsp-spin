package org.rspspin.lang.cqels;

import java.time.Duration;
import java.util.List;

import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.graph.Node;
import org.apache.jena.query.QueryException;
import org.apache.jena.sparql.serializer.SerializationContext;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementNamedGraph;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.rspspin.syntax.ElementLogicalPastWindow;
import org.rspspin.syntax.ElementLogicalWindow;
import org.rspspin.syntax.ElementPhysicalWindow;
import org.rspspin.syntax.ElementWindow;
import org.rspspin.syntax.ElementWindowGraph;

public class FormatterElement extends org.apache.own.sparql.serializer.FormatterElement {
	boolean strict = false;
	boolean abbrStream = false;


	public FormatterElement(IndentedWriter out, SerializationContext context) {
		super(out, context);
	}

	@Override
	public void visit(ElementWindowGraph el) {
		visitStreamPattern(el.getWindowNameNode(), el.getElement());
	}

	protected void visitStreamPattern(Node node, Element subElement) {
		out.print("STREAM");
		out.print(" ");

		ElementWindow window = getWindow(node);
		if (window == null) {
			throw new QueryException("Unable to find matching window");
		}

		Node streamNode = window.getStreamNameNode();
		String stream = abbrStream ? slotToString(streamNode) : "<" + streamNode.toString() + ">";
		out.print(stream);

		// Logical window
		if (window.getClass().equals(ElementLogicalWindow.class)) {
			ElementLogicalWindow w = (ElementLogicalWindow) window;
			String range = cqelsTime(w.getRangeNode());
			if (w.getStepNode() != null) {
				String step = cqelsTime(w.getStepNode());
				out.print(String.format(" [RANGE %s SLIDE %s] ", range, step));
			} else {
				out.print(String.format(" [RANGE %s] ", range));
			}
		} else if (window.getClass().equals(ElementLogicalPastWindow.class)) {
			throw new QueryException("CQELS-QL does not support windows in the past.");
		} else if (window.getClass().equals(ElementPhysicalWindow.class)) {
			ElementPhysicalWindow w = (ElementPhysicalWindow) window;
			System.err.println("WARNING: CQELS-QL only supports triple streams.");
			String range = intOrVar(w.getRangeNode());
			if (w.getStepNode() != null) {
				System.err.println("WARNING: CQELS-QL does not support SLIDE for physical windows.");
			}
			out.print(String.format(" [TRIPLES %s] ", range));
		}
		out.print("{");
		out.println();
		out.incIndent(INDENT);
		visitStreamBlock(subElement);
		out.decIndent(INDENT);
		out.print("}");
	}

	/**
	 * 
	 */
	public void visitStreamBlock(Element el) {
		if (!(el instanceof ElementGroup || el instanceof ElementPathBlock || el instanceof ElementNamedGraph)) {
			if(strict)
				throw new QueryException("ERROR: CQELS-QL does not support " + el.getClass().getSimpleName() + " in stream blocks.");
			else
				System.err.println("WARNING: CQELS-QL does not support " + el.getClass().getSimpleName() + " in stream blocks.");
		}

		// Take apart groups
		if (el.getClass().equals(ElementGroup.class)) {
			List<Element> elements = ((ElementGroup) el).getElements();
			for (Element e : elements) {
				visitStreamBlock(e);
			}
		} else if (el.getClass().equals(ElementNamedGraph.class)) {
			visitStreamBlock(((ElementNamedGraph) el).getElement());
		} else {
			el.visit(this);
			out.println(" .");
		}
	}


	private String cqelsTime(Node node) {
		if (node.isVariable()) {
			return node.toString();
		}

		Duration d = Duration.parse(node.getLiteralLexicalForm());
		// Use the largest possible unit
		String unit = "s";
		double time = d.getSeconds();
		if (d.getNano() != 0) {
			System.err.println(d.getNano() + "ns");
			unit = "ns";
			time *= 1000000;
			time += d.getNano();
			if (time % 1000000 == 0) {
				unit = "ms";
				time /= 1000000;
			}
		} else {
			if (time % 60 == 0) {
				unit = "m";
				time = time / 60;
			}
			if (time % 60 == 0) {
				unit = "h";
				time = time / 60;
			}
			if (time % 24 == 0) {
				unit = "d";
				time = time / 24;
			}
		}
		return Integer.toString((int) time) + unit;
	}

	/**
	 * Integer or variable to string.
	 * 
	 * @param node
	 * @return
	 */
	private String intOrVar(Node node) {
		if (node.isVariable()) {
			return node.toString();
		}
		return node.getLiteralLexicalForm();
	}

	/**
	 * Find the declared window corresponding to the name of the window.
	 * 
	 * @param node
	 * @return
	 */
	private ElementWindow getWindow(Node node) {
		for (ElementWindow window : logicalWindows) {
			if (window.getWindowNameNode().equals(node))
				return window;
		}
		for (ElementWindow window : logicalPastWindows) {
			if (window.getWindowNameNode().equals(node))
				return window;
		}
		for (ElementWindow window : physicalWindows) {
			if (window.getWindowNameNode().equals(node))
				return window;
		}
		return null;
	}



}
