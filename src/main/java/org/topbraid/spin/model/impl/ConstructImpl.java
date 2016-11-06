package org.topbraid.spin.model.impl;

import java.util.LinkedList;
import java.util.List;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.RDFNode;
import org.topbraid.spin.model.Construct;
import org.topbraid.spin.model.TripleTemplate;
import org.topbraid.spin.model.print.PrintContext;
import org.topbraid.spin.vocabulary.SP;

public class ConstructImpl extends QueryImpl implements Construct {

	public ConstructImpl(Node node, EnhGraph graph) {
		super(node, graph);
	}

	public List<TripleTemplate> getTemplates() {
		List<TripleTemplate> results = new LinkedList<TripleTemplate>();
		for (RDFNode next : getList(SP.templates)) {
			if (next != null && next.isResource()) {
				if(next.canAs(TripleTemplate.class))
					results.add(next.as(TripleTemplate.class));
			}
		}
		return results;
	}

	public void printSPINRDF(PrintContext context) {
		printComment(context);
		printPrefixes(context);
		context.printIndentation(context.getIndentation());
		printOutputStream(context);
		context.printKeyword("CONSTRUCT ");
		printOutputStreamOperator(context);
		printNestedElementList(context, SP.templates);
		printStringFrom(context);
		context.println();
		printWhere(context);
		printSolutionModifiers(context);
		printValues(context);
	}
}
