/**
 * @author Robin Keskisarkka (https://github.com/keski)
 */
package org.rspspin.syntax;

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementVisitor;
import org.apache.own.sparql.syntax.WindowedElementVisitor;
import org.apache.jena.sparql.util.NodeIsomorphismMap;

public class ElementPhysicalWindow extends ElementWindow {
	private Node rangeNode;
	private Node stepNode;

	public ElementPhysicalWindow(Node windowNameNode, Node streamNameNode, Node rangeNode, Node stepNode) {
		super(windowNameNode, streamNameNode);
		this.rangeNode = rangeNode;
		this.stepNode = stepNode;
	}

	@Override
	public boolean equalTo(Element el, NodeIsomorphismMap isoMap) {
		if (!(el instanceof ElementPhysicalWindow))
			return false;
		ElementPhysicalWindow w = (ElementPhysicalWindow) el;
		return getWindowNameNode().equals(w.getWindowNameNode());
	}

	@Override
	public void visit(ElementVisitor v) {

	}

	@Override
	public int hashCode() {
		return getWindowNameNode().hashCode() ^ getStreamNameNode().hashCode();
	}

	public void visit(WindowedElementVisitor v) {
	}

	public Node getRangeNode() {
		return rangeNode;
	}

	public Node getStepNode() {
		return stepNode;
	}
}
