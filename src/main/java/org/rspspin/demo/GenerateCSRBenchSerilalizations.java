package org.rspspin.demo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.rspspin.lang.cqels.ParserCQELS;
import org.rspspin.lang.csparql.ParserCSPARQL;
import org.rspspin.lang.rspql.ParserRSPQL;
import org.rspspin.lang.sparqlstream.ParserSPARQLStream;

/**
 * This class generates serializations for the CSRBench queries based on their
 * RSP-QL representations.
 * 
 * Disclaimer: This uses a different set of serializers than those used in the
 * papers. The new serializers are developed for Jena 3.0.1 and use a more
 * minimalistic approach to simplify maintenance.
 * 
 * @author Robin Keskisarkka
 *
 */
public class GenerateCSRBenchSerilalizations {
	public static void main(String[] args) throws IOException {
		ParserCQELS.register();
		ParserCSPARQL.register();
		ParserSPARQLStream.register();
		ParserRSPQL.register();

		FileWriter fw;

		for (int i = 1; i < 8; i++) {
			String path = String.format("./queries/csrbench/rspql/query%d.rspql", i);
			String queryString = new String(Files.readAllBytes(Paths.get(path)));

			Query query = QueryFactory.create(queryString, ParserRSPQL.syntax);

			// CQELS-QL
			query.setSyntax(ParserCQELS.syntax);
			path = String.format("./queries/csrbench/cqelsql/query%d.cqels", i);
			fw = new FileWriter(new File(path));
			fw.write(query.toString());
			fw.close();

			// CSPARQL
			query.setSyntax(ParserCSPARQL.syntax);
			path = String.format("./queries/csrbench/csparql/query%d.csparql", i);
			fw = new FileWriter(new File(path));
			fw.write(query.toString());
			fw.close();

			// SPARQLStream
			query.setSyntax(ParserSPARQLStream.syntax);
			path = String.format("./queries/csrbench/sparqlstream/query%d.sparqlstream", i);
			fw = new FileWriter(new File(path));
			fw.write(query.toString());
			fw.close();
		}
	}

}
