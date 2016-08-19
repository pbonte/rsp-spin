package org.rspql.tests.csrbench;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;
import org.rspql.lang.cqels.ParserCQELS;
import org.rspql.lang.csparql.ParserCSPARQL;
import org.rspql.lang.rspql.ParserRSPQL;
import org.rspql.lang.sparqlstream.ParserSPARQLStream;
import org.topbraid.spin.system.SPINModuleRegistry;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;

public class TestSerializationCSPARQL {

	//@Test
	public void serializeQuery7() throws IOException{
		SPINModuleRegistry.get().init();
		ParserRSPQL.register();
		String csparqlPath = "./queries/csrbench/csparql/query7.csparql";
		String rspqlPath = "./queries/csrbench/rspql/query7.rspql";
		Query query = parse(new String(Files.readAllBytes(Paths.get(rspqlPath))));
		
		// Change the syntax
		query.setSyntax(ParserCSPARQL.csparqlSyntax);
		String parsed = query.toString();
		
		String original = new String(Files.readAllBytes(Paths.get(csparqlPath)));
		
		System.out.println(parsed);
		System.out.println(original);
	
//	// CQELS
//			reparsedQuery.setSyntax(ParserCQELS.cqelsSyntax);
//			queryString = reparsedQuery.toString();
//			if (print) {
//				System.out.println("CQELS-QL:");
//				System.out.println(queryString);
//			}
			
			// CSPARQL
//			reparsedQuery.setSyntax(ParserCSPARQL.csparqlSyntax);
//			queryString = reparsedQuery.toString();
//			if (print) {
//				System.out.println("C-SPARQL:");
//				System.out.println(queryString);
//			}

//			// SAPRQLStream
//			reparsedQuery.setSyntax(ParserSPARQLStream.sparqlStreamSyntax);
//			queryString = reparsedQuery.toString();
//			if (print) {
//				System.out.println("SPARQLStream:");
//				System.out.println(queryString);
//			}
}
	
	public static Query parse(String queryString){
		Query query = QueryFactory.create(queryString, ParserRSPQL.rspqlSyntax);
		return query;
	}
}
