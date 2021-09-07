package org.rspspin.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.jena.query.Query;
import org.json.JSONObject;
import org.rspspin.lang.cqels.ParserCQELS;
import org.rspspin.lang.csparql.ParserCSPARQL;
import org.rspspin.lang.rspql.ParserRSPQL;
import org.topbraid.spin.model.Template;

public class Demo {
	public static void main(String[] args) throws RSPSPINException, IOException{
		RSPSPINTemplateManager tm = new RSPSPINTemplateManager();
		
		String content = new String(Files.readAllBytes(Paths.get("templates/listAnpr.rspspin")));
		System.out.println(content.replaceAll("\n", "\\\\n"));
		JSONObject ob = new JSONObject(content.replaceAll("(#.*)\n", "$1\\\\n").replaceAll("\n"," "));
		Template t = tm.createTemplateFromJson(ob);
		
		
		Query q = tm.getQuery(t);

		q.setSyntax(ParserRSPQL.syntax);
		
		System.out.println(q);
		
		q.setSyntax(ParserCQELS.syntax);
		System.out.println(q);
		
		q.setSyntax(ParserCSPARQL.syntax);
		System.out.println(q);
	}
}
