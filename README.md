# RSP-SPIN Modeling Vocabulary

This is the RSP-QL extension of the SPIN modeling vocabulary, which enables RSP-QL queries (see [link](https://github.com/streamreasoning/RSP-QL)) to be represented as RDF. In combination with the provided API queries can be converted to and from RDF, and queries can be parameterized into reusable query templates that support parameter constraints. 

Below is a sample query demonstrating how an RSP-QL query would be represented as RDF:
```
# Based on sample query 1 (https://github.com/streamreasoning/RSP-QL)
# Get the number of taxi rides that exceeded ?limit miles in the last hour.
PREFIX  :     <http://debs2015.org/streams/>
PREFIX  onto: <http://debs2015.org/onto#>

REGISTER STREAM ?outputStream AS

SELECT ISTREAM  (COUNT(?ride) AS ?rideCount)
FROM NAMED WINDOW :wind ON ?inputStream [RANGE PT1H STEP PT1H]
WHERE
  { WINDOW :win
      { ?ride  onto:distance  ?distance
        FILTER ( ?distance > ?limit )
      }
  }
```
Using the RSP-SPIN modeling vocabulary it would be represented in RDF as:
```
@prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .
@prefix sp:    <http://spinrdf.org/sp#> .
@prefix rsp:   <http://w3id.org/rsp/spin#> .

@prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .
@prefix sp:    <http://spinrdf.org/sp#> .
@prefix rsp:   <http://w3id.org/rsp/spin#> .

<http://example.org/query/1>
  a                            sp:Select ;
  rsp:hasOutputStream          [ sp:varName "outputStream" ] ;
  rsp:hasOutputStreamOperator  rsp:Istream
  sp:resultVariables           ( _:b0 ) ;
  rsp:fromNamedWindow          [ a                 rsp:LogicalWindow ;
                                 rsp:logicalRange  "PT1H"^^xsd:duration ;
                                 rsp:logicalStep   "PT1H"^^xsd:duration ;
                                 rsp:streamUri     [ sp:varName  "inputStream" ] ;
                                 rsp:windowUri     <http://debs2015.org/streams/wind>
                               ] ;
  sp:where                     ( _:b1 )
.

_:b0    sp:expression  [ a              sp:Count ;
                         sp:expression  [ sp:varName  "ride" ]
                       ] ;
        sp:varName     "rideCount" .

_:b1    a                   rsp:NamedWindow ;
        sp:elements         ( _:b3 _:b2 ) ;
        rsp:windowNameNode  <http://debs2015.org/streams/win> .

_:b2    a              sp:Filter ;
        sp:expression  [ a        sp:gt ;
                         sp:arg1  [ sp:varName  "distance" ] ;
                         sp:arg2  [ sp:varName  "limit" ]
                       ] .

_:b3    sp:subject    [ sp:varName  "ride" ] ;
        sp:predicate  <http://debs2015.org/onto#distance> ;
        sp:object     [ sp:varName  "distance" ] .
```

Now, let's use define a template over this query allowing us to specify the ```limit``` variable of the query as an integer with a default value, and require that the ```inputStream``` and ```outputStream``` are provided as URIs.
```
@prefix :      <http://debs2015.org/streams/> .
@prefix spin:  <http://spinrdf.org/spin#> .
@prefix arg:   <http://spinrdf.org/arg#> .
@prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .
@prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> .
@prefix sp:    <http://spinrdf.org/sp#> .
@prefix spl:   <http://spinrdf.org/spl#> .

:t1   a                spin:Template ;
      spin:body        <http://example.org/query/1> ;
      spin:constraint  [ a                 spl:Argument ;
                         rdfs:comment      "Get the taxi rides that exceeded this limit in the last hour." ;
                         spl:defaultValue  2 ;
                         spl:optional      true ;
                         spl:predicate     arg:limit ;
                         spl:valueType     xsd:integer
                       ] ;
      spin:constraint  [ a              spl:Argument ;
                         rdfs:comment   "Represents the URI identifier of the input stream." ;
                         spl:optional   false ;
                         spl:predicate  arg:inputStream ;
                         spl:valueType  rdfs:Resource
                       ] ;
      spin:constraint  [ a              spl:Argument ;
                         rdfs:comment   "Represents the URI identifier of the resulting stream." ;
                         spl:optional   false ;
                         spl:predicate  arg:outputStream ;
                         spl:valueType  rdfs:Resource
                       ] .
```


See <http://spinrdf.org/spin.html> for details about the SPIN Modeling Vocabulary.
