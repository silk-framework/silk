package controllers.workspaceApi

import controllers.projectApi.ChangeJournalApi.{ChangeEntryJson, ChangeListJson}
import helper.{ApiClient, IntegrationTestTrait}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.rule.{DirectMapping, MappingRules, MappingTarget, RootMappingRule, TransformSpec}
import org.silkframework.workspace.changes.AddMapping
import org.silkframework.workspace.{ProjectConfig, WorkspaceFactory}
import play.api.routing.Router

class ChangeJournalApiTest extends AnyFlatSpec with IntegrationTestTrait with ApiClient with Matchers {

  behavior of "Change journal API"

  override def workspaceProviderId: String = "inMemoryWorkspaceProvider"

  override def routes: Option[Class[_ <: Router]] = Some(classOf[testWorkspace.Routes])

  private val projectId = "changeJournalProject"

  private def changesUrl: String = baseUrl + controllers.projectApi.routes.ChangeJournalApi.changes(projectId).url

  private def revertUrl(seq: Int): String = baseUrl + controllers.projectApi.routes.ChangeJournalApi.revert(projectId, seq).url

  private def rule(name: String): DirectMapping =
    DirectMapping(id = name, sourcePath = UntypedPath(name), mappingTarget = MappingTarget("http://example.org/" + name))

  private def changes(): Seq[ChangeEntryJson] = checkResponse(client.url(changesUrl).get()).json.as[ChangeListJson].changes

  it should "list the changes of a project, newest first, and revert one of them" in {
    val project = WorkspaceFactory().workspace.createProject(ProjectConfig(projectId))
    val task = project.addTask[TransformSpec]("transform", TransformSpec(mappingRule = RootMappingRule(MappingRules(propertyRules = Seq(rule("a"))))))
    task.applyChange(AddMapping(task.id, task.data.mappingRule.id, rule("b")))

    val listed = changes()
    listed.map(_.`type`) mustBe Seq("AddMapping", "AddTask")
    listed.head.description mustBe s"Added mapping rule 'b' under '${task.data.mappingRule.id}' in transform 'transform'"
    listed.head.revertedBy mustBe None
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
}
