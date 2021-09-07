package org.apache.own.query;

import org.apache.jena.graph.Node;
import org.apache.jena.query.*;
import org.apache.jena.sparql.core.*;
import org.rspspin.syntax.ElementLogicalPastWindow;
import org.rspspin.syntax.ElementLogicalWindow;
import org.rspspin.syntax.ElementPhysicalWindow;

import java.util.*;

public class RSPQLQuery extends Query{



	public static final int OutputStreamTypeUnknown = -1;
	public static final int OutputStreamTypeIstream = 1;
	public static final int OutputStreamTypeDstream = 2;
	public static final int OutputStreamTypeRstream = 3;
	private int outputStreamType = OutputStreamTypeUnknown;

	private Node outputStream = null;
	private List<Node> namedWindowNodes = new ArrayList<>();
	private List<ElementLogicalWindow> logicalWindows = new ArrayList<>();
	private List<ElementLogicalPastWindow> logicalPastWindows = new ArrayList<>();
	private List<ElementPhysicalWindow> physicalWindows = new ArrayList<>();


	// LIMIT/OFFSET
	private Node resultLimit = null;
	private Node	 resultOffset = null;

	private Syntax syntax = Syntax.syntaxSPARQL; // Default

	/**
	 * Creates a new empty query
	 */
	public RSPQLQuery() {
		syntax = Syntax.syntaxSPARQL;
	}

	/**
	 * Creates a new empty query with the given prologue
	 */
	public RSPQLQuery(Prologue prologue) {
		super(prologue);
	}



	// ---- Limit/offset

	public Node getLimitNode() {
		return resultLimit;
	}

	public void setLimitNode(Node limit) {
		resultLimit = limit;
	}

	public boolean hasLimitNode() {
		return resultLimit != null;
	}

	public Node getOffsetNode() {
		return resultOffset;
	}

	public void setOffsetNode(Node offset) {
		resultOffset = offset;
	}

	public boolean hasOffsetNode() {
		return resultOffset != null;
	}


	public void visit(QueryVisitor visitor) {
		visitor.startVisit(this);
		visitor.visitResultForm(this);
		visitor.visitPrologue(this);
		if (visitor instanceof RSPQLQueryVisitor) {
			((RSPQLQueryVisitor) visitor).visitOutputStreamDecl(this);
		}
		if (this.isSelectType())
			visitor.visitSelectResultForm(this);
		if (this.isConstructType())
			visitor.visitConstructResultForm(this);
		if (this.isDescribeType())
			visitor.visitDescribeResultForm(this);
		if (this.isAskType())
			visitor.visitAskResultForm(this);
		visitor.visitDatasetDecl(this);
		if(visitor instanceof RSPQLQueryVisitor) {
			((RSPQLQueryVisitor)visitor).visitWindowDecl(this);
		}
		visitor.visitQueryPattern(this);
		visitor.visitGroupBy(this);
		visitor.visitHaving(this);
		visitor.visitOrderBy(this);
		visitor.visitOffset(this);
		visitor.visitLimit(this);
		visitor.visitValues(this);
		visitor.finishVisit(this);
	}


	/**
	 * Makes a copy of this query. Copies by parsing a query from the serialized
	 * form of this query
	 *
	 * @return Copy of this query
	 */
	public RSPQLQuery cloneQuery() {
		String qs = this.toString();
		return RSPQLQueryFactory.create(qs, getSyntax());
	}


	public void addLogicalWindow(Node windowNameNode, Node streamNameNode, Node rangeNode, Node stepNode) {
		checkDuplicateWindowUri(windowNameNode);
		namedWindowNodes.add(windowNameNode);
		ElementLogicalWindow window = new ElementLogicalWindow(windowNameNode, streamNameNode, rangeNode, stepNode);
		logicalWindows.add(window);
	}

	public void addLogicalPastWindow(Node windowNameNode, Node streamNameNode, Node rangeNode, Node toNode, Node stepNode) {
		checkDuplicateWindowUri(windowNameNode);
		namedWindowNodes.add(windowNameNode);
		ElementLogicalPastWindow window = new ElementLogicalPastWindow(windowNameNode, streamNameNode, rangeNode,
				toNode, stepNode);
		logicalPastWindows.add(window);
	}

	public void addPhysicalWindow(Node windowNameNode, Node streamNameNode, Node rangeNode, Node stepNode) {
		checkDuplicateWindowUri(windowNameNode);
		namedWindowNodes.add(windowNameNode);
		ElementPhysicalWindow window = new ElementPhysicalWindow(windowNameNode, streamNameNode, rangeNode, stepNode);
		physicalWindows.add(window);
	}

	private void checkDuplicateWindowUri(Node windowNameNode) {
		// This only does the check from a local query, not the super query.
		if(hasWindowUri(windowNameNode)){
			throw new QueryException("Window node already in named window set: " + windowNameNode);
		}
	}
	
	public boolean hasWindowUri(Node windowNameNode){
		for (Node n : namedWindowNodes) {
			if (n.toString().equals(windowNameNode.toString())) {
				return true;
			}
		}
		return false;
	}

	public List<ElementLogicalWindow> getLogicalWindows() {
		return logicalWindows;
	}

	public List<ElementLogicalPastWindow> getLogicalPastWindows() {
		return logicalPastWindows;
	}

	public List<ElementPhysicalWindow> getPhysicalWindows() {
		return physicalWindows;
	}

	public void setOutputstream(Node iri) {
		outputStream = iri;
	}
	
	public Node getOutputStream() {
		return outputStream;
	}
	
	public void setOutputStreamOp(int outputStreamOp){
		this.outputStreamType = outputStreamOp;
	}
	
	public int getOutputStreamType(){
		return outputStreamType;
	}

	public void setQueryDstreamType() {
		outputStreamType = OutputStreamTypeDstream;
	}
	
	public void setQueryIstreamType() {
		outputStreamType = OutputStreamTypeIstream;
	}
	
	public void setQueryRstreamType() {
		outputStreamType = OutputStreamTypeRstream;
	}
}
