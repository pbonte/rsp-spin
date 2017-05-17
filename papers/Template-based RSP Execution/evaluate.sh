#!/bin/bash
function listTemplates {
    curl http://localhost:8080/TemplateBasedRSP/templates
    echo ''
}

function addTemplate {
    curl -s -H "Content-Type: application/json" \
         -X POST \
         -d "$1" \
         http://localhost:8080/TemplateBasedRSP/templates
}

function deleteTemplate {
    curl -s -H "Content-Type: application/json" \
         -X DELETE \
         "http://localhost:8080/TemplateBasedRSP/templates/$1"
}

function getQuery {
    curl -s \
         -H "Content-Type: application/json" \
         -H "Accept: text/plain" \
         -X POST \
         -d "$2" \
         "http://localhost:8080/TemplateBasedRSP/templates/$1/query"
}

function registerQuery {
    curl -s -H "Content-Type: application/json" \
         -X POST \
         -d "$2" \
         "http://localhost:8080/TemplateBasedRSP/templates/$1/register"
}

# Extra
function setupAnpr {
    curl -s \
         -H 'Content-Type: application/json' \
         -X POST \
         -d "{ 'url' : 'http://localhost:8080/AnprStream/anpr' }" \
         "http://localhost:8080/CqelsService/cqels/streams/register"
    curl -s \
         -H 'Content-Type: application/json' \
         -X POST \
         -d "{ 'url' : 'http://localhost:8080/AnprStream/anpr' }" \
         "http://localhost:8080/CsparqlService/csparql/streams/register"
}
function registerCqelsQuery {
    curl -s \
       -H 'Content-Type: application/json' \
       -X POST \
       -d "$1" \
       "http://localhost:8080/CqelsService/cqels/queries/register"
}

function subscribeToCqels {
    curl -s  -H 'Content-Type: application/json' \
         -X POST \
         -d "$1" \
         "http://localhost:8080/CqelsService/cqels/subscribe"
}

# Admin services typically not exposed to users
function startAnpr {
    curl -s http://localhost:8080/AnprStream/anpr/start
}

function stopAnpr {
    curl -s http://localhost:8080/AnprStream/anpr/stop
}


# Track vehicle. 5 parameters (4 optional)
function scenario1_ {
  echo
  echo 'Scenario 1'
  template=$(tr '\n' ' ' < template1.txt)
  del=$(deleteTemplate "trackVehicle")
  add=$(addTemplate "$template")
  sleep 1
  echo
  echo "Instantiate query:"
  time query=$(getQuery "trackVehicle" "{ 'url' : 'http://ns.valcri.org/streams/track/LN43EXS' }', 'regno' : 'LN43EXS' }")
  echo "${query}"
  echo
  sleep 1
  echo 'Register query'
  time URL=$(registerQuery "trackVehicle" "{ 'url' : 'http://ns.valcri.org/streams/track/LN43EXS', 'regno' : 'LN43EXS' }")
  echo "Query results available at $URL"
  echo
  #subscribeToCqels "$URL"
}

