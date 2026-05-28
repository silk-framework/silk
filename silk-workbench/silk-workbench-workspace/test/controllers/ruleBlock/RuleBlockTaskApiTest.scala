package controllers.ruleBlock

import helper.IntegrationTestTrait
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.silkframework.config.MetaData
import org.silkframework.rule.input.{InputPortInput, TransformInput, Transformer}
import org.silkframework.rule.{RuleBlockInputExample, RuleBlockModel, RuleBlockPort, RuleBlockSpec}
import org.silkframework.runtime.serialization.WriteContext
import org.silkframework.serialization.json.JsonSerialization
import org.silkframework.util.Identifier
import org.silkframework.workspace.SingleProjectWorkspaceProviderTestTrait
import play.api.libs.json.JsValue
import testWorkspace.Routes

class RuleBlockTaskApiTest extends AnyFlatSpec
    with SingleProjectWorkspaceProviderTestTrait
    with IntegrationTestTrait
    with Matchers {

  behavior of "Rule Block Task API"

  override def workspaceProviderId: String = "inMemoryWorkspaceProvider"

  override def projectPathInClasspath: String = "diProjects/relatedItemsProject.zip"

  override def routes: Option[Class[Routes]] = Some(classOf[testWorkspace.Routes])

  private def evaluateRuleBlock(taskId: String, ruleBlockModel: RuleBlockModel): JsValue = {
    val path = controllers.ruleBlock.routes.RuleBlockTaskApi.evaluateRuleBlock(projectId, taskId).url
    implicit val writeContext: WriteContext[JsValue] = WriteContext.fromProject[JsValue](project)
    checkResponse(client.url(s"$baseUrl$path").post(JsonSerialization.toJson[RuleBlockModel](ruleBlockModel))).json
  }

  "evaluateRuleBlock" should "evaluate a rule block model against its stored input examples" in {
    val taskId = "evaluateRuleBlock"
    val ruleBlockModel = RuleBlockModel(
      ports = IndexedSeq(
        RuleBlockPort(Identifier("nameInput"), label = "Name", displayOrder = 1)
      ),
      inputExamples = IndexedSeq(
        RuleBlockInputExample(
          id = Identifier("example-1"),
          inputs = Map(
            Identifier("nameInput") -> Seq("Alice")
          )
        ),
        RuleBlockInputExample(
          id = Identifier("example-2"),
          inputs = Map(
            Identifier("nameInput") -> Seq("BOB")
          )
        )
      ),
      operator = Some(
        TransformInput(
          id = Identifier("lowerCaseNode"),
          transformer = Transformer("lowerCase"),
          inputs = IndexedSeq(
            InputPortInput(id = Identifier("inputPortNode"), portId = Identifier("nameInput"))
          )
        )
      )
    )
    project.addTask(
      taskId,
      RuleBlockSpec(ruleBlockModel),
      MetaData(Some("Evaluate rule block"))
    )

    val response = evaluateRuleBlock(taskId, ruleBlockModel).as[Seq[JsValue]]

    response must have size 2
    (response.head \ "operatorId").as[String] mustBe "lowerCaseNode"
    (response.head \ "values").as[Seq[String]] mustBe Seq("alice")
    val firstChildren = (response.head \ "children").as[Seq[JsValue]]
    firstChildren must have size 1
    (firstChildren.head \ "operatorId").as[String] mustBe "inputPortNode"
    (firstChildren.head \ "values").as[Seq[String]] mustBe Seq("Alice")

    (response(1) \ "operatorId").as[String] mustBe "lowerCaseNode"
    (response(1) \ "values").as[Seq[String]] mustBe Seq("bob")
    val secondChildren = (response(1) \ "children").as[Seq[JsValue]]
    secondChildren must have size 1
    (secondChildren.head \ "operatorId").as[String] mustBe "inputPortNode"
    (secondChildren.head \ "values").as[Seq[String]] mustBe Seq("BOB")
  }

  it should "treat missing example inputs as empty values" in {
    val taskId = "evaluateRuleBlockWithMissingInput"
    val ruleBlockModel = RuleBlockModel(
      ports = IndexedSeq(
        RuleBlockPort(Identifier("optionalInput"), label = "Optional", displayOrder = 1)
      ),
      inputExamples = IndexedSeq(
        RuleBlockInputExample(
          id = Identifier("example-without-value"),
          inputs = Map.empty
        )
      ),
      operator = Some(
        InputPortInput(
          id = Identifier("optionalInputNode"),
          portId = Identifier("optionalInput")
        )
      )
    )
    project.addTask(
      taskId,
      RuleBlockSpec(ruleBlockModel),
      MetaData(Some("Evaluate rule block with missing input"))
    )

    val response = evaluateRuleBlock(taskId, ruleBlockModel).as[Seq[JsValue]]

    response must have size 1
    (response.head \ "operatorId").as[String] mustBe "optionalInputNode"
    (response.head \ "values").as[Seq[String]] mustBe Seq.empty
    val children = (response.head \ "children").as[Seq[JsValue]]
    children must have size 1
    (children.head \ "values").as[Seq[String]] mustBe Seq.empty
  }
}
