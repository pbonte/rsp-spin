/*******************************************************************************
 * Copyright (c) 2009 TopQuadrant, Inc.
 * All rights reserved. 
 ******************************************************************************
 * Modified by Robin Keskisarkka in accordance with the Apache License Version 2.0 
 * distribution of SPIN API (http://topbraid.org/spin/api/)
 */
package org.topbraid.spin.model.impl;

import java.time.Duration;
import java.util.LinkedList;
import java.util.List;

import org.rspql.syntax.ElementLogicalPastWindow;
import org.rspql.syntax.ElementLogicalWindow;
import org.rspql.syntax.ElementNamedWindow;
import org.rspql.syntax.ElementPhysicalWindow;
import org.topbraid.spin.arq.ARQ2SPIN;
import org.topbraid.spin.model.Element;
import org.topbraid.spin.model.ElementList;
import org.topbraid.spin.model.FunctionCall;
import org.topbraid.spin.model.SPINFactory;
import org.topbraid.spin.model.SolutionModifierQuery;
import org.topbraid.spin.model.Values;
import org.topbraid.spin.model.Variable;
import org.topbraid.spin.model.print.PrintContext;
import org.topbraid.spin.util.JenaUtil;
import org.topbraid.spin.vocabulary.SP;

import com.hp.hpl.jena.enhanced.EnhGraph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;

public abstract class QueryImpl extends AbstractSPINResourceImpl implements SolutionModifierQuery {

	public QueryImpl(Node node, EnhGraph graph) {
		super(node, graph);
	}

	public List<String> getFrom() {
		return getStringList(SP.from);
	}

	public List<String> getFromNamed() {
		return getStringList(SP.fromNamed);
	}

	public List<ElementNamedWindow> getFromNamedWindows() {
		LinkedList<ElementNamedWindow> windows = new LinkedList<>();
		NodeIterator iter = getModel().listObjectsOfProperty(this, SP.fromNamedWindow);
		while (iter.hasNext()) {
			Resource window = iter.next().asResource();
			String windowIri = window.getProperty(SP.windowIri).getObject().toString();
			Object streamIri = window.getProperty(SP.streamIri).getObject();

			// Window types
			Resource type = window.getProperty(RDF.type).getResource();
			if (type.equals(SP.LogicalWindow)) {
				Object range = window.getProperty(SP.windowRange).getObject();
				System.out.println(range);
				range = range instanceof Duration ? range.toString() : range;
				Object step = null;
				if (window.getProperty(SP.windowStep) != null) {
					step = window.getProperty(SP.windowStep).getObject();
					step = step instanceof Duration ? step.toString() : step;
				}
				windows.add(new ElementLogicalWindow(windowIri, streamIri, range, step));
			} else if (type.equals(SP.LogicalPastWindow)) {
				Object from = window.getProperty(SP.windowFrom).getObject();
				Object to = window.getProperty(SP.windowTo).getObject();
				from = from instanceof Duration ? from.toString() : from;
				to = to instanceof Duration ? to.toString() : to;

				Object step = null;
				if (window.getProperty(SP.windowStep) != null) {
					step = window.getProperty(SP.windowStep).getObject();
					step = step instanceof Duration ? step.toString() : step;
				}
				windows.add(new ElementLogicalPastWindow(windowIri, streamIri, from, to, step));
			} else if (type.equals(SP.PhysicalWindow)) {
				Object size = window.getProperty(SP.windowSize).getObject();
				Object step = null;
				if (window.getProperty(SP.windowStep) != null) {
					step = window.getProperty(SP.windowStep).getObject();
				}
				windows.add(new ElementPhysicalWindow(windowIri, streamIri, size, step));
			}
		}
		return windows;
	}

	public Long getLimit() {
		return getLong(SP.limit);
	}

	public Long getOffset() {
		return getLong(SP.offset);
	}

	private List<String> getStringList(Property predicate) {
		List<String> results = new LinkedList<String>();
		StmtIterator it = listProperties(predicate);
		while (it.hasNext()) {
			RDFNode node = it.nextStatement().getObject();
			if (node.isLiteral()) {
				results.add(((Literal) node).getLexicalForm());
			} else if (node.isURIResource()) {
				results.add(((Resource) node).getURI());
			}
		}
		return results;
	}

	@Override
	public Values getValues() {
		Resource values = JenaUtil.getResourceProperty(this, SP.values);
		if (values != null) {
			return values.as(Values.class);
		} else {
			return null;
		}
	}

