package org.rspspin.lang.csparql;

import java.time.Duration;
import java.util.HashMap;

import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.graph.Node;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryException;
import org.apache.own.query.RSPQLQueryVisitor;
import org.apache.jena.sparql.core.Prologue;
import org.apache.jena.sparql.serializer.FmtExprSPARQL;
import org.apache.jena.sparql.serializer.FormatterTemplate;
import org.apache.jena.sparql.syntax.Template;
import org.apache.own.query.RSPQLQuery;
import org.apache.own.sparql.serializer.RSPQLQuerySerializer;
import org.rspspin.syntax.ElementLogicalWindow;
import org.rspspin.syntax.ElementPhysicalWindow;
import org.rspspin.syntax.ElementWindow;

public class CSPARQLSerializer extends RSPQLQuerySerializer implements RSPQLQueryVisitor {
	public boolean strict = false;

	public CSPARQLSerializer(IndentedWriter iwriter, FormatterElement formatterElement, FmtExprSPARQL formatterExpr,
			FormatterTemplate formatterTemplate) {
		super(iwriter, formatterElement, formatterExpr, formatterTemplate);
	}

	@Override
	public void visitPrologue(Prologue prologue) {

		// Output stream
		RSPQLQuery query = (RSPQLQuery) prologue;
		if (query.getOutputStream() == null)
			return;
		out.print("REGISTER ");
		if (query.getQueryType() == Query.QueryTypeSelect) {
			out.print("QUERY");
		} else {
			out.print("STREAM");
		}
		out.print(" ");

		out.print(asCsparqlName(query.getOutputStream()));
		out.println(" AS");
		out.println();

		super.visitPrologue(prologue);
	}

	/**
	 * Convert output stream URI into a legal CSPARQL name.
	 * 
	 * @param node
	 * @return
	 */
	private String asCsparqlName(Node node) {
		if (node.isVariable())
			return node.toString();
		String uri = node.toString();
		String[] parts = uri.split("[/#]");
		return parts[parts.length - 1];
	}

	@Override
	public void visitWindowDecl(Query inputQuery) {
		RSPQLQuery query = (RSPQLQuery)inputQuery;
		if (!query.getLogicalPastWindows().isEmpty()) {
			throw new QueryException("ERROR: CSPARQL does not support windows in the past.");
		}

		HashMap<String, ElementWindow> windowsMap = new HashMap<>();

		// Add logical windows
		for (ElementWindow window : query.getLogicalWindows()) {
			String streamName = window.getStreamNameNode().toString();
			// If already defined create a new window
			if (windowsMap.containsKey(streamName)) {
				windowsMap.put(streamName, combineWindows(window, windowsMap.get(streamName)));
			} else {
				windowsMap.put(streamName, window);
			}
		}
		// Add physical windows
		for (ElementWindow window : query.getPhysicalWindows()) {
			String streamName = window.getStreamNameNode().toString();
			// If already defined create a new window
			if (windowsMap.containsKey(streamName)) {
				windowsMap.put(streamName, combineWindows(window, windowsMap.get(streamName)));
			} else {
				windowsMap.put(streamName, window);
			}
		}

		// Print windows
		for (ElementWindow window : windowsMap.values()) {
			out.print(String.format("FROM STREAM <%s> ", window.getStreamNameNode().toString()));
			if (window.getClass().equals(ElementLogicalWindow.class)) {
				ElementLogicalWindow w = (ElementLogicalWindow) window;
				String range = csparqlTime(w.getRangeNode());
				String step = w.getStepNode() == null ? "1ms" : csparqlTime(w.getStepNode());

				out.println(String.format("[RANGE %s STEP %s]", range, step));
			} else if (window.getClass().equals(ElementPhysicalWindow.class)) {
				ElementPhysicalWindow w = (ElementPhysicalWindow) window;
				String range = intOrVar(w.getRangeNode());
				if (w.getStepNode() != null) {
					System.err.println("WARNING: CSPARQL does not support STEP for physical windows.");
				}
				out.println(String.format("[TRIPLES %s]", range));
			}
		}
	}

	/**
	 * Get node as variable or integer strings.
	 * 
	 * @param node
	 * @return
	 */
	private String intOrVar(Node node) {
		if (node.isVariable())
			return node.toString();
		return node.getLiteralLexicalForm();
	}

	/**
	 * Format a duration as CSPARQL time.
	 * 
	 * @param node
	 * @return
	 */
	private String csparqlTime(Node node) {
		if (node.isVariable())
			return node.toString();
		Duration d = Duration.parse(node.getLiteralLexicalForm());
		// Use the largest possible unit
		String unit = "s";
		double time = d.getSeconds();
		if (d.getNano() != 0) {
			unit = "ms";
			time *= 1000000 + d.getNano();
			time /= 1000000;
		} else {
			if (time % 60 == 0) {
				unit = "m";
				time = time / 60;
			}
			if (time % 60 == 0) {
				unit = "h";
				time = time / 60;
			}
			if (time % 24 == 0) {
				unit = "d";
				time = time / 24;
			}
		}
		return Integer.toString((int) time) + unit;
	}

