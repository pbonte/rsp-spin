package org.rspspin.tests;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.junit.Test;
import org.rspspin.lang.rspql.ParserRSPQL;
import org.topbraid.spin.arq.ARQ2SPIN;
import org.topbraid.spin.arq.ARQFactory;
import org.topbraid.spin.system.SPINModuleRegistry;
import org.topbraid.spin.util.JenaUtil;
import static org.junit.Assert.*;

public class TestRSPSPIN {

	/* Literals and URIs */
	@Test
	public void selectLogicalWindow1() {
		SPINModuleRegistry.get().init();
		ParserRSPQL.register();
		ARQFactory.setSyntax(ParserRSPQL.syntax);
		String q = "REGISTER STREAM <http://output> AS\n" 
				+ "SELECT *\n"
				+ "FROM NAMED WINDOW <http://window> ON <http://stream> [RANGE PT1H STEP PT30M]\n" 
				+ "WHERE {\n"
				+ "   WINDOW <http://window> { ?s ?p ?o . } .\n" 
				+ "}";
		Query arqQuery = QueryFactory.create(q, ParserRSPQL.syntax);
		Model model = JenaUtil.createDefaultModel();
		org.topbraid.spin.model.Query spinQuery = new ARQ2SPIN(model).createQuery(arqQuery, "");
		assertEquals(compress(q), compress(spinQuery.toString()));
	}

	/* Variables */
	@Test
	public void selectLogicalWindow2() {
		SPINModuleRegistry.get().init();
		ParserRSPQL.register();
		ARQFactory.setSyntax(ParserRSPQL.syntax);
		String q = ""
				+ "REGISTER STREAM ?output AS\n" 
				+ "SELECT *\n"
				+ "FROM NAMED WINDOW ?w ON ?s [RANGE ?range STEP ?step]\n" 
				+ "WHERE {\n"
				+ "   WINDOW ?w { ?s ?p ?o . } .\n" 
				+ "}";
		Query arqQuery = QueryFactory.create(q, ParserRSPQL.syntax);
		Model model = JenaUtil.createDefaultModel();
		org.topbraid.spin.model.Query spinQuery = new ARQ2SPIN(model).createQuery(arqQuery, "");
		assertEquals(compress(q), compress(spinQuery.toString()));
	}

	/* No step */
	@Test
	public void selectLogicalWindow3() {
		SPINModuleRegistry.get().init();
		ParserRSPQL.register();
		ARQFactory.setSyntax(ParserRSPQL.syntax);
		String q = ""
				+ "REGISTER STREAM ?output AS\n" 
				+ "SELECT *\n" 
				+ "FROM NAMED WINDOW ?w ON ?s [RANGE ?range]\n"
				+ "WHERE {\n" 
				+ "   WINDOW ?w { ?s ?p ?o . } .\n" 
				+ "}";
		Query arqQuery = QueryFactory.create(q, ParserRSPQL.syntax);
		Model model = JenaUtil.createDefaultModel();
		org.topbraid.spin.model.Query spinQuery = new ARQ2SPIN(model).createQuery(arqQuery, "");
		assertEquals(compress(q), compress(spinQuery.toString()));
	}

	/* Literals and URIs */
	@Test
	public void constructLogicalWindow1() {
		SPINModuleRegistry.get().init();
		ParserRSPQL.register();
		ARQFactory.setSyntax(ParserRSPQL.syntax);
		String q = ""
				+ "REGISTER STREAM <http://output> AS\n" 
				+ "CONSTRUCT { ?s ?p ?o . }\n"
				+ "FROM NAMED WINDOW <http://window> ON <http://stream> [RANGE PT1H STEP PT30M]\n" 
				+ "WHERE {\n"
				+ "   WINDOW <http://window> { ?s ?p ?o . } .\n" 
				+ "}";
		Query arqQuery = QueryFactory.create(q, ParserRSPQL.syntax);
		Model model = JenaUtil.createDefaultModel();
		org.topbraid.spin.model.Query spinQuery = new ARQ2SPIN(model).createQuery(arqQuery, "");
		assertEquals(compress(q), compress(spinQuery.toString()));
	}

	/* Variables */
	@Test
	public void constructLogicalWindow2() {
		SPINModuleRegistry.get().init();
		ParserRSPQL.register();
		ARQFactory.setSyntax(ParserRSPQL.syntax);
		String q = ""
				+ "REGISTER STREAM ?output AS\n" 
				+ "CONSTRUCT { ?s ?p ?o . }\n"
				+ "FROM NAMED WINDOW ?w ON ?s [RANGE ?range STEP ?step]\n" 
				+ "WHERE {\n"
				+ "   WINDOW ?w { ?s ?p ?o . } .\n" 
				+ "}";
		Query arqQuery = QueryFactory.create(q, ParserRSPQL.syntax);
		Model model = JenaUtil.createDefaultModel();
		org.topbraid.spin.model.Query spinQuery = new ARQ2SPIN(model).createQuery(arqQuery, "");
		assertEquals(compress(q), compress(spinQuery.toString()));
	}

