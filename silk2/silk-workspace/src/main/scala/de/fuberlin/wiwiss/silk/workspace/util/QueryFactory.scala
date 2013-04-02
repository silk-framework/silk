/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.fuberlin.wiwiss.silk.workspace.util

import de.fuberlin.wiwiss.silk.config.Prefixes

object QueryFactory{

  var ontoGraph = ""
  val mappingGraph = "smwGraphs:MappingRepository"
  val datasourceGraph = "smwGraphs:DataSourceInformationGraph"
  val smwpropSuffix  = "/property/"
  val smwcatSuffix  =  "/category/"
  val dataSources = "http://www.example.org/smw-lde/smwDatasources/"
  val dataSourceLinks = "http://www.example.org/smw-lde/smwDatasourceLinks/"


  // default prefixes
  def getLDEDefaultPrefixes =  Map( "rdf" -> "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
                                    "rdfs" -> "http://www.w3.org/2000/01/rdf-schema#",
                                    "owl" -> "http://www.w3.org/2002/07/owl#",
                                    "smwprop" -> (ontoGraph+smwpropSuffix),
                                    "smwcat" -> (ontoGraph+smwcatSuffix))

  def getPrefixes = getLDEDefaultPrefixes ++
                         Map (  "smwGraphs" -> "http://www.example.org/smw-lde/smwGraphs/",
                                "smwDatasourceLinks" -> dataSourceLinks,
                                "smwDatasources" -> dataSources,
                                "smw-lde" -> "http://www.example.org/smw-lde/smw-lde.owl#" )



  def sparqlPrefixes :String  = Prefixes(getPrefixes).toSparql

  def setOntoGraph (graph : String) {
      ontoGraph = graph
  }


  //-- LDEWorkspace --

  // retrieve all mappings
  def sMappings =   sparqlPrefixes +
                    " SELECT ?uri" +
                    " FROM "+mappingGraph+
                    " WHERE  { ?uri rdf:type smw-lde:SilkMatchingDescription }"

  // create a new project
  def iNewProject(projectName : String) = {
    val projectUri = dataSourceLinks + projectName
    "INSERT DATA INTO "+mappingGraph+"  {  <"+projectUri+"> rdf:type  smw-lde:SilkMatchingDescription . <"+projectUri+">	smw-lde:linksTo <http://www.example.org/smw-lde/smwDatasources/Wiki> .  <"+projectUri+">	smw-lde:sourceCode \"<Silk />\" .  <"+projectUri+">	rdf:label \""+projectName+"\" }"
  }

  // delete a project by uri
  def dProject(projectName : String) =  "DELETE DATA FROM  "+mappingGraph+"  { <"+dataSourceLinks+projectName+"> ?p ?o } "

  // retrieve all datasources
  def sDataSources  =  sparqlPrefixes +
                        " SELECT ?uri ?id " +
                        " FROM "+datasourceGraph+
                        " WHERE  { ?uri rdf:type smw-lde:Datasource . ?uri smw-lde:ID ?id }"



  //-- LDEProject --

  // retrieve a project info by uri
  def sProjectDataSource(projectUri : String) = sparqlPrefixes + "SELECT ?from FROM "+mappingGraph+" WHERE  {   <"+projectUri+"> smw-lde:linksFrom ?from  }"
  def sProjectSourceCode(projectUri : String) = sparqlPrefixes + "SELECT ?xml FROM "+mappingGraph+" WHERE  {  <"+projectUri+"> smw-lde:sourceCode ?xml   }"

  // retrieve a datasource by uri
  def sDataSource(dataSourceUri : String) = sparqlPrefixes + "SELECT ?id ?desc FROM "+datasourceGraph+" WHERE   { <"+dataSourceUri+"> smw-lde:ID ?id . OPTIONAL {<"+dataSourceUri+"> smw-lde:description ?desc } }"

  // - Update DataSource
  // delete a SOURCE datasource link
  // SMW constraint - every link specification MUST have the WIKI datasource as TARGET source
  def dDataSource(projectUri : String) = "DELETE DATA FROM  "+mappingGraph+"  {<"+projectUri+"> smw-lde:linksFrom ?datasource} "
  // create a SOURCE datasource link
  def iDataSource(projectUri : String, dataSourceId : String) = "INSERT DATA INTO  "+mappingGraph+" {<"+projectUri+"> smw-lde:linksFrom <"+dataSourceId+"> } "

  // - Update Link Spec 
  // delete sourceCode mapping property
  def dSourceCode(projectUri : String) = "DELETE DATA FROM "+mappingGraph+" {<"+projectUri+"> smw-lde:sourceCode ?xml} "
  // create sourceCode mapping property 
  def iSourceCode(projectUri : String, sourceCode : String) = "INSERT DATA INTO "+mappingGraph+" {<"+projectUri+"> smw-lde:sourceCode \"<?xml version='1.0' encoding='utf-8' ?>"+sourceCode.replaceAll("\n","").replaceAll("\"","'") +"\" }    "



  //-- Linking Task Editor --

  // retrieve property paths from the (wiki) ontology
  def sPropertyPaths(categoryUri : String) = sparqlPrefixes + "SELECT ?p WHERE { GRAPH ?g {?p smwprop:Has_domain_and_range ?x. ?x smwprop:_1 "+ urify(categoryUri) + "} } "

  // retrieve categories from the (wiki) ontology
  // <http://mywiki/resource/category/Gene>	rdf:type owl:Class                  -> too generic, we have to query all the graphs..
  // <http://mywiki/property/exampleProperty> <http://mywiki/resource/property:Has_domain_and_range> ?x
  // ?x <http://mywiki/resource/property/_1> <http://mywiki/category/Gene>      <- domain
  // ?x <http://mywiki/resource/property/_2> <http://mywiki/category/Gene>      <- range
  def sCategories = sparqlPrefixes +"SELECT DISTINCT ?c WHERE { GRAPH ?g {?p smwprop:Has_domain_and_range ?x. {{?x smwprop:_1 ?c} UNION {?x smwprop:_2 ?c}} } } "
  //def sCategories = "SELECT DISTINCT ?c WHERE { GRAPH ?g {?p haloprop:domainAndRange ?x. {{?x haloprop:domain ?c} UNION {?x haloprop:range ?c}} } } "

  // if almostUri looks like an Uri -> add angle brackets or a proper prefix
  def urify(almostUri : String) : String = {
    if (almostUri.startsWith("http://"))
      {//"<"+ almostUri +">"
        "smwcat:"+ almostUri.split("/").last
      }
    else almostUri
  }

}