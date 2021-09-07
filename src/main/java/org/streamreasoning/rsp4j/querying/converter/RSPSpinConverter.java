package org.streamreasoning.rsp4j.querying.converter;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryException;
import org.apache.own.query.RSPQLQueryFactory;
import org.rspspin.lang.RSPQLSyntax;
import org.rspspin.lang.cqels.ParserCQELS;
import org.rspspin.lang.csparql.ParserCSPARQL;
import org.rspspin.lang.rspql.ParserRSPQL;
import org.rspspin.lang.sparqlstream.ParserSPARQLStream;
import org.streamreasoning.rsp4j.api.querying.syntax.rspdialects.RSPDialect;
import org.streamreasoning.rsp4j.api.querying.syntax.rspdialects.RSPDialectConverter;


import java.util.Map;

public class RSPSpinConverter implements RSPDialectConverter {
    private final Map<RSPDialect, RSPQLSyntax> syntaxConverter;

    public RSPSpinConverter(){
        ParserCQELS.register();
        ParserCSPARQL.register();
        ParserSPARQLStream.register();
        ParserRSPQL.register();
        syntaxConverter = Map.of(RSPDialect.CSPARQL,ParserCSPARQL.syntax, RSPDialect.CQELS, ParserCQELS.syntax, RSPDialect.MORPHSTREAM, ParserSPARQLStream.syntax);

    }
    @Override
    public String convertToDialectFromRSPQLSyntax(String rspqlQuery, RSPDialect dialectRSPDialect) {
        Query query;
        try {
            query = (Query) RSPQLQueryFactory.create(rspqlQuery, ParserRSPQL.syntax);
            query.setSyntax(syntaxConverter.get(dialectRSPDialect));
            return query.toString();
        } catch (QueryException e) {
            System.err.println(e.getMessage());
            System.err.println("Unable to convert query");
        }
        return null;
    }
}
