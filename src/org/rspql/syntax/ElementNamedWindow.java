/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @author Robin Keskisarkka (https://github.com/keski)
 */
package org.rspql.syntax;

import com.hp.hpl.jena.sparql.syntax.Element;
import com.hp.hpl.jena.sparql.syntax.ElementVisitor;
import com.hp.hpl.jena.sparql.util.NodeIsomorphismMap;

public class ElementNamedWindow extends Element {
	private String windowIri;
	private Object streamIri;

	public ElementNamedWindow(String windowIri, Object streamIri) {
		this.windowIri = windowIri;
		this.streamIri = streamIri;
	}

	public String getWindowIri() {
		return windowIri;
	}

	public Object getStream() {
		return streamIri;
	}

	@Override
	public boolean equalTo(Element el2, NodeIsomorphismMap isoMap) {
		if (!(el2 instanceof ElementNamedWindow))
			return false;
		ElementNamedWindow f2 = (ElementNamedWindow) el2;
		if (!this.getWindowIri().equals(f2.getWindowIri()))
			return true;
		return false;
	}

	@Override
	public int hashCode() {
		return windowIri.hashCode() ^ streamIri.hashCode();
	}

	@Override
	public void visit(ElementVisitor v) {
		// TODO Auto-generated method stub
		
	}
}
