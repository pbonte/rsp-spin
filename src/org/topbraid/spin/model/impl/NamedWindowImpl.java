/*******************************************************************************
 * Copyright (c) 2009 TopQuadrant, Inc.
 * All rights reserved. 
 *******************************************************************************/
package org.topbraid.spin.model.impl;

import org.topbraid.spin.model.NamedWindow;
import org.topbraid.spin.model.SPINFactory;
import org.topbraid.spin.model.Variable;
import org.topbraid.spin.model.print.PrintContext;
import org.topbraid.spin.model.visitor.ElementVisitor;
import org.topbraid.spin.vocabulary.SP;

import com.hp.hpl.jena.enhanced.EnhGraph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.Resource;


public class NamedWindowImpl extends ElementImpl implements NamedWindow {
	
	public NamedWindowImpl(Node node, EnhGraph graph) {
		super(node, graph);
	}
	
	
	public Resource getNameNode() {
		Resource r = getResource(SP.windowNameNode);
		if(r != null) {
			Variable variable = SPINFactory.asVariable(r);
			if(variable != null) {
				return variable;
			}
			else {
				return r;
			}
		}
		else {
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
		visitor.visit(this);
	}
}
