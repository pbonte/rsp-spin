package org.rspspin.lang;

public class RSPQLSyntax extends org.apache.jena.query.Syntax {
    public RSPQLSyntax(String lookupName, String uri) {
        super(uri);
        querySyntaxNames.put(lookupName, this);
    }
}

