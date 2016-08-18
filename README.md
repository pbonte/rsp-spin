## Coming soon: RSP-SPIN API 
# RSP-SPIN Modeling Vocabulary

This is the RSP-QL extension of the SPIN modeling vocabulary, which enables RSP-QL queries (see [link](https://github.com/streamreasoning/RSP-QL)) to be represented as RDF. In combination with the provided API queries can be converted to and from RDF, as well as support parameterization of queries into reusable query templates. 

Below is a sample query demonstrating how an RSP-QL query would be represented as RDF:
```
# Sample query 1 (https://github.com/streamreasoning/RSP-QL)
# Get the number of taxi rides that exceeded 2 miles in the last hour.
PREFIX  :     <http://debs2015.org/streams/>
PREFIX  debs: <http://debs2015.org/onto#>

REGISTER STREAM :rideCount AS

SELECT  (count(?ride) AS ?rideCount)
FROM NAMED WINDOW :wind ON :trips [RANGE PT1H STEP PT1H]
WHERE
  { WINDOW :win
      { ?ride debs:distance ?distance
        FILTER ( ?distance > 2 )
      }
  }
```
Using the RSP-SPIN modeling vocabulary it would be represented in RDF as:
```
@prefix :      <http://debs2015.org/streams/> .
@prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .
@prefix debs:  <http://debs2015.org/onto#> .
@prefix sp:    <http://spinrdf.org/sp#> .
@prefix rsp:   <http://w3id.org/rsp/spin#> .

[       a                    sp:Select ;
        rsp:registerAs       :rideCount ;
        rsp:fromNamedWindow  [ a               rsp:LogicalWindow ;
                               rsp:range       "PT1H"^^xsd:duration ;
                               rsp:logicalStep "PT1H"^^xsd:duration ;
                               rsp:streamIri   :trips ;
                               rsp:windowIri   :wind
                             ] ;
        sp:resultVariables   ( [ sp:expression  [ a              sp:Count ;
                                                  sp:expression  [ sp:varName  "ride"^^xsd:string ]
                                                ] ;
                                 sp:varName     "rideCount"^^xsd:string
                               ] ) ;
        sp:where             ( [ a              rsp:NamedWindow ;
                                 sp:elements    ( [ sp:object     [ sp:varName  "distance"^^xsd:string ] ;
                                                    sp:predicate  debs:distance ;
                                                    sp:subject    [ sp:varName  "ride"^^xsd:string ]
                                                  ] [ a              sp:Filter ;
                                                      sp:expression  [ a  sp:gt ;
                                                                          sp:arg1  [ sp:varName  "distance"^^xsd:string ] ;
                                                                          sp:arg2  2
                                                                      ]
                                                    ] ) ;
                                 rsp:windowNameNode  :win
                               ] ) .
```

Now, let's assume we wish to use this query to create a template that allows us to specify the distance to filter on as a query template parameter. We modify the query slightly, replacing ```FILTER ( ?distance > 2 )``` with ```FILTER ( ?distance > ?limit )``` and to make the template even more flexible let's assume that we also replace the name of the output stream (```:rideCount```) and input stream (```:trips```) with the variables ```?input``` and ```?output```. 
```
# Modifed query
# Get the number of taxi rides that exceeded a certain distance in miles in the last hour.
PREFIX  :     <http://debs2015.org/streams/>
PREFIX  debs: <http://debs2015.org/onto#>

REGISTER STREAM ?output AS

SELECT  (count(?ride) AS ?rideCount)
FROM NAMED WINDOW :wind ON ?input [RANGE PT1H STEP PT1H]
WHERE
  { WINDOW :win
      { ?ride debs:distance ?distance
        FILTER ( ?distance > ?limit )
      }
  }
```
We can now specify a template over the query, and we can instantiate it multiple time with different sets of bindings for the three parameters (```spl:Argument```):
```
@prefix :      <http://debs2015.org/streams/> .
@prefix spin:  <http://spinrdf.org/spin#> .
@prefix arg:   <http://spinrdf.org/arg#> .
@prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .
@prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> .
@prefix sp:    <http://spinrdf.org/sp#> .
@prefix spl:   <http://spinrdf.org/spl#> .

:t1   a                spin:Template ;
      spin:body        :q1 ;
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
                           spl:predicate  arg:input ;
                           spl:valueType  rdfs:Resource
                         ] ;
        spin:constraint  [ a              spl:Argument ;
                           rdfs:comment   "Represents the URI identifier of the resulting stream." ;
                           spl:optional   false ;
                           spl:predicate  arg:output ;
                           spl:valueType  rdfs:Resource
                         ] .
```


See <http://spinrdf.org/spin.html> for details about the SPIN Modeling Vocabulary.