	/* No step */
	@Test
	public void constructLogicalWindow3() {
		SPINModuleRegistry.get().init();
		ParserRSPQL.register();
		ARQFactory.setSyntax(ParserRSPQL.syntax);
		String q = ""
				+ "REGISTER STREAM <http://output> AS\n" 
				+ "CONSTRUCT { ?s ?p ?o . }\n"
				+ "FROM NAMED WINDOW <http://window> ON <http://stream> [RANGE PT1H]\n" 
				+ "WHERE {\n"
				+ "   WINDOW <http://window> { ?s ?p ?o . } .\n" 
				+ "}";
		Query arqQuery = QueryFactory.create(q, ParserRSPQL.syntax);
		Model model = JenaUtil.createDefaultModel();
		org.topbraid.spin.model.Query spinQuery = new ARQ2SPIN(model).createQuery(arqQuery, "");
		assertEquals(compress(q), compress(spinQuery.toString()));
	}

	/* Literals and URIs */
	@Test
	public void logicalPastWindow1() {
		SPINModuleRegistry.get().init();
		ParserRSPQL.register();
		ARQFactory.setSyntax(ParserRSPQL.syntax);
		String q = "REGISTER STREAM <http://output> AS\n" 
				+ "SELECT *\n"
				+ "FROM NAMED WINDOW <http://window> ON <http://stream> [FROM NOW-PT1H TO NOW-PT30M STEP PT10M]\n" 
				+ "WHERE {\n"
				+ "   WINDOW <http://window> { ?s ?p ?o . } .\n" 
				+ "}";
		Query arqQuery = QueryFactory.create(q, ParserRSPQL.syntax);
		Model model = JenaUtil.createDefaultModel();
		org.topbraid.spin.model.Query spinQuery = new ARQ2SPIN(model).createQuery(arqQuery, "");
		assertEquals(compress(q), compress(spinQuery.toString()));
	}

	/* Variables */
	@Test
	public void logicalPastWindow2() {
		SPINModuleRegistry.get().init();
		ParserRSPQL.register();
		ARQFactory.setSyntax(ParserRSPQL.syntax);
		String q = ""
				+ "REGISTER STREAM ?output AS\n" 
				+ "SELECT *\n"
				+ "FROM NAMED WINDOW ?w ON ?s [FROM NOW-?from TO NOW-?to STEP ?step]\n" 
				+ "WHERE {\n"
				+ "   WINDOW ?w { ?s ?p ?o . } .\n" 
				+ "}";
		Query arqQuery = QueryFactory.create(q, ParserRSPQL.syntax);
		Model model = JenaUtil.createDefaultModel();
		org.topbraid.spin.model.Query spinQuery = new ARQ2SPIN(model).createQuery(arqQuery, "");
		assertEquals(compress(q), compress(spinQuery.toString()));
	}

	/* No step */
	@Test
	public void logicalPastWindow3() {
		SPINModuleRegistry.get().init();
		ParserRSPQL.register();
		ARQFactory.setSyntax(ParserRSPQL.syntax);
		String q = ""
				+ "REGISTER STREAM ?output AS\n" 
				+ "SELECT *\n" 
				+ "FROM NAMED WINDOW ?w ON ?s [FROM NOW-?from TO NOW-?to]\n"
				+ "WHERE {\n" 
				+ "   WINDOW ?w { ?s ?p ?o . } .\n" 
				+ "}";
		Query arqQuery = QueryFactory.create(q, ParserRSPQL.syntax);
		Model model = JenaUtil.createDefaultModel();
		org.topbraid.spin.model.Query spinQuery = new ARQ2SPIN(model).createQuery(arqQuery, "");
		assertEquals(compress(q), compress(spinQuery.toString()));
	}
	
	/* Named graph in window clause */
	@Test
	public void graphInWindowClause(){
		SPINModuleRegistry.get().init();
		ParserRSPQL.register();
		ARQFactory.setSyntax(ParserRSPQL.syntax);
		String q = ""
				+ "REGISTER STREAM <http://output> AS\n"
				+ "CONSTRUCT { ?s ?p ?o . }\n"
				+ "FROM NAMED WINDOW <http://window> ON <http://stream> [RANGE PT1H]\n" 
				+ "WHERE {\n"
				+ "   WINDOW <http://window> { GRAPH ?g { ?s ?p ?o . } . } .\n" 
				+ "}";
		Query arqQuery = QueryFactory.create(q, ParserRSPQL.syntax);
		Model model = JenaUtil.createDefaultModel();
		org.topbraid.spin.model.Query spinQuery = new ARQ2SPIN(model).createQuery(arqQuery, "");		
		assertEquals(compress(q), compress(spinQuery.toString()));
	}
	
	/* Named graph in construct result */
	@Test
	public void graphInConstructResult(){
		SPINModuleRegistry.get().init();
		ParserRSPQL.register();
		ARQFactory.setSyntax(ParserRSPQL.syntax);
		String q = ""
				+ "REGISTER STREAM <http://output> AS\n"
				+ "CONSTRUCT { \n"
				+ "   GRAPH <http://g> {\n"
				+ "      ?s ?p ?o .\n"
				+ "   } .\n"
				+ "   ?s ?p ?o .\n"
				+ "}\n"
				+ "WHERE {\n"
				+ "}";
		Query arqQuery = QueryFactory.create(q, ParserRSPQL.syntax);
		Model model = JenaUtil.createDefaultModel();
		org.topbraid.spin.model.Query spinQuery = new ARQ2SPIN(model).createQuery(arqQuery, "");
		assertEquals(compress(q), compress(spinQuery.toString()));
	}
	
