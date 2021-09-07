package org.rspspin.demo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryException;
import org.apache.jena.query.QueryFactory;
import org.apache.own.query.RSPQLQueryFactory;
import org.rspspin.lang.cqels.ParserCQELS;
import org.rspspin.lang.csparql.ParserCSPARQL;
import org.rspspin.lang.rspql.ParserRSPQL;
import org.rspspin.lang.sparqlstream.ParserSPARQLStream;

/**
 * This class generates serializations for the RSP-QL sample queries based on
 * their RSP-QL representations.
 * 
 * Disclaimer: This uses a different set of serializers than those used in the
 * papers. The new serilaizers are developed for Jena 3.0.1 and use a more
 * minimalistic approach to simplify maintenance. Note also that the current
 * serializers do not have any strategy for combining windows of different
 * types, and no fallback for handling unsupported window types (i.e., more
 * strict than previous version). This means that no serializer works for
 * query 7 and query 8.
 * 
 * @author Robin Keskisarkka
 *
 */
public class GenerateRSPQLSampleSerilalizations {
	public static void main(String[] args) throws IOException {
		ParserCQELS.register();
		ParserCSPARQL.register();
		ParserSPARQLStream.register();
		ParserRSPQL.register();

		FileWriter fw;

		for (String file : new String[] {"query3","query1", "query2", "query3", "query4", "query5", "query6", "query7",
				"query8", "extra" }) {
			System.out.println("Parsing: " + file);
			System.err.println("Query: " + file);
			String path = String.format("./queries/sample-queries/rspql/%s.rspql", file);
			String queryString = new String(Files.readAllBytes(Paths.get(path)));
			Query query;
			try {
				query = RSPQLQueryFactory.create(queryString, ParserRSPQL.syntax);
			} catch (QueryException e) {
				System.err.println(e.getMessage());
				System.err.println("Skipping");
				continue;
			}

			// CQELS-QL
			query.setSyntax(ParserCQELS.syntax);
			path = String.format("./queries/sample-queries/cqelsql/%s.cqels", file);
			fw = new FileWriter(new File(path));
			try {
				fw.write(query.toString());
			} catch (QueryException e) {
				fw.write("Could not be serialized as CQELS");
			}
			fw.close();

			// CSPARQL
			query.setSyntax(ParserCSPARQL.syntax);
			path = String.format("./queries/sample-queries/csparql/%s.csparql", file);
			fw = new FileWriter(new File(path));
			try {
				fw.write(query.toString());
			} catch (QueryException e) {
				fw.write("Could not be serialized as CSPARQL");
			}
			fw.close();

			// SPARQLStream
			query.setSyntax(ParserSPARQLStream.syntax);
			path = String.format("./queries/sample-queries/sparqlstream/%s.sparqlstream", file);
			fw = new FileWriter(new File(path));
			try {
				fw.write(query.toString());
			} catch (QueryException e) {
				fw.write("Could not be serialized as SPARQLStream");
			}
			fw.close();
		}
	}

}