	/**
	 * Combines two windows of the same type by applying the maximum range and
	 * minimum step. No step will be interpreted as the minimum supported step
	 * size. Clashing variables or window types throws an exception.
	 * 

	 * @return
	 */
	private ElementWindow combineWindows(ElementWindow window1, ElementWindow window2) {
		if (strict)
			throw new QueryException("ERROR: CSPARQL does not support multiple windows over a stream.");
		else
			System.err.println("WARNING: CSPARQL does not support multiple windows over a stream.");

		if (!window1.getClass().equals(window2.getClass())) {
			throw new QueryException("ERROR: Combining physical and logical windows is not possible.");
		}
		
		// Combine physical windows
		if(window1.getClass().equals(ElementPhysicalWindow.class)){
			ElementPhysicalWindow w1 = (ElementPhysicalWindow)window1;
			ElementPhysicalWindow w2 = (ElementPhysicalWindow)window2;
			Node range1 = w1.getRangeNode();
			Node range2 = w2.getRangeNode();
			Node range;
			if(range1.equals(range2)){
				range = range1;
			} else if(range1.isVariable() || range2.isVariable()){
				throw new QueryException("ERROR: Unable to combine windows accross variables.");
			} else {
				int a = Integer.parseInt(range1.getLiteralLexicalForm());
				int b = Integer.parseInt(range2.getLiteralLexicalForm());
				range = a > b ? range1 : range2;
			}
			return new ElementPhysicalWindow(window1.getWindowNameNode(), window1.getStreamNameNode(),
					range, null);
		}
		
		// Combine logical windows
		if(window1.getClass().equals(ElementLogicalWindow.class)){
			ElementLogicalWindow w1 = (ElementLogicalWindow)window1;
			ElementLogicalWindow w2 = (ElementLogicalWindow)window2;
			
			// Range
			Node range1 = w1.getRangeNode();
			Node range2 = w2.getRangeNode();
			Node range;
			if(range1.equals(range2)){
				range = range1;
			} else if(range1.isVariable() || range2.isVariable()){
				throw new QueryException("ERROR: Unable to combine windows accross variables.");
			} else {
				Duration a = Duration.parse(range1.getLiteralLexicalForm());
				Duration b = Duration.parse(range2.getLiteralLexicalForm());
				range = a.minus(b).isNegative() ? range2 : range1;
			}
			
			// Step
			Node step1 = w1.getStepNode();
			Node step2 = w2.getStepNode();
			Node step;
			if(step1.equals(step2)){
				step = step1;
			} else if(step1.isVariable() || step2.isVariable()){
				throw new QueryException("ERROR: Unable to combine windows accross variables.");
			} else {
				Duration a = Duration.parse(step1.getLiteralLexicalForm());
				Duration b = Duration.parse(step2.getLiteralLexicalForm());
				step = a.minus(b).isNegative() ? step1 : step2;
			}
			
			return new ElementLogicalWindow(window1.getWindowNameNode(), window1.getStreamNameNode(),
					range, step);
		}

		return null;
	}

	@Override
	public void visitSelectResultForm(Query inputQuery) {
		RSPQLQuery query = (RSPQLQuery) inputQuery;
		if (query.getOutputStreamType() != RSPQLQuery.OutputStreamTypeUnknown) {
			if (query.getOutputStreamType() != RSPQLQuery.OutputStreamTypeRstream) {
				System.err.println("WARNING: CSPARQL only supports implicit Rstream as the output stream operator.");
			}
		}
		super.visitSelectResultForm(query);
	}

	@Override
	public void visitConstructResultForm(Query inputQuery) {
		RSPQLQuery query = (RSPQLQuery) inputQuery;
		if (query.getOutputStreamType() != RSPQLQuery.OutputStreamTypeUnknown) {
			if (query.getOutputStreamType() != RSPQLQuery.OutputStreamTypeRstream) {
				System.err.println("WARNING: CSPARQL only supports implicit Rstream as the output stream operator.");
			}
		}
		out.print("CONSTRUCT ");
		out.incIndent(BLOCK_INDENT);
		out.newline();
		Template t = query.getConstructTemplate();
		fmtTemplate.format(t);
		out.decIndent(BLOCK_INDENT);
		out.incIndent();
	}

	@Override
	public void visitOutputStreamDecl(Query query) {
		// performed as part of prologue
	}

	@Override
	public void visitDescribeResultForm(Query query) {
		System.err.println("Error: CSPARQL does not support DESCRIBE queries.");
	}

	@Override
	public void visitAskResultForm(Query query) {
		System.err.println("Error: C-SPARQL does not support ASK queries.");
	}
}