	public ElementList getWhere() {
		Statement whereS = getProperty(SP.where);
		if (whereS != null) {
			Element element = SPINFactory.asElement(whereS.getResource());
			return (ElementList) element;
		} else {
			return null;
		}
	}

	public List<Element> getWhereElements() {
		return getElements(SP.where);
	}

	@Override
	public void print(PrintContext p) {
		String text = ARQ2SPIN.getTextOnly(this);
		if (text != null) {
			if (p.hasInitialBindings()) {
				throw new IllegalArgumentException(
						"Queries that only have an sp:text cannot be converted to a query string if initial bindings are present.");
			} else {
				p.print(text);
			}
		} else {
			printSPINRDF(p);
		}
	}

	protected abstract void printSPINRDF(PrintContext p);

	protected void printStringFrom(PrintContext context) {
		for (String from : getFrom()) {
			context.println();
			context.printKeyword("FROM");
			context.print(" <");
			context.print(from);
			context.print(">");
		}
		for (String fromNamed : getFromNamed()) {
			context.println();
			context.printKeyword("FROM NAMED");
			context.print(" <");
			context.print(fromNamed);
			context.print(">");
		}
		for (ElementNamedWindow window : getFromNamedWindows()) {
			context.println();
			// Window iri and stream iri
			String windowIri = "<" + window.getWindowIri() + ">";
			RDFNode streamNode = (RDFNode) window.getStream();
			String streamIri = "<" + streamNode.toString() + ">";
			if (streamNode instanceof Resource && ((Resource) streamNode).hasProperty(SP.varName)) {
				streamIri = String.format("?%s ", streamNode.as(Variable.class).getName());
			}
			context.printKeyword(String.format("FROM NAMED WINDOW %s ON %s ", windowIri, streamIri));

			// Window
			if (window.getClass().equals(ElementLogicalWindow.class)) {
				// Standard logical window
				ElementLogicalWindow logicalWindow = (ElementLogicalWindow) window;
				String range;
				RDFNode rangeNode = (RDFNode) logicalWindow.getRange();
				if (rangeNode.equals(SP.WindowNow)) {
					context.print(String.format("[%s]", "NOW"));
					continue;
				}
				if (rangeNode instanceof Resource && ((Resource) rangeNode).hasProperty(SP.varName)) {
					range = "?" + rangeNode.as(Variable.class).getName();
				} else {
					range = rangeNode.asLiteral().getString();
				}

				// Get step (optional)
				RDFNode stepNode = (RDFNode) logicalWindow.getStep();
				if (stepNode != null) {
					String step;
					if (stepNode instanceof Resource && ((Resource) stepNode).hasProperty(SP.varName)) {
						step = "?" + stepNode.as(Variable.class).getName();
					} else {
						step = stepNode.asLiteral().getString();
					}
					context.print(String.format("[RANGE %s STEP %s]", range, step));
				} else {
					context.print(String.format("[RANGE %s]", range));
				}
			} else if (window.getClass().equals(ElementLogicalPastWindow.class)) {
				// Logical past window
				ElementLogicalPastWindow logicalPastWindow = (ElementLogicalPastWindow) window;
				String from;
				RDFNode fromNode = (RDFNode) logicalPastWindow.getFrom();
				if (fromNode instanceof Resource && ((Resource) fromNode).hasProperty(SP.varName)) {
					from = "?" + fromNode.as(Variable.class).getName();
				} else {
					from = "NOW-" + fromNode.asLiteral().getString();
				}
				String to;
				RDFNode toNode = (RDFNode) logicalPastWindow.getTo();
				if (toNode instanceof Resource && ((Resource) toNode).hasProperty(SP.varName)) {
					to = "?" + toNode.as(Variable.class).getName();
				} else {
					to = "NOW-" + toNode.asLiteral().getString();
				}

				// Get step (optional)
				RDFNode stepNode = (RDFNode) logicalPastWindow.getStep();
				if (stepNode != null) {
					String step;
					if (stepNode instanceof Resource && ((Resource) stepNode).hasProperty(SP.varName)) {
						step = "?" + stepNode.as(Variable.class).getName();
					} else {
						step = stepNode.asLiteral().getString();
					}
					context.print(String.format("[FROM %s TO %s STEP %s]", from, to, step));
				} else {
					context.print(String.format("[FROM %s TO %s]", from, to));
				}
			} else if (window.getClass().equals(ElementPhysicalWindow.class)) {
				// Physical window
				ElementPhysicalWindow physicalWindow = (ElementPhysicalWindow) window;
				RDFNode sizeNode = (RDFNode) physicalWindow.getSize();
				String size;
				if (sizeNode instanceof Resource && ((Resource) sizeNode).hasProperty(SP.varName)) {
					size = "?" + sizeNode.as(Variable.class).getName();
				} else {
					if (sizeNode.equals(SP.WindowAll)) {
						context.print(String.format("[%s]", "ALL"));
						continue;
					}
					size = Integer.toString(sizeNode.asLiteral().getInt());
				}
				// Get step (optional)
				RDFNode stepNode = (RDFNode) physicalWindow.getStep();
				if (stepNode != null) {
					String step;
					if (stepNode instanceof Resource && ((Resource) stepNode).hasProperty(SP.varName)) {
						step = "?" + stepNode.as(Variable.class).getName();
					} else {
						step = Integer.toString(stepNode.asLiteral().getInt());
					}
					context.print(String.format("[ITEM %s STEP %s]", size, step));
				} else {
					context.print(String.format("[ITEM %s]", size));
				}
			}
		}
	}

