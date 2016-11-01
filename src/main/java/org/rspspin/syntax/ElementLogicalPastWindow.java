/**
 * @author Robin Keskisarkka (https://github.com/keski)
 */
package org.rspspin.syntax;

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementVisitor;
import org.apache.jena.sparql.util.NodeIsomorphismMap;

public class ElementLogicalPastWindow extends ElementWindow {
	private Node fromNode;
	private Node toNode;
	private Node stepNode;

	public ElementLogicalPastWindow(Node windowNameNode, Node streamNameNode, Node fromNode, Node toNode, Node stepNode) {
		super(windowNameNode, streamNameNode);
		this.fromNode = fromNode;
		this.toNode = toNode;
		this.stepNode = stepNode;
	}

	@Override
	public boolean equalTo(Element el, NodeIsomorphismMap isoMap) {
		if (!(el instanceof ElementLogicalPastWindow))
			return false;
		ElementLogicalPastWindow w = (ElementLogicalPastWindow) el;
		return getWindowNameNode().equals(w.getWindowNameNode());
	}

	@Override
	public int hashCode() {
		return getWindowNameNode().hashCode() ^ getStreamNameNode().hashCode();
	}

	@Override
	public void visit(ElementVisitor v) {
	}

	public Node getFromNode() {
		return fromNode;
	}

	public Node getToNode() {
		return toNode;
	}
	
	public Node getStepNode() {
		return stepNode;
	}
}
