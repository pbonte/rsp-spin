package org.rspspin.util;

import org.apache.jena.rdf.model.RDFNode;
import org.topbraid.spin.model.SPINFactory;

public class Formatter {
	/**
	 * Return a URI or variable for printing in a query expression.
	 * 
	 * @param node
	 * @return
	 */
	public static String varOrUriAsString(RDFNode node) {
		RDFNode n = SPINFactory.asExpression(node);
		if (n.isURIResource())
			return "<" + n.toString() + ">";
		return n.toString();
	}

	/**
	 * Return a string representation of an RDFNode. For literals the lexical
	 * form will be returned.
	 * 
	 * @param node
	 * @return
	 */
	public static String varOrLiteralAsString(RDFNode node) {
		RDFNode n = SPINFactory.asExpression(node);
		if (n.isLiteral())
			return n.asLiteral().getLexicalForm();
		return n.toString();
	}

}