	protected void printRegisterAs(PrintContext context) {
		RDFNode n = getRDFNode(SP.registerAs);
		if (n == null) {
			return;
		}
		context.printKeyword("REGISTER STREAM ");

		if (n instanceof Resource && ((Resource) n).hasProperty(SP.varName)) {
			context.print("?" + n.as(Variable.class).getName());
		} else {
			context.print("<" + n.toString() + ">");
		}
		context.printKeyword(" AS");
		context.println();
		context.println();
	}

	protected void printSolutionModifiers(PrintContext context) {
		List<RDFNode> orderBy = getList(SP.orderBy);
		if (!orderBy.isEmpty()) {
			context.println();
			context.printIndentation(context.getIndentation());
			context.printKeyword("ORDER BY");
			for (RDFNode node : orderBy) {
				if (node.isResource()) {
					Resource resource = (Resource) node;
					if (resource.hasProperty(RDF.type, SP.Asc)) {
						context.print(" ");
						context.printKeyword("ASC");
						context.print(" ");
						RDFNode expression = resource.getProperty(SP.expression).getObject();
						printOrderByExpression(context, expression);
					} else if (resource.hasProperty(RDF.type, SP.Desc)) {
						context.print(" ");
						context.printKeyword("DESC");
						context.print(" ");
						RDFNode expression = resource.getProperty(SP.expression).getObject();
						printOrderByExpression(context, expression);
					} else {
						context.print(" ");
						printOrderByExpression(context, node);
					}
				}
			}
		}
		Long limit = getLimit();
		if (limit != null) {
			context.println();
			context.printIndentation(context.getIndentation());
			context.printKeyword("LIMIT");
			context.print(" " + limit);
		}
		Long offset = getOffset();
		if (offset != null) {
			context.println();
			context.printIndentation(context.getIndentation());
			context.print("OFFSET");
			context.print(" " + offset);
		}
	}

	private void printOrderByExpression(PrintContext sb, RDFNode node) {

		if (node instanceof Resource) {
			Resource resource = (Resource) node;
			FunctionCall call = SPINFactory.asFunctionCall(resource);
			if (call != null) {
				sb.print("(");
				PrintContext pc = sb.clone();
				pc.setNested(true);
				call.print(pc);
				sb.print(")");
				return;
			}
		}

		printNestedExpressionString(sb, node, true);
	}

	protected void printValues(PrintContext p) {
		Values values = getValues();
		if (values != null) {
			p.println();
			values.print(p);
		}
	}

	protected void printWhere(PrintContext p) {
		p.printIndentation(p.getIndentation());
		p.printKeyword("WHERE");
		printNestedElementList(p, SP.where);
	}

	protected void printStreamType(PrintContext p) {
		Statement stmt = getProperty(SP.windowToStreamOperator);
		if (stmt != null) {
			Resource resource = getProperty(SP.windowToStreamOperator).getObject().asResource();
			if (resource.equals(SP.Rstream)) {
				p.printKeyword("RSTREAM ");
			} else if (resource.equals(SP.Istream)) {
				p.printKeyword("ISTREAM ");
			} else if (resource.equals(SP.Dstream)) {
				p.printKeyword("DSTREAM ");
			}
		}
	}
}
