package org.rspspin.lang.sparqlstream;

import java.time.Duration;
import java.util.HashMap;

import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.graph.Node;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryException;
import org.apache.jena.query.QueryVisitor;
import org.apache.jena.sparql.serializer.FmtExprSPARQL;
import org.apache.jena.sparql.serializer.FormatterTemplate;
import org.apache.jena.sparql.syntax.Template;
import org.rspspin.syntax.ElementLogicalPastWindow;
import org.rspspin.syntax.ElementLogicalWindow;
import org.rspspin.syntax.ElementPhysicalWindow;
import org.rspspin.syntax.ElementWindow;

public class SPARQLStreamSerializer extends org.apache.jena.sparql.serializer.QuerySerializer implements QueryVisitor {
	public boolean strict = false;

	public SPARQLStreamSerializer(IndentedWriter iwriter, FormatterElement formatterElement,
			FmtExprSPARQL formatterExpr, FormatterTemplate formatterTemplate) {
		super(iwriter, formatterElement, formatterExpr, formatterTemplate);
	}

	@Override
	public void visitWindowDecl(Query query) {
		if (!query.getPhysicalWindows().isEmpty()) {
			throw new QueryException("ERROR: SPARQLStream does not support physical windows.");
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
		
		// Add logical past windows
		for (ElementWindow window : query.getLogicalPastWindows()) {
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
				String range = sparqlstreamTime(w.getRangeNode());
				if(w.getStepNode() != null){
					String step = sparqlstreamTime(w.getStepNode());
					out.println(String.format("[NOW-%s SLIDE %s]", range, step));
				} else {
					out.println(String.format("[NOW-%s]", range));
				}
			}
			if (window.getClass().equals(ElementLogicalPastWindow.class)) {
				ElementLogicalPastWindow w = (ElementLogicalPastWindow) window;
				String from = sparqlstreamTime(w.getFromNode());
				String to = sparqlstreamTime(w.getToNode());
				if(w.getStepNode() != null){
					String step = sparqlstreamTime(w.getStepNode());
					out.println(String.format("[NOW-%s TO NOW-%s SLIDE %s]", from, to, step));
				} else {
					out.println(String.format("[NOW-%s TO %s]", from, to));
				}
			}
		}
	}

	/**
	 * Format a duration as SPARQLStream time.
	 * 
	 * @param node
	 * @return
	 */
	private String sparqlstreamTime(Node node) {
		if (node.isVariable())
			return node.toString();
		Duration d = Duration.parse(node.getLiteralLexicalForm());
		// Use the largest possible unit
		String unit = "S";
		double time = d.getSeconds();
		if (d.getNano() != 0) {
			unit = "MS";
			time *= 1000000 + d.getNano();
			time /= 1000000;
		} else {
			if (time % 60 == 0) {
				unit = "MINUTE";
				time = time / 60;
			}
			if (time % 60 == 0) {
				unit = "HOUR";
				time = time / 60;
			}
			if (time % 24 == 0) {
				unit = "DAY";
				time = time / 24;
			}
			if (time % 7 == 0) {
				unit = "WEEK";
				time = time / 24;
			}
		}
		return String.format("%d %s", (int) time, unit);
	}

