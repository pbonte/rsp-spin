package org.streamreasoning.rsp4j.querying.converter;

import org.junit.Test;
import org.streamreasoning.rsp4j.api.querying.syntax.rspdialects.RSPDialect;
import org.streamreasoning.rsp4j.api.querying.syntax.rspdialects.RSPDialectConverter;


import static org.junit.Assert.assertEquals;

public class RSPQLToDialectTest {
    String rspQLQuery= "PREFIX  :     <http://debs2015.org/streams/>\n" +
            "PREFIX  debs: <http://debs2015.org/onto#>\n" +
            "\n" +
            "REGISTER STREAM :stream1 AS\n" +
            "\n" +
            "SELECT (count(?ride) AS ?rideCount)\n" +
            "FROM NAMED WINDOW :win ON :trips [RANGE PT1H STEP PT1H]\n" +
            "WHERE\n" +
            "  { WINDOW :win\n" +
            "      { ?ride debs:distance ?distance\n" +
            "        FILTER ( ?distance > 2 )\n" +
            "      }\n" +
            "  }";
    @Test
    public void testToCSPARQL(){
        String expectedCSPARQLQuery = "REGISTER QUERY stream1 AS\n" +
                "\n" +
                "PREFIX  :     <http://debs2015.org/streams/>\n" +
                "PREFIX  debs: <http://debs2015.org/onto#>\n" +
                "\n" +
                "SELECT  (COUNT(?ride) AS ?rideCount)\n" +
                "FROM STREAM <http://debs2015.org/streams/trips> [RANGE 1h STEP 1h]\n" +
                "WHERE\n" +
                "  { \n" +
                "    ?ride  debs:distance  ?distance .\n" +
                "    FILTER ( ?distance > 2 ) .\n" +
                "  }\n";
        RSPDialectConverter rspDialectConverter = new RSPSpinConverter();
        String convertedToCSPARQL = rspDialectConverter.convertToDialectFromRSPQLSyntax(rspQLQuery, RSPDialect.CSPARQL);
        assertEquals(convertedToCSPARQL,expectedCSPARQLQuery);
    }
    @Test
    public void testToCQELS(){
        String expectedCQELSQuery = "PREFIX  :     <http://debs2015.org/streams/>\n" +
                "PREFIX  debs: <http://debs2015.org/onto#>\n" +
                "\n" +
                "SELECT  (COUNT(?ride) AS ?rideCount)\n" +
                "WHERE\n" +
                "  { STREAM <http://debs2015.org/streams/trips> [RANGE 1h SLIDE 1h] {\n" +
                "      ?ride  debs:distance  ?distance .\n" +
                "      FILTER ( ?distance > 2 ) .\n" +
                "    }\n" +
                "  }\n";
        RSPDialectConverter rspDialectConverter = new RSPSpinConverter();
        String convertedToCQELS = rspDialectConverter.convertToDialectFromRSPQLSyntax(rspQLQuery, RSPDialect.CQELS);
        assertEquals(convertedToCQELS,expectedCQELSQuery);
    }
    @Test
    public void testToMorph(){
        String expectedMorphStreamSQuery = "PREFIX  :     <http://debs2015.org/streams/>\n" +
                "PREFIX  debs: <http://debs2015.org/onto#>\n" +
                "\n" +
                "SELECT  (COUNT(?ride) AS ?rideCount)\n" +
                "FROM STREAM <http://debs2015.org/streams/trips> [NOW-1 HOUR SLIDE 1 HOUR]\n" +
                "WHERE\n" +
                "  { \n" +
                "    ?ride  debs:distance  ?distance .\n" +
                "    FILTER ( ?distance > 2 ) .\n" +
                "  }\n";
        RSPDialectConverter rspDialectConverter = new RSPSpinConverter();
        String convertedToMorphStream = rspDialectConverter.convertToDialectFromRSPQLSyntax(rspQLQuery, RSPDialect.MORPHSTREAM);
        assertEquals(convertedToMorphStream,expectedMorphStreamSQuery);
    }
}
