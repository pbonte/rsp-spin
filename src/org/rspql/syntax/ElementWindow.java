package org.rspql.syntax;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.sparql.syntax.Element;
import com.hp.hpl.jena.sparql.syntax.ElementVisitor;
import com.hp.hpl.jena.sparql.util.NodeIsomorphismMap;

public class ElementWindow extends Element {
	private Node sourceNode;
	private Element element;

	public ElementWindow(Element el) {
		this(null, el);
	}

	public ElementWindow(Node n, Element el) {
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
	public int hashCode() {
		return element.hashCode() ^ sourceNode.hashCode();
	}

	@Override
	public boolean equalTo(Element el2, NodeIsomorphismMap isoMap) {
		if (el2 == null)
			return false;

		if (!(el2 instanceof ElementWindow))
			return false;
		ElementWindow g2 = (ElementWindow) el2;
		if (!this.getWindowNameNode().equals(g2.getWindowNameNode()))
			return false;
		if (!this.getElement().equalTo(g2.getElement(), isoMap))
			return false;
		return true;
	}

	@Override
	public void visit(ElementVisitor v) {
		v.visit(this);
	}
}