	/**
	 * Combines two windows of the same type by applying the maximum range and
	 * minimum step. No step will be interpreted as the minimum supported step
	 * size. Clashing variables or window types throws an exception.
	 * 
	 * @param window
	 * @param elementWindow
	 * @return
	 */
	private ElementWindow combineWindows(ElementWindow window1, ElementWindow window2) {
		if (strict)
			throw new QueryException("ERROR: SPARQLStream does not support multiple windows over a stream.");
		else
			System.err.println("WARNING: SPARQLStream does not support multiple windows over a stream.");

		if (!window1.getClass().equals(window2.getClass())) {
			throw new QueryException("ERROR: Combining logical and logical past windows is not possible.");
		}

		// Combine logical windows
		if (window1.getClass().equals(ElementLogicalWindow.class)) {
			ElementLogicalWindow w1 = (ElementLogicalWindow) window1;
			ElementLogicalWindow w2 = (ElementLogicalWindow) window2;

			// Range
			Node range1 = w1.getRangeNode();
			Node range2 = w2.getRangeNode();
			Node range;
			if (range1.equals(range2)) {
				range = range1;
			} else if (range1.isVariable() || range2.isVariable()) {
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
			if (step1.equals(step2)) {
				step = step1;
			} else if (step1.isVariable() || step2.isVariable()) {
				throw new QueryException("ERROR: Unable to combine windows accross variables.");
			} else {
				Duration a = Duration.parse(step1.getLiteralLexicalForm());
				Duration b = Duration.parse(step2.getLiteralLexicalForm());
				step = a.minus(b).isNegative() ? step1 : step2;
			}
			return new ElementLogicalWindow(window1.getWindowNameNode(), window1.getStreamNameNode(), range, step);
		}
		
		// Combine logical past windows
		if (window1.getClass().equals(ElementLogicalPastWindow.class)) {
			ElementLogicalPastWindow w1 = (ElementLogicalPastWindow) window1;
			ElementLogicalPastWindow w2 = (ElementLogicalPastWindow) window2;

			// From
			Node from1 = w1.getFromNode();
			Node from2 = w2.getFromNode();
			Node from;
			if (from1.equals(from2)) {
				from = from1;
			} else if (from1.isVariable() || from2.isVariable()) {
				throw new QueryException("ERROR: Unable to combine windows accross variables.");
			} else {
				Duration a = Duration.parse(from1.getLiteralLexicalForm());
				Duration b = Duration.parse(from2.getLiteralLexicalForm());
				from = a.minus(b).isNegative() ? from2 : from1;
			}
			
			// To
			Node to1 = w1.getToNode();
			Node to2 = w2.getToNode();
			Node to;
			if (to1.equals(to2)) {
				to = to1;
			} else if (to1.isVariable() || to2.isVariable()) {
				throw new QueryException("ERROR: Unable to combine windows accross variables.");
			} else {
				Duration a = Duration.parse(to1.getLiteralLexicalForm());
				Duration b = Duration.parse(to2.getLiteralLexicalForm());
				to = a.minus(b).isNegative() ? to1 : to2;
			}

			// Step
			Node step1 = w1.getStepNode();
			Node step2 = w2.getStepNode();
			Node step;
			if (step1.equals(step2)) {
				step = step1;
			} else if (step1.isVariable() || step2.isVariable()) {
				throw new QueryException("ERROR: Unable to combine windows accross variables.");
			} else {
				Duration a = Duration.parse(step1.getLiteralLexicalForm());
				Duration b = Duration.parse(step2.getLiteralLexicalForm());
				step = a.minus(b).isNegative() ? step1 : step2;
			}
			return new ElementLogicalPastWindow(window1.getWindowNameNode(), window1.getStreamNameNode(), from, to, step);
		}

		return null;
	}

	@Override
	public void visitSelectResultForm(Query query) {
		out.print("SELECT ");
		switch (query.getOutputStreamType()) {
		case Query.OutputStreamTypeRstream:
			out.print("RSTREAM ");
			break;
		case Query.OutputStreamTypeDstream:
			out.print("DSTREAM ");
			break;
		case Query.OutputStreamTypeIstream:
			out.print("ISTREAM ");
			break;
		}
		if (query.isDistinct())
			out.print("DISTINCT ");
		if (query.isReduced())
			out.print("REDUCED ");
		out.print(" "); // Padding

		if (query.isQueryResultStar())
			out.print("*");
		else
			appendNamedExprList(query, out, query.getProject());
		out.newline();
	}

	@Override
	public void visitConstructResultForm(Query query) {
		out.print("CONSTRUCT ");
		
		switch (query.getOutputStreamType()) {
		case Query.OutputStreamTypeRstream:
			out.print("RSTREAM ");
			break;
		case Query.OutputStreamTypeDstream:
			out.print("DSTREAM ");
			break;
		case Query.OutputStreamTypeIstream:
			out.print("ISTREAM ");
			break;
		}

		out.incIndent(BLOCK_INDENT);
		out.newline();

		Template t = query.getConstructTemplate();
		fmtTemplate.format(t);
		out.decIndent(BLOCK_INDENT);
		out.incIndent();
	}

	@Override
	public void visitOutputStreamDecl(Query query) {
		if (query.getOutputStream() == null)
			return;
		System.err.println("WARNING: SPARQLStream does not support naming of the output stream.");
	}

	@Override
	public void visitDescribeResultForm(Query query) {
		System.err.println("Error: SPARQLStream does not support DESCRIBE queries.");
	}

	@Override
	public void visitAskResultForm(Query query) {
		System.err.println("Error: SPARQLStream does not support ASK queries.");
	}
}
