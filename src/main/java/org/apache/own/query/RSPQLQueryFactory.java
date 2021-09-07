package org.apache.own.query;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.Syntax;

public class RSPQLQueryFactory extends QueryFactory {


    static public RSPQLQuery create(String queryString, Syntax syntax)
    {
        return create(queryString, null, syntax) ;
    }
    static public RSPQLQuery create(String queryString, String baseURI, Syntax syntax)
    {
        Query query = new RSPQLQuery();
        return (RSPQLQuery) parse(query, queryString, baseURI, syntax);
    }
}
