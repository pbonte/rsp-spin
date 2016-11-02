package org.rspspin.model;

import org.apache.jena.rdf.model.Resource;
import org.topbraid.spin.model.ElementGroup;


/**
 * A named window element (WINDOW keyword in RSP-QL).
 */
public interface NamedWindow extends ElementGroup {

	/**
	 * Gets the URI Resource or Variable that holds the name of this
	 * named window.  If it's a Variable, then this method will typecast
	 * it into an instance of Variable.
	 * @return a Resource or Variable
	 */
	Resource getNameNode();
}