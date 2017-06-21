package controllers.transform

import controllers.transform.AutoCompletionApi.Categories
import org.silkframework.config.{PlainTask, Task}
import org.silkframework.entity.Path
import org.silkframework.rule._
import org.silkframework.serialization.json.JsonSerializers._
import play.api.libs.json._
import play.api.libs.ws.WS

class AutoCompletionApiTest extends TransformTaskApiTestBase {

  def printResponses: Boolean = false

  "Setup project" in {
    setup()
  }

  "auto complete source paths" in {
    val response = jsonGetRequest(s"$baseUrl/transform/tasks/$project/$task/rule/root/completions/sourcePaths")

    response.checkCompletionValues(
      category = Categories.sourcePaths,
      expectedValues = Set("rdf:type", "source:name", "source:age", "source:address")
    )
  }

  "auto complete source paths matching input term" in {
    val response = jsonGetRequest(s"$baseUrl/transform/tasks/$project/$task/rule/root/completions/sourcePaths?term=NaMe")

    response.checkCompletionValues(
      category = Categories.sourcePaths,
      expectedValues = Set("source:name")
    )
  }

  "auto complete target types matching input term" in {
    val response = jsonGetRequest(s"$baseUrl/transform/tasks/$project/$task/rule/root/completions/targetTypes?term=Perso")

    response.checkCompletionValues(
      category = Categories.vocabularyTypes,
      expectedValues = Set("foaf:Person", "foaf:PersonalProfileDocument")
    )
  }

  "auto complete target properties matching input term" in {
    val response = jsonGetRequest(s"$baseUrl/transform/tasks/$project/$task/rule/root/completions/targetProperties?term=given+name")

    response.checkCompletionValues(
      category = Categories.vocabularyProperties,
      expectedValues = Set("foaf:givenname", "foaf:givenName")
    )
  }

  private def uri(name: String) = "https://ns.eccenca.com/source/" + name

  private def setup() = {
    createProject(project)
    addProjectPrefixes(project, Map("foaf" -> "http://xmlns.com/foaf/0.1/"))
    uploadResource(project, "persons.ttl", "controllers/transform")
    uploadResource(project, "foaf.rdf", "controllers/transform")
    val datasetConfig =
      <Dataset id="source" type="file">
        <Param name="file" value="persons.ttl"/>
        <Param name="format" value="Turtle"/>
      </Dataset>
    createDataset(project, "source", datasetConfig)

    val transformSpec =
      TransformSpec(
        selection = DatasetSelection("source", uri("Person")),
        mappingRule = RootMappingRule(
          MappingRules(
            uriRule = None,
            typeRules = Seq(TypeMapping(typeUri = uri("Person"))),
            propertyRules = Seq(
              DirectMapping(sourcePath = Path(uri("name")), mappingTarget = MappingTarget(uri("name"))),
              ObjectMapping(
                id = "addressMapping",
                sourcePath = Path(uri("address")),
                target = Some(MappingTarget(uri("address"))),
                rules = MappingRules(
                  uriRule = None,
                  typeRules = Seq.empty,
                  propertyRules = Seq(
                    DirectMapping(sourcePath = Path(uri("city")), mappingTarget = MappingTarget(uri("city"))),
                    DirectMapping(sourcePath = Path(uri("country")), mappingTarget = MappingTarget(uri("country")))
                  )
                )
              )
            )
          )
        ),
        targetVocabularies = Seq("foaf.rdf")
      )
    val transformTask = PlainTask(task, transformSpec)
    val request = WS.url(s"$baseUrl/transform/tasks/$project/$task")
    val response = request.put(toJson[Task[TransformSpec]](transformTask))
    checkResponse(response)

    waitForCaches(task)
  }

  private implicit class AutoCompletionChecks(json: JsValue) {

    def checkCompletionValues(category: String, expectedValues: Set[String]): Unit = {
      val filteredCompletions = json.as[JsArray].value.filter(c => (c \ "category").get == JsString(category))
      val values = filteredCompletions.map(c => (c \ "value").as[JsString].value)
      values.toSet mustBe expectedValues
    }
  }

}
