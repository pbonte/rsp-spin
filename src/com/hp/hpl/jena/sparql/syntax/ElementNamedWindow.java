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
package com.hp.hpl.jena.sparql.syntax;

import com.hp.hpl.jena.sparql.util.NodeIsomorphismMap;

public class ElementNamedWindow extends Element {
	private String windowIri;
	private Object stream;
	private Object range;
	private Object step;

	public ElementNamedWindow(String windowIri, Object stream, Object range, Object step) {
		this.windowIri = windowIri;
		this.stream = stream;
		this.step = step;
		this.range = range;
	}

	public String getWindowIri() {
		return windowIri;
	}

	public Object getStream() {
		return stream;
	}

	public Object getRange() {
		return range;
	}

	public Object getStep() {
		return step;
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
		return windowIri.hashCode() ^ stream.hashCode();
	}

	@Override
	public void visit(ElementVisitor v) {
		v.visit(this);
	}
}