	/* Istream construct */
	@Test
	public void istreamConstruct(){
		SPINModuleRegistry.get().init();
		ParserRSPQL.register();
		ARQFactory.setSyntax(ParserRSPQL.syntax);
		String q = ""
				+ "REGISTER STREAM <http://output> AS\n"
				+ "CONSTRUCT ISTREAM {\n"
				+ "   ?s ?p ?o .\n"
				+ "}\n"
				+ "WHERE { }";
		Query arqQuery = QueryFactory.create(q, ParserRSPQL.syntax);
		Model model = JenaUtil.createDefaultModel();
		org.topbraid.spin.model.Query spinQuery = new ARQ2SPIN(model).createQuery(arqQuery, "");
		assertEquals(compress(q), compress(spinQuery.toString()));
	}
	
	/* Dstream construct */
	@Test
	public void dstreamConstruct(){
		SPINModuleRegistry.get().init();
		ParserRSPQL.register();
		ARQFactory.setSyntax(ParserRSPQL.syntax);
		String q = ""
				+ "REGISTER STREAM <http://output> AS\n"
				+ "CONSTRUCT DSTREAM {\n"
				+ "   ?s ?p ?o .\n"
				+ "}\n"
				+ "WHERE { }";
		Query arqQuery = QueryFactory.create(q, ParserRSPQL.syntax);
		Model model = JenaUtil.createDefaultModel();
		org.topbraid.spin.model.Query spinQuery = new ARQ2SPIN(model).createQuery(arqQuery, "");
		assertEquals(compress(q), compress(spinQuery.toString()));
	}
	
	/* Rstream construct */
	@Test
	public void rstreamConstruct(){
		SPINModuleRegistry.get().init();
		ParserRSPQL.register();
		ARQFactory.setSyntax(ParserRSPQL.syntax);
		String q = ""
				+ "REGISTER STREAM <http://output> AS\n"
				+ "CONSTRUCT RSTREAM {\n"
				+ "   ?s ?p ?o .\n"
				+ "}\n"
				+ "WHERE { }";
		Query arqQuery = QueryFactory.create(q, ParserRSPQL.syntax);
		Model model = JenaUtil.createDefaultModel();
		org.topbraid.spin.model.Query spinQuery = new ARQ2SPIN(model).createQuery(arqQuery, "");
		assertEquals(compress(q), compress(spinQuery.toString()));
	}
	
	/* Istream select */
	@Test
	public void istreamSelect(){
		SPINModuleRegistry.get().init();
		ParserRSPQL.register();
		ARQFactory.setSyntax(ParserRSPQL.syntax);
		String q = ""
				+ "REGISTER STREAM <http://output> AS\n"
				+ "SELECT ISTREAM ?s ?p ?o\n"
				+ "WHERE { }";
		Query arqQuery = QueryFactory.create(q, ParserRSPQL.syntax);
		Model model = JenaUtil.createDefaultModel();
		org.topbraid.spin.model.Query spinQuery = new ARQ2SPIN(model).createQuery(arqQuery, "");
		assertEquals(compress(q), compress(spinQuery.toString()));
	}
	
	/* Dstream select */
	@Test
	public void dstreamSelect(){
		SPINModuleRegistry.get().init();
		ParserRSPQL.register();
		ARQFactory.setSyntax(ParserRSPQL.syntax);
		String q = ""
				+ "REGISTER STREAM <http://output> AS\n"
				+ "SELECT DSTREAM ?s ?p ?o\n"
				+ "WHERE { }";
		Query arqQuery = QueryFactory.create(q, ParserRSPQL.syntax);
		Model model = JenaUtil.createDefaultModel();
		org.topbraid.spin.model.Query spinQuery = new ARQ2SPIN(model).createQuery(arqQuery, "");
		assertEquals(compress(q), compress(spinQuery.toString()));
	}
	
	/* Rstream select */
	@Test
	public void rstreamSelect(){
		SPINModuleRegistry.get().init();
		ParserRSPQL.register();
		ARQFactory.setSyntax(ParserRSPQL.syntax);
		String q = ""
				+ "REGISTER STREAM <http://output> AS\n"
				+ "SELECT RSTREAM ?s ?p ?o\n"
				+ "WHERE { }";
		Query arqQuery = QueryFactory.create(q, ParserRSPQL.syntax);
		Model model = JenaUtil.createDefaultModel();
		org.topbraid.spin.model.Query spinQuery = new ARQ2SPIN(model).createQuery(arqQuery, "");
		assertEquals(compress(q), compress(spinQuery.toString()));
	}
	
	public String compress(String s) {
		return s.replaceAll("\\s+", " ").trim();
	}
}
