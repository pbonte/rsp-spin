package org.apache.jena.sparql.syntax;

import org.rspspin.syntax.ElementWindowGraph;

/**
 * A ElementVisitor that does nothing. It saves writing lots of empty visits
 * when only interested in a few element types.
 */

public class ElementVisitorBase implements ElementVisitor {
	@Override
	public void visit(ElementTriplesBlock el) {
	}

	@Override
	public void visit(ElementFilter el) {
	}

	@Override
	public void visit(ElementAssign el) {
	}

	@Override
	public void visit(ElementBind el) {
	}

	@Override
	public void visit(ElementData el) {
	}

	@Override
	public void visit(ElementUnion el) {
	}

	@Override
	public void visit(ElementDataset el) {
	}

	@Override
	public void visit(ElementOptional el) {
	}

	@Override
	public void visit(ElementGroup el) {
	}

	@Override
	public void visit(ElementNamedGraph el) {
	}

	@Override
	public void visit(ElementExists el) {
	}

	@Override
	public void visit(ElementNotExists el) {
	}

	@Override
	public void visit(ElementMinus el) {
	}

	@Override
	public void visit(ElementService el) {
	}

	@Override
	public void visit(ElementSubQuery el) {
	}

	@Override
	public void visit(ElementPathBlock el) {
	}

	@Override
	public void visit(ElementWindowGraph el) {
	}
}
