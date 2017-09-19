package controllers.transform

import org.silkframework.config.{PlainTask, Task}
import org.silkframework.rule._
import org.silkframework.serialization.json.JsonSerializers._
import play.api.libs.ws.WS

class TargetVocabularyApiTest extends TransformTaskApiTestBase {

  def printResponses: Boolean = false

  "setup project" in {
    setup()
  }

  "retrieve information about a vocabulary type using its full URI" in {
    val response = jsonGetRequest(s"$baseUrl/transform/tasks/$project/$task/targetVocabulary/type?uri=http://xmlns.com/foaf/0.1/Person")
    response mustMatchJson
      """
        {
          "genericInfo" : {
            "uri" : "foaf:Person",
            "label" : "Person",
            "description" : "A person."
          },
          "parentClasses" : [ "http://www.w3.org/2003/01/geo/wgs84_pos#SpatialThing", "foaf:Agent" ]
        }
      """
  }

  "retrieve information about a vocabulary type using a prefixed name" in {
    val response = jsonGetRequest(s"$baseUrl/transform/tasks/$project/$task/targetVocabulary/type?uri=foaf:Person")
    response mustMatchJson
      """
        {
          "genericInfo" : {
            "uri" : "foaf:Person",
            "label" : "Person",
            "description" : "A person."
          },
          "parentClasses" : [ "http://www.w3.org/2003/01/geo/wgs84_pos#SpatialThing", "foaf:Agent" ]
        }
      """
  }

  "retrieve information about a vocabulary property" in {
    val response = jsonGetRequest(s"$baseUrl/transform/tasks/$project/$task/targetVocabulary/property?uri=foaf:name")
    response mustMatchJson
      """
        {
          "genericInfo" : {
            "uri" : "foaf:name",
            "label" : "name",
            "description" : "A name for some thing."
          },
          "domain" : "owl:Thing",
          "range" : "rdfs:Literal"
        }
      """
  }

  "retrieve information about a URI that could be a type or a property and actually is a type" in {
    val response = jsonGetRequest(s"$baseUrl/transform/tasks/$project/$task/targetVocabulary/typeOrProperty?uri=foaf:Person")
    response mustMatchJson
      """
        {
          "genericInfo" : {
            "uri" : "foaf:Person",
            "label" : "Person",
            "description" : "A person."
          },
          "parentClasses" : [ "http://www.w3.org/2003/01/geo/wgs84_pos#SpatialThing", "foaf:Agent" ]
        }
      """
  }

  "retrieve information about a URI that could be a type or a property and actually is a property" in {
    val response = jsonGetRequest(s"$baseUrl/transform/tasks/$project/$task/targetVocabulary/typeOrProperty?uri=foaf:name")
    response mustMatchJson
      """
        {
          "genericInfo" : {
            "uri" : "foaf:name",
            "label" : "name",
            "description" : "A name for some thing."
          },
          "domain" : "owl:Thing",
          "range" : "rdfs:Literal"
        }
      """
  }

  private def setup() = {
    createProject(project)
    addProjectPrefixes(project, Map("foaf" -> "http://xmlns.com/foaf/0.1/"))
    uploadResource(project, "foaf.rdf", "controllers/transform")
    val datasetConfig =
      <Dataset id="source" type="inMemory">
      </Dataset>
    createDataset(project, "source", datasetConfig)

    val transformSpec =
      TransformSpec(
        selection = DatasetSelection("source", "https://ns.eccenca.com/source/Person"),
        mappingRule = RootMappingRule(MappingRules.empty),
        targetVocabularies = Seq("foaf.rdf")
      )
    val transformTask = PlainTask(task, transformSpec)
    val request = WS.url(s"$baseUrl/transform/tasks/$project/$task")
    val response = request.put(toJson[Task[TransformSpec]](transformTask))
    checkResponse(response)

    waitForCaches(task)
  }

}
