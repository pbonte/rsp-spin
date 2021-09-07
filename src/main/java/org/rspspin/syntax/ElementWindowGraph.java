package org.rspspin.syntax;

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementVisitor;
import org.apache.own.sparql.syntax.WindowedElementVisitor;
import org.apache.jena.sparql.util.NodeIsomorphismMap;

/**
 * Evaluate a query element based on source information in a named collection.
 */

public class ElementWindowGraph extends Element {
	private Node sourceNode;
	private Element element;

	public ElementWindowGraph(Element el) {
		this(null, el);
	}

	// WINDOW <uri> or WINDOW ?var
	public ElementWindowGraph(Node n, Element el) {
		sourceNode = n;
		element = el;
	}

	public Node getWindowNameNode() {
		return sourceNode;
	}

	/** @return Returns the element. */
	public Element getElement() {
		return element;
	}

	@Override
	public void visit(ElementVisitor v) {
		try {
			WindowedElementVisitor windowVis = (WindowedElementVisitor) v;
			windowVis.visit(this);
		}catch (Exception e){
			//v.visit(this);
		}


	}

	@Override
	public int hashCode() {
		return element.hashCode() ^ sourceNode.hashCode();
	}

	@Override
	public boolean equalTo(Element el2, NodeIsomorphismMap isoMap) {
		if (el2 == null)
			return false;

		if (!(el2 instanceof ElementWindowGraph))
			return false;
		ElementWindowGraph g2 = (ElementWindowGraph) el2;
		if (!this.getWindowNameNode().equals(g2.getWindowNameNode()))
			return false;
		if (!this.getElement().equalTo(g2.getElement(), isoMap))
			return false;
		return true;
	}

	public void visit(WindowedElementVisitor v) {
		v.visit(this);
	}
}
