package de.fuberlin.wiwiss.silk.workbench.util


object QueryFactory{

  val mappingGraph = "smwGraphs:MappingRepository"
  val datasourceGraph = "smwGraphs:DataSourceInformationGraph"
  val dataSources = "http://www.example.org/smw-lde/smwDatasources/"
  val dataSourceLinks = "http://www.example.org/smw-lde/smwDatasourceLinks/"

  def getPrefixes = Map ( "smwGraphs" -> "http://www.example.org/smw-lde/smwGraphs/",
                          "smwDatasourceLinks" -> dataSourceLinks,
                          "smwDatasources" -> dataSources,
                          "smw-lde" -> "http://www.example.org/smw-lde/smw-lde.owl#",
                          "rdf" -> "http://www.w3.org/1999/02/22-rdf-syntax-ns#"  )
  
  //-- LDEWorkspace --

  // retrieve all mappings
  def sMappings =   " SELECT ?uri" +
                    " FROM "+mappingGraph+
                    " WHERE  { ?uri rdf:type smw-lde:SilkMatchingDescription }"

  // create a new project
  def iNewProject(projectName : String) = {
    val projectUri = dataSourceLinks + projectName
    "INSERT DATA INTO "+mappingGraph+"  {  <"+projectUri+"> rdf:type  smw-lde:SilkMatchingDescription . <"+projectUri+">	smw-lde:linksTo <http://www.example.org/smw-lde/smwDatasources/Wiki> .  <"+projectUri+">	smw-lde:sourceCode \"<Silk />\" .  <"+projectUri+">	rdf:label \""+projectName+"\" }"
  }

  // delete a project by uri
  def dProject(projectName : String) =  "DELETE DATA FROM  "+mappingGraph+"  { <"+dataSourceLinks+projectName+"> ?p ?o } "

  //-- LDEProject --

  // retrieve a project info by uri
  def sProjectDataSource(projectUri : String) = "SELECT ?from FROM "+mappingGraph+" WHERE  {   <"+projectUri+"> smw-lde:linksFrom ?from  }"
  def sProjectSourceCode(projectUri : String) = "SELECT ?xml FROM "+mappingGraph+" WHERE  {  <"+projectUri+"> smw-lde:sourceCode ?xml   }"

  // retrieve all datasources
  def sDataSources  =   " SELECT ?uri " +
                        " FROM "+datasourceGraph+
                        " WHERE  { ?uri rdf:type smw-lde:Datasource }"

  // retrieve a datasource by uri
  def sDataSource(dataSourceUri : String) = "SELECT ?url ?label ?graph FROM "+datasourceGraph+" WHERE   { <"+dataSourceUri+"> smw-lde:sparqlEndpointLocation ?url . <"+dataSourceUri+"> smw-lde:ID ?label . <"+dataSourceUri+"> smw-lde:sparqlGraphName ?graph }"

  // - Update DataSource
  // delete a SOURCE datasource link
  // SMW constraint - every link specification MUST have the WIKI datasource as TARGET source
  def dDataSource(projectUri : String) = "DELETE DATA FROM  "+mappingGraph+"  {<"+projectUri+"> smw-lde:linksFrom ?datasource} "
  // create a SOURCE datasource link
  def iDataSource(projectUri : String, dataSourceId : String) = "INSERT DATA INTO  "+mappingGraph+" {<"+projectUri+"> smw-lde:linksFrom <"+dataSources+dataSourceId+"> } "

  // - Update Link Spec 
  // delete sourceCode mapping property
  def dSourceCode(projectUri : String) = "DELETE DATA FROM "+mappingGraph+" {<"+projectUri+"> smw-lde:sourceCode ?xml} "
  // create sourceCode mapping property 
  def iSourceCode(projectUri : String, sourceCode : String) = "INSERT DATA INTO "+mappingGraph+" {<"+projectUri+"> smw-lde:sourceCode \"<?xml version='1.0' encoding='utf-8' ?>"+sourceCode.replaceAll("\n","").replaceAll("\"","'") +"\" }    "

}