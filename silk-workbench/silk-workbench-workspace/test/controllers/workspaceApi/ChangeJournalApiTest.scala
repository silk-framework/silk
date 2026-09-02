package controllers.workspaceApi

import controllers.projectApi.ChangeJournalApi.{ChangeEntryJson, ChangeListJson, RevertResultsJson}
import controllers.workspaceApi.coreApi.routes.{VariableTemplateApi => TemplateApi}
import helper.{ApiClient, IntegrationTestTrait}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.rule.{DirectMapping, MappingRules, MappingTarget, RootMappingRule, TransformSpec}
import org.silkframework.runtime.activity.{SimpleUserContext, UserExecutionContext}
import org.silkframework.runtime.templating.{TemplateVariable, VariableScope}
import org.silkframework.runtime.users.DefaultUserManager
import org.silkframework.serialization.json.TemplateVariableJson
import org.silkframework.util.ConfigTestTrait
import org.silkframework.workspace.changes.AddMapping
import org.silkframework.workspace.{ProjectConfig, WorkspaceFactory}
import play.api.libs.json.Json
import play.api.routing.Router

class ChangeJournalApiTest extends AnyFlatSpec with ConfigTestTrait with IntegrationTestTrait with ApiClient with Matchers {

  behavior of "Change journal API"

  override def workspaceProviderId: String = "inMemoryWorkspaceProvider"

  // No store is configured by default, which records nothing.
  override def propertyMap: Map[String, Option[String]] = Map("workspace.changes.plugin" -> Some("inMemoryChangeJournal"))

  override def routes: Option[Class[_ <: Router]] = Some(classOf[testWorkspace.Routes])

  private val projectId = "changeJournalProject"

  private def changesUrl(project: String): String = baseUrl + controllers.projectApi.routes.ChangeJournalApi.changes(project).url

  private def revertUrl(seq: Int, project: String = projectId): String =
    baseUrl + controllers.projectApi.routes.ChangeJournalApi.revert(project, seq).url

  private def rule(name: String): DirectMapping =
    DirectMapping(id = name, sourcePath = UntypedPath(name), mappingTarget = MappingTarget("http://example.org/" + name))

  private def changes(project: String = projectId): Seq[ChangeEntryJson] =
    checkResponse(client.url(changesUrl(project)).get()).json.as[ChangeListJson].changes

  it should "list the changes of a project, newest first, and revert one of them" in {
    val project = WorkspaceFactory().workspace.createProject(ProjectConfig(projectId))
    val task = project.addTask[TransformSpec]("transform", TransformSpec(mappingRule = RootMappingRule(MappingRules(propertyRules = Seq(rule("a"))))))
    task.applyChange(AddMapping(task.id, task.data.mappingRule.id, rule("b")))

    val listed = changes()
    listed.map(_.`type`) mustBe Seq("AddMapping", "AddTask")
    listed.head.description mustBe s"Added mapping rule 'b' under '${task.data.mappingRule.id}' in transform 'transform'"
    listed.head.revertedBy mustBe None
    listed.head.revertible mustBe true
    val seq = listed.head.seq

    val revert = checkResponse(client.url(revertUrl(seq)).post("")).json.as[ChangeEntryJson]
    revert.`type` mustBe "RemoveMapping"
    revert.reverts mustBe Some(seq)
    task.data.mappingRule.rules.propertyRules.map(_.id.toString) mustBe Seq("a")
    changes().find(_.seq == seq).get.revertedBy mustBe Some(revert.seq)

    // A change is reverted at most once; an unknown change is not found.
    checkResponseExactStatusCode(client.url(revertUrl(seq)).post(""), CONFLICT)
    checkResponseExactStatusCode(client.url(revertUrl(999)).post(""), NOT_FOUND)
  }

  it should "track the reviewed watermark and revert batches" in {
    val watermarkProjectId = "changeJournalWatermarkProject"
    val project = WorkspaceFactory().workspace.createProject(ProjectConfig(watermarkProjectId))
    val agent = SimpleUserContext(Some(DefaultUserManager.get("urn:agent")), UserExecutionContext(origin = Some("mcp:test")))
    def transform(ruleName: String): TransformSpec =
      TransformSpec(mappingRule = RootMappingRule(MappingRules(propertyRules = Seq(rule(ruleName)))))
    project.addTask[TransformSpec]("first", transform("a"))(implicitly, agent)
    project.addTask[TransformSpec]("second", transform("b"))(implicitly, agent)

    // Both agent entries are unreviewed until the watermark passes them
    val listed = checkResponse(client.url(changesUrl(watermarkProjectId)).get()).json.as[ChangeListJson]
    listed.reviewedUpTo mustBe 0
    listed.changes.map(_.unreviewed) mustBe Seq(Some(true), Some(true))

    val reviewedUrl = baseUrl + controllers.projectApi.routes.ChangeJournalApi.markReviewed(watermarkProjectId).url
    checkResponse(client.url(reviewedUrl).put(Json.obj("upTo" -> 1))).json mustBe Json.obj("reviewedUpTo" -> 1)
    val reviewed = checkResponse(client.url(changesUrl(watermarkProjectId)).get()).json.as[ChangeListJson]
    reviewed.reviewedUpTo mustBe 1
    reviewed.changes.map(_.unreviewed) mustBe Seq(Some(true), None)
    // A review beyond the latest change is refused
    checkResponseExactStatusCode(client.url(reviewedUrl).put(Json.obj("upTo" -> 99)), CONFLICT)

    // The batch reverts newest-first and skips an unknown entry
    val revertAllUrl = baseUrl + controllers.projectApi.routes.ChangeJournalApi.revertAll(watermarkProjectId).url
    val results = checkResponse(client.url(revertAllUrl).post(Json.obj("seqs" -> Seq(1, 2, 99)))).json.as[RevertResultsJson].results
    results.map(_.seq) mustBe Seq(99, 2, 1)
    results.map(_.outcome) mustBe Seq("skipped", "reverted", "reverted")
    results.last.entry.get.`type` mustBe "RemoveTask"
    project.anyTaskOption("first") mustBe None
    project.anyTaskOption("second") mustBe None

    // The reverted entries need no review anymore, although the watermark did not move
    val afterRevert = checkResponse(client.url(changesUrl(watermarkProjectId)).get()).json.as[ChangeListJson]
    afterRevert.reviewedUpTo mustBe 1
    afterRevert.changes.flatMap(_.unreviewed) mustBe empty
  }

  it should "journal a variable written through the variables API and revert it" in {
    val variablesProjectId = "changeJournalVariablesProject"
    val project = WorkspaceFactory().workspace.createProject(ProjectConfig(variablesProjectId))
    val variable = TemplateVariable("base", "urn:a", scope = VariableScope.project)
    checkResponse(createRequest(TemplateApi.putVariable(variablesProjectId, "base", None)).put(Json.toJson(TemplateVariableJson(variable))))

    val listed = changes(variablesProjectId)
    listed.map(_.`type`) mustBe Seq("SetVariable")
    listed.head.description mustBe "Added variable 'base' = 'urn:a'"

    val revert = checkResponse(client.url(revertUrl(listed.head.seq, variablesProjectId)).post("")).json.as[ChangeEntryJson]
    revert.`type` mustBe "RemoveVariable"
    revert.reverts mustBe Some(listed.head.seq)
    project.templateVariables.all.map.contains("base") mustBe false
  }
}
