/*******************************************************************************
 * Copyright (c) 2009 TopQuadrant, Inc.
 * All rights reserved. 
 *******************************************************************************/
package org.topbraid.spin.model;

import com.hp.hpl.jena.rdf.model.Resource;


/**
 * A named window element (WINDOW keyword in RSP-SPARQL).
 * 
 * @author Robin Keskisarkka
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
