package org.rspspin.syntax;

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.syntax.Element;

abstract class ElementWindow extends Element {
	private Node windowNameNode;
	private Node streamNameNode;
	
	public ElementWindow(Node windowNameNode, Node streamNameNode){
		this.windowNameNode = windowNameNode;
		this.streamNameNode = streamNameNode;
	}
	
	public Node getWindowNameNode() {
		return windowNameNode;
	}

	public Node getStreamNameNode() {
		return streamNameNode;
	}
}
