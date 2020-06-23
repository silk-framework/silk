package controllers.transform

import controllers.transform.AutoCompletionApi.Categories
import org.silkframework.config.PlainTask
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.rule._
import org.silkframework.serialization.json.JsonSerializers._
import play.api.libs.json._

class AutoCompletionApiTest extends TransformTaskApiTestBase {

  def printResponses: Boolean = false

  "setup project" in {
    setup()
  }

  "auto complete source paths" in {
    val response = jsonGetRequest(s"$baseUrl/transform/tasks/$project/$task/rule/root/completions/sourcePaths")

    response.checkCompletionValues(
      category = Categories.sourcePaths,
      expectedValues = Seq("rdf:type", "source:address", "source:address/source:city", "source:address/source:country", "source:age", "source:name")
    )
  }

  "auto complete source paths matching input term" in {
    val response = jsonGetRequest(s"$baseUrl/transform/tasks/$project/$task/rule/root/completions/sourcePaths?term=NaMe")

    response.checkCompletionValues(
      category = Categories.sourcePaths,
      expectedValues = Seq("source:name")
    )
  }

  "auto complete source paths matching input term with leading /" in {
    val response = jsonGetRequest(s"$baseUrl/transform/tasks/$project/$task/rule/root/completions/sourcePaths?term=/NaMe")

    response.checkCompletionValues(
      category = Categories.sourcePaths,
      expectedValues = Seq("source:name")
    )
  }

  "auto complete target types matching input term" in {
    val response = jsonGetRequest(s"$baseUrl/transform/tasks/$project/$task/rule/root/completions/targetTypes?term=Perso")

    response.checkCompletionValues(
      category = Categories.vocabularyTypes,
      expectedValues = Seq("foaf:Agent", "foaf:Person", "foaf:PersonalProfileDocument")
    )
  }

  "auto complete target properties matching input term" in {
    val response = jsonGetRequest(s"$baseUrl/transform/tasks/$project/$task/rule/root/completions/targetProperties?term=given+name")

    response.checkCompletionValues(
      category = Categories.vocabularyProperties,
      expectedValues = Seq("foaf:givenname", "foaf:givenName")
    )
  }

  "auto complete target properties matching input term in their description" in {
    val response = jsonGetRequest(s"$baseUrl/transform/tasks/$project/$task/rule/root/completions/targetProperties?term=depiction")

    response.checkCompletionValues(
      category = Categories.vocabularyProperties,
      expectedValues = Seq("foaf:img", "foaf:depiction")
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
        mappingRule = RootMappingRule(rules =
          MappingRules(
            uriRule = None,
            typeRules = Seq(TypeMapping(typeUri = uri("Person"))),
            propertyRules = Seq(
              DirectMapping("name", sourcePath = UntypedPath(uri("name")), mappingTarget = MappingTarget(uri("name"))),
              ObjectMapping(
                id = "addressMapping",
                sourcePath = UntypedPath(uri("address")),
                target = Some(MappingTarget(uri("address"))),
                rules = MappingRules(
                  uriRule = None,
                  typeRules = Seq.empty,
                  propertyRules = Seq(
                    DirectMapping("city", sourcePath = UntypedPath(uri("city")), mappingTarget = MappingTarget(uri("city"))),
                    DirectMapping("country", sourcePath = UntypedPath(uri("country")), mappingTarget = MappingTarget(uri("country")))
                  )
                )
              )
            )
          )
        ),
        targetVocabularies = Seq("foaf.rdf")
      )
    val transformTask = PlainTask(task, transformSpec)
    val request = client.url(s"$baseUrl/transform/tasks/$project/$task")
    val response = request.put(toJson[TransformTask](transformTask))
    checkResponse(response)

    waitForCaches(task)
  }

  private implicit class AutoCompletionChecks(json: JsValue) {

    def checkCompletionValues(category: String, expectedValues: Seq[String]): Unit = {
      val filteredCompletions = json.as[JsArray].value.filter(c => (c \ "category").get == JsString(category))
      val values = filteredCompletions.map(c => (c \ "value").as[JsString].value)
      values must contain theSameElementsAs expectedValues
    }
  }

}
