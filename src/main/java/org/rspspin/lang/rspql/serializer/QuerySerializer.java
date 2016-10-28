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

package org.rspspin.lang.rspql.serializer;

import java.io.OutputStream;

import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.graph.Node;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryVisitor;
import org.apache.jena.sparql.serializer.FmtExprSPARQL;
import org.apache.jena.sparql.serializer.FormatterElement;
import org.apache.jena.sparql.serializer.FormatterTemplate;
import org.apache.jena.sparql.util.FmtUtils;
import org.rspspin.syntax.ElementLogicalWindow;
import org.rspspin.syntax.ElementPhysicalWindow;
import org.rspspin.syntax.ElementLogicalPastWindow;

/**
 * Serialize a query into RSP-QL or standard SPARQL/ARQ
 */
public class QuerySerializer extends org.apache.jena.sparql.serializer.QuerySerializer implements QueryVisitor {
	public QuerySerializer(OutputStream _out, FormatterElement formatterElement, FmtExprSPARQL formatterExpr,
			FormatterTemplate formatterTemplate) {
		this(new IndentedWriter(_out), formatterElement, formatterExpr, formatterTemplate);
	}

	public QuerySerializer(IndentedWriter iwriter, FormatterElement formatterElement, FmtExprSPARQL formatterExpr,
			FormatterTemplate formatterTemplate) {
		super(iwriter, formatterElement, formatterExpr, formatterTemplate);
	}

	@Override
	public void visitWindowDecl(Query query) {
		// Logical windows
		for (ElementLogicalWindow window : query.getLogicalWindows()) {
			out.print("FROM NAMED WINDOW ");
			out.print(FmtUtils.stringForNode(window.getWindowNameNode(), query.getPrefixMapping()));
			out.print(" ON ");
			out.print(FmtUtils.stringForNode(window.getStreamNameNode(), query.getPrefixMapping()));
			out.print(" [ ");
			out.print("RANGE ");
			out.print(stringForLiteral(window.getRangeNode()));
			out.print(" STEP ");
			out.print(stringForLiteral(window.getStepNode()));
			out.print("]");
			out.newline();
		}
		// Logical past windows
		for (ElementLogicalPastWindow window : query.getLogicalPastWindows()) {
			out.print("FROM NAMED WINDOW ");
			out.print(FmtUtils.stringForNode(window.getWindowNameNode(), query.getPrefixMapping()));
			out.print(" ON ");
			out.print(FmtUtils.stringForNode(window.getStreamNameNode(), query.getPrefixMapping()));
			out.print(" [ ");
			out.print("FROM ");
			out.print(stringForPastDuration(window.getFromNode()));
			out.print(" TO ");
			out.print(stringForPastDuration(window.getToNode()));
			out.print("]");
			out.newline();
		}
		// Physical windows
		for (ElementPhysicalWindow window : query.getPhysicalWindows()) {
			out.print("FROM NAMED WINDOW ");
			out.print(FmtUtils.stringForNode(window.getWindowNameNode(), query.getPrefixMapping()));
			out.print(" ON ");
			out.print(FmtUtils.stringForNode(window.getStreamNameNode(), query.getPrefixMapping()));
			out.print(" [ ");
			out.print("ITEM ");
			out.print(stringForLiteral(window.getRangeNode()));
			out.print(" STEP ");
			out.print(stringForLiteral(window.getStepNode()));
			out.print("]");
			out.newline();
		}
	}

	public String stringForLiteral(Node n) {
		if (n.isVariable()) {
			return n.toString();
		}
		return n.getLiteral().getValue().toString();
	}

	public String stringForPastDuration(Node n) {
		if (n.isVariable()) {
			return n.toString();
		}
		return "NOW-" + n.getLiteral().getValue().toString();
	}

	public void visitOutputStreamDecl(Query query) {
		if(query.getOutputStream() != null){
			out.print("REGISTER STREAM ");
			out.print(FmtUtils.stringForNode(query.getOutputStream()));
			out.print(" AS ");
			out.newline();
			out.newline();
		}
	}
}
