/**
 * @author Robin Keskisarkka (https://github.com/keski)
 */
package org.rspspin.syntax;

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementVisitor;
import org.apache.jena.sparql.util.NodeIsomorphismMap;

public class ElementLogicalWindow extends ElementWindow {
	private Node rangeNode;
	private Node stepNode;

	public ElementLogicalWindow(Node windowNameNode, Node streamNameNode, Node rangeNode, Node stepNode) {
		super(windowNameNode, streamNameNode);
		this.rangeNode = rangeNode;
		this.stepNode = stepNode;
	}

	@Override
	public boolean equalTo(Element el, NodeIsomorphismMap isoMap) {
		if (!(el instanceof ElementLogicalWindow))
			return false;
		ElementLogicalWindow w = (ElementLogicalWindow) el;
		return getWindowNameNode().equals(w.getWindowNameNode());
	}

	@Override
	public int hashCode() {
		return getWindowNameNode().hashCode() ^ getStreamNameNode().hashCode();
	}

	@Override
	public void visit(ElementVisitor v) {
	}

	public Node getRangeNode() {
		return rangeNode;
	}

	public Node getStepNode() {
		return stepNode;
	}
}