# 1 param
function scenario0 {
  echo
  echo 'Scenario 0'
  template=$(tr '\n' ' ' < ./paper_scenarios/scenario0.txt)
  del=$(deleteTemplate "all")
  add=$(addTemplate "$template")
  #echo $add
  echo
  echo "Instantiate query:"
  time query=$(getQuery "all" "{ 'out' : 'http://ns.valcri.org/streams/anpr' }")
  #echo "${query}"
  echo
  sleep 1
  echo 'Register query'
  time URL=$(registerQuery "all" "{ 'out' : 'http://ns.valcri.org/streams/anpr' }")
  echo "Query results available at $URL"
  echo
  #subscribeToCqels "$URL"
}
# 2 param
function scenario1 {
  echo
  echo 'Scenario 1'
  template=$(tr '\n' ' ' < ./paper_scenarios/scenario1.txt)
  del=$(deleteTemplate "firehose")
  add=$(addTemplate "$template")
  #echo $add
  echo
  echo "Instantiate query:"
  time query=$(getQuery "firehose" "{
    'out' : 'http://ns.valcri.org/streams/anpr1', 
    'stream' : 'http://localhost:8080/AnprStream/anpr' }")
  #echo "${query}"
  echo
  sleep 1
  echo 'Register query'
  time URL=$(registerQuery "firehose" "{
    'out' : 'http://ns.valcri.org/streams/anpr1', 
    'stream' : 'http://localhost:8080/AnprStream/anpr' }")
  echo "Query results available at $URL"
  echo
  #subscribeToCqels "$URL"
}
# 3 param
function scenario2 {
  echo
  echo 'Scenario 2'
  template=$(tr '\n' ' ' < ./paper_scenarios/scenario2.txt)
  del=$(deleteTemplate "trackVehicle")
  add=$(addTemplate "$template")
  #echo $add

  echo "Instantiate query:"
  time query=$(getQuery "trackVehicle" "{
    'out' : 'http://ns.valcri.org/streams/anpr2', 
    'stream' : 'http://localhost:8080/AnprStream/anpr',
    'regnoFilter' : 'LN43EXS' }")
  #echo "${query}"
  echo
  sleep 1
  echo 'Register query'
  time URL=$(registerQuery "trackVehicle" "{
    'out' : 'http://ns.valcri.org/streams/anpr2', 
    'stream' : 'http://localhost:8080/AnprStream/anpr',
    'regnoFilter' : 'LN43EXS' }")
  echo "Query results available at $URL"
  echo
  #subscribeToCqels "$URL"
}
# 4 param
function scenario3 {
  echo
  echo 'Scenario 3'
  template=$(tr '\n' ' ' < ./paper_scenarios/scenario3.txt)
  del=$(deleteTemplate "trackNominal")
  add=$(addTemplate "$template")
  #echo $add

  echo "Instantiate query:"
  time query=$(getQuery "trackNominal" "{
    'out' : 'http://ns.valcri.org/streams/anpr3', 
    'stream' : 'http://localhost:8080/AnprStream/anpr',
    'nominal' : 'http://ns.valcri.org/data/nominals#nominal1234',
    'nominalVehiclesGraph' : 'http://localhost:8080/rest-spin/templates/get_triples/execute?limit=100000' }")
  #echo "${query}"
  echo
  sleep 1
  echo 'Register query'
  time URL=$(registerQuery "trackNominal" "{
    'out' : 'http://ns.valcri.org/streams/anpr3', 
    'stream' : 'http://localhost:8080/AnprStream/anpr',
    'nominal' : 'http://ns.valcri.org/data/nominals#nominal1234',
    'nominalVehiclesGraph' : 'http://localhost:8080/rest-spin/templates/get_triples/execute?limit=100000' }")
  echo "Query results available at $URL"
  echo
  #subscribeToCqels "$URL"
}
# 5 param
function scenario4 {
  echo
  echo 'Scenario 4'
  template=$(tr '\n' ' ' < ./paper_scenarios/scenario4.txt)
  del=$(deleteTemplate "trackCriminalNetwork")
  add=$(addTemplate "$template")
  #echo $add

  echo "Instantiate query:"
  time query=$(getQuery "trackCriminalNetwork" "{
    'out' : 'http://ns.valcri.org/streams/anpr4', 
    'stream' : 'http://localhost:8080/AnprStream/anpr',
    'nominalTarget' : 'http://ns.valcri.org/data/nominals#nominal1234',
    'nominalVehiclesGraph' : 'http://localhost:8080/rest-spin/templates/get_triples/execute?limit=100000',
    'criminalNetworksGraph' : 'http://localhost:8080/rest-spin/templates/get_triples/execute?limit=100001' }")
  #echo "${query}"
  echo
  sleep 1
  echo 'Register query'
  time URL=$(registerQuery "trackCriminalNetwork" "{
    'out' : 'http://ns.valcri.org/streams/anpr', 
    'stream' : 'http://localhost:8080/AnprStream/anpr4',
    'nominalTarget' : 'http://ns.valcri.org/data/nominals#nominal1234',
    'nominalVehiclesGraph' : 'http://localhost:8080/rest-spin/templates/get_triples/execute?limit=100000',
    'criminalNetworksGraph' : 'http://localhost:8080/rest-spin/templates/get_triples/execute?limit=100001' }")
  echo "Query results available at $URL"
  #echo
  #subscribeToCqels "$URL"
}

# 10 param
function scenario5 {
  echo
  echo 'Scenario 5'
  template=$(tr '\n' ' ' < ./paper_scenarios/scenario5.txt)
  del=$(deleteTemplate "trackPerson")
  add=$(addTemplate "$template")
  #echo $add
  sleep 1
  echo "Instantiate query:"
  time query=$(getQuery "trackPerson" "{
    'out' : 'http://ns.valcri.org/streams/anpr5', 
    'stream' : 'http://localhost:8080/AnprStream/anpr',
    'nominalVehiclesGraph' : 'http://localhost:8080/rest-spin/templates/get_triples/execute?limit=100000',
    'nominalsGraph' : 'http://localhost:8080/rest-spin/templates/get_triples/execute?limit=100001',
    'forename' : 'Jane',
    'surname' : 'Doe',
    'gender' : 'http://ns.valcri.org/ontology/valcricrimedata#F',
    'eaDesc' : 'http://ns.valcri.org/ontology/valcricrimedata#WHITE',
    'range' : 'PT10M',
    'step' : 'PT30M' }")
  #echo "${query}"
  #echo

  echo 'Register query'
  time URL=$(registerQuery "trackPerson" "{
    'out' : 'http://ns.valcri.org/streams/anpr', 
    'stream' : 'http://localhost:8080/AnprStream/anpr5',
    'nominalVehiclesGraph' : 'http://localhost:8080/rest-spin/templates/get_triples/execute?limit=100000',
    'nominalsGraph' : 'http://localhost:8080/rest-spin/templates/get_triples/execute?limit=100001',
    'forename' : 'Jane',
    'surname' : 'Doe',
    'gender' : 'http://ns.valcri.org/ontology/valcricrimedata#F',
    'eaDesc' : 'http://ns.valcri.org/ontology/valcricrimedata#WHITE',
    'range' : 'PT30M',
    'step' : 'PT10M' }")
  echo "Query results available at $URL"
  echo
  #subscribeToCqels "$URL"
}

e=$(stopAnpr)
e=$(startAnpr)
e=$(setupAnpr)
clear
# Warm up sceanrio, not recorded
scenario0
read -p "Press [Enter] key to go to next scenario..."
scenario1
read -p "Press [Enter] key to go to next scenario..."
scenario2
read -p "Press [Enter] key to go to next scenario..."
scenario3
read -p "Press [Enter] key to go to next scenario..."
scenario4
read -p "Press [Enter] key to go to next scenario..."
scenario5

#
