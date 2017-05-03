package org.rspspin.model;

import org.topbraid.spin.model.SPINFactory;
import org.topbraid.spin.model.Variable;
import org.topbraid.spin.model.print.PrintContext;
import org.topbraid.spin.model.visitor.ElementVisitor;
import org.topbraid.spin.model.impl.ElementImpl;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Resource;
import org.rspspin.vocabulary.RSPSPIN;

public class NamedWindowImpl extends ElementImpl implements NamedWindow {

	public NamedWindowImpl(Node node, EnhGraph graph) {
		super(node, graph);
	}

	public Resource getNameNode() {
		Resource r = getResource(RSPSPIN.windowNameNode);
		if (r != null) {
			Variable variable = SPINFactory.asVariable(r);
			if (variable != null) {
				return variable;
			} else {
				return r;
			}
		} else {
			return null;
		}
	}

	public void print(PrintContext p) {
		p.printKeyword("WINDOW");
		p.print(" ");
		printVarOrResource(p, getNameNode());
		printNestedElementList(p);
	}

	public void visit(ElementVisitor visitor) {
		System.err.println("visit window");
		visitor.visit(this);
	}
}