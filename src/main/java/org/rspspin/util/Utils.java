package org.rspspin.util;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;
import org.rspspin.vocabulary.RSP;
import org.topbraid.spin.vocabulary.ARG;
import org.topbraid.spin.vocabulary.SP;
import org.topbraid.spin.vocabulary.SPIN;
import org.topbraid.spin.vocabulary.SPL;

public class Utils {

	public static Model createDefaultModel() {
		Model model = ModelFactory.createDefaultModel();
		model.setNsPrefix("sp", SP.NS);
		model.setNsPrefix("spl", SPL.NS);
		model.setNsPrefix("arg", ARG.NS);
		model.setNsPrefix("spin", SPIN.NS);
		model.setNsPrefix("rsp", RSP.NS);
		model.setNsPrefix("xsd", XSD.NS);
		model.setNsPrefix("rdf", RDF.getURI());
		model.setNsPrefix("rdfs", RDFS.getURI());
		return model;
	}

	/**
	 * Validate URI
	 * @param uriString
	 * @return
	 */
	public static boolean validateUri(String uriString) {
		try {
			new URI(uriString);
		} catch (URISyntaxException e) {
			return false;
		}
		return true;
	}
}
