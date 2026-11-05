package controllers.workspaceApi

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.{Message, WebSocketRequest}
import akka.stream.scaladsl.{Keep, Sink, SinkQueueWithCancel, Source}
import controllers.projectApi.ChangeJournalApi.{ChangeEntryJson, ChangeListJson, ChangeSummaryJson, RevertConflictJson, RevertConflictsJson, RevertResultsJson}
import controllers.util.{ItemLink, ItemType}
import controllers.workspaceApi.coreApi.routes.{VariableTemplateApi => TemplateApi}
import helper.{ApiClient, IntegrationTestTrait}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.rule.{DirectMapping, MappingRules, MappingTarget, RootMappingRule, TransformSpec}
import org.silkframework.runtime.activity.{SimpleUserContext, UserContext, UserExecutionContext}
import org.silkframework.runtime.plugin.PluginRegistry
import org.silkframework.runtime.templating.{TemplateVariable, VariableScope}
import org.silkframework.runtime.users.DefaultUserManager
import org.silkframework.serialization.json.TemplateVariableJson
import org.silkframework.util.ConfigTestTrait
import org.silkframework.workspace.changes.{AddMapping, ChangeJournal, ProposedWorkflowRun, TestJournalAccess, WorkflowExecuted}
import org.silkframework.workspace.{ProjectConfig, WorkspaceFactory}
import play.api.libs.json.Json
import play.api.routing.Router

import scala.concurrent.Await
import scala.concurrent.duration._

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

  private def conflictsUrl(seqs: Seq[Int], project: String = projectId): String =
    baseUrl + controllers.projectApi.routes.ChangeJournalApi.conflicts(project, seqs).url

  private def conflicts(seqs: Seq[Int]): Seq[RevertConflictJson] =
    checkResponse(client.url(conflictsUrl(seqs)).get()).json.as[RevertConflictsJson].conflicts

  private def rule(name: String): DirectMapping =
    DirectMapping(id = name, sourcePath = UntypedPath(name), mappingTarget = MappingTarget("http://example.org/" + name))

  private def changes(project: String = projectId): Seq[ChangeEntryJson] =
    checkResponse(client.url(changesUrl(project)).get()).json.as[ChangeListJson].changes

  private def changeSocket(project: String, unreviewedOnly: Boolean,
                           user: Option[String] = None): SinkQueueWithCancel[ChangeListJson] = {
    implicit val actorSystem: ActorSystem = app.injector.instanceOf[ActorSystem]
    val url = baseUrl.replaceFirst("http", "ws") +
      s"/api/workspace/projects/$project/changes/updatesWebSocket?unreviewedOnly=$unreviewedOnly"
    val headers = user.toSeq.map(akka.http.scaladsl.model.headers.RawHeader("X-Forwarded-User", _))
    val (upgrade, queue) = Source.maybe[Message]
      .viaMat(Http().webSocketClientFlow(WebSocketRequest(url, extraHeaders = headers)))(Keep.right)
      .map(message => Json.parse(message.asTextMessage.getStrictText).as[ChangeListJson])
      .toMat(Sink.queue[ChangeListJson]())(Keep.both).run()
    Await.result(upgrade, 10.seconds).response.status mustBe StatusCodes.SwitchingProtocols
    queue
  }

  it should "push full unreviewed entries from every conversation, with updates after writes, reverts and review" in {
    val agent = UserContext.empty(UserExecutionContext(origin = Some("mcp:test"), conversationId = Some("conversation-1")))
    val otherAgent = UserContext.empty(UserExecutionContext(origin = Some("mcp:test"), conversationId = Some("other")))
    val unattributedAgent = UserContext.empty(UserExecutionContext(origin = Some("mcp:test")))
    val project = WorkspaceFactory().workspace.createProject(ProjectConfig("projectPushedChanges"))
    val spec = TransformSpec(mappingRule = RootMappingRule(MappingRules(propertyRules = Seq(rule("a")))))
    project.addTask[TransformSpec]("human", spec)
    project.addTask[TransformSpec]("reviewed", spec)(implicitly, agent)
    project.changeJournal.markReviewed(project.changeJournal.all.last.seq)
    project.addTask[TransformSpec]("first", spec)(implicitly, agent)
    val queue = changeSocket(project.id.toString, unreviewedOnly = true)
    def next(): ChangeListJson = Await.result(queue.pull(), 10.seconds).get
    try {
      val initial = next()
      initial.reviewedUpTo mustBe 2
      initial.changes.map(_.conversationId) mustBe Seq(Some("conversation-1"))
      initial.changes.head.summary must include("first")
      initial.changes.head.links must not be empty
      initial.changes.head.unreviewed mustBe Some(true)
      project.addTask[TransformSpec]("second", spec)(implicitly, otherAgent)
      next().changes.map(_.conversationId) mustBe Seq(Some("other"), Some("conversation-1"))
      project.addTask[TransformSpec]("third", spec)(implicitly, unattributedAgent)
      next().changes.map(_.conversationId) mustBe Seq(None, Some("other"), Some("conversation-1"))
      project.changeJournal.revert(project.changeJournal.all.last.seq)
      next().changes.size mustBe 2
      project.changeJournal.markReviewed(project.changeJournal.all.last.seq)
      next().changes mustBe empty
      // HTTP uses the same filter and payload; omitting the flag still returns the full journal.
      val filteredUrl = changesUrl(project.id.toString) + "?unreviewedOnly=true"
      checkResponse(client.url(filteredUrl).get()).json.as[ChangeListJson].changes mustBe empty
      changes(project.id.toString).size mustBe 6
    } finally {
      queue.cancel()
    }
  }

  it should "also support streaming the complete journal without the unreviewed filter" in {
    val project = WorkspaceFactory().workspace.createProject(ProjectConfig("projectAllPushedChanges"))
    val queue = changeSocket(project.id.toString, unreviewedOnly = false)
    try {
      Await.result(queue.pull(), 10.seconds).get.changes mustBe empty
      project.addTask[TransformSpec]("human", TransformSpec())
      Await.result(queue.pull(), 10.seconds).get.changes.map(_.unreviewed) mustBe Seq(None)
    } finally {
      queue.cancel()
    }
  }

  it should "enforce project access for both the filtered list and the websocket" in {
    import TestWebUserManager._
    PluginRegistry.registerPlugin(classOf[TestWebUserManager])
    ConfigTestTrait.withConfig("workspace.accessControl.enabled" -> Some("true"),
      "user.manager.web.plugin" -> Some("testWebUserManager")) {
      val agent = SimpleUserContext(Some(admin), UserExecutionContext(origin = Some("mcp:test"), conversationId = Some("conversation-1")))
      val project = WorkspaceFactory().workspace.createProject(ProjectConfig("restrictedPushedChanges"))(agent)
      project.accessControl.setGroups(Set(group1))
      project.addTask[TransformSpec]("pending", TransformSpec())(implicitly, agent)
      val url = changesUrl(project.id.toString) + "?unreviewedOnly=true"
      checkResponse(client.url(url).addHttpHeaders("X-Forwarded-User" -> user1.uri).get()).json.as[ChangeListJson].changes.size mustBe 1
      checkResponseExactStatusCode(client.url(url).addHttpHeaders("X-Forwarded-User" -> user2.uri).get(), FORBIDDEN)
      val queue = changeSocket(project.id.toString, unreviewedOnly = true, user = Some(user1.uri))
      try {
        Await.result(queue.pull(), 10.seconds).get.changes.size mustBe 1
      } finally {
        queue.cancel()
      }
      implicit val actorSystem: ActorSystem = app.injector.instanceOf[ActorSystem]
      val wsUrl = baseUrl.replaceFirst("http", "ws") +
        s"/api/workspace/projects/${project.id}/changes/updatesWebSocket?unreviewedOnly=true"
      val request = WebSocketRequest(wsUrl, extraHeaders = Seq(akka.http.scaladsl.model.headers.RawHeader("X-Forwarded-User", user2.uri)))
      val (upgrade, done) = Source.empty[Message].viaMat(Http().webSocketClientFlow(request))(Keep.right)
        .toMat(Sink.ignore)(Keep.both).run()
      Await.result(upgrade, 10.seconds).response.status mustBe StatusCodes.Forbidden
      scala.util.Try(Await.result(done, 10.seconds))
    }
  }

  it should "list the changes of a project, newest first, and revert one of them" in {
    val project = WorkspaceFactory().workspace.createProject(ProjectConfig(projectId))
    val task = project.addTask[TransformSpec]("transform", TransformSpec(mappingRule = RootMappingRule(MappingRules(propertyRules = Seq(rule("a"))))))
    task.applyChange(AddMapping(task.id, task.data.mappingRule.id, rule("b")))

    val listed = changes()
    listed.map(_.`type`) mustBe Seq("AddMapping", "AddTask")
    listed.head.description mustBe
      s"Added value mapping 'b' (b → http://example.org/b) under '${task.data.mappingRule.id}' in transform 'transform'"
    listed.head.revertedBy mustBe None
    listed.head.revertible mustBe true
    listed.head.summary mustBe listed.head.description
    listed.head.details mustBe empty
    // The task addition cannot be reverted while the mapping change since stands, which is told for the asked changes before a revert is tried
    conflicts(listed.map(_.seq)) mustBe Seq(RevertConflictJson(listed(1).seq, s"Task 'transform' in project '$projectId' has been changed since."))
    // At least one and at most 100 changes are checked per request; a change named twice counts once
    checkResponseExactStatusCode(client.url(conflictsUrl(Seq.empty)).get(), BAD_REQUEST)
    checkResponseExactStatusCode(client.url(conflictsUrl(1 to 101)).get(), BAD_REQUEST)
    conflicts(Seq.fill(101)(listed.head.seq)) mustBe empty
    // A task change links the task page, a mapping change the rule it added, as handed out by the server
    val taskLink = ItemType.itemDetailsPage(ItemType.transform, projectId, "transform")
    def ruleLink(ruleId: String) = ItemLink("rule", s"Mapping rule '$ruleId'", s"${taskLink.path}?ruleId=$ruleId")
    listed.map(_.links) mustBe Seq(Seq(ruleLink("b")), Seq(taskLink))
    val seq = listed.head.seq

    val revert = checkResponse(client.url(revertUrl(seq)).post("")).json.as[ChangeEntryJson]
    revert.`type` mustBe "RemoveMapping"
    revert.reverts mustBe Some(seq)
    task.data.mappingRule.rules.propertyRules.map(_.id.toString) mustBe Seq("a")
    changes().find(_.seq == seq).get.revertedBy mustBe Some(revert.seq)
    // The task is as added again, so its addition can be reverted; a reverted entry is not checked
    conflicts(changes().map(_.seq)) mustBe empty
    // The removal links the parent it happened in; the addition falls back to the task page, as its rule is gone
    revert.links mustBe Seq(ruleLink(task.data.mappingRule.id))
    changes().find(_.seq == seq).get.links mustBe Seq(taskLink)

    // A change is reverted at most once; an unknown change is not found.
    checkResponseExactStatusCode(client.url(revertUrl(seq)).post(""), CONFLICT)
    checkResponseExactStatusCode(client.url(revertUrl(999)).post(""), NOT_FOUND)

    // A recorded workflow run links its persisted execution report; the workflow itself does not exist here, so no task page
    val proposal = project.changeJournal.propose(ProposedWorkflowRun("wf"))
    TestJournalAccess.record(project.changeJournal, WorkflowExecuted("wf", Some("2026-08-26T09:52:08.126Z"), failed = false))
    val run = changes().head
    run.`type` mustBe "WorkflowExecuted"
    val reportUrl = controllers.workspaceApi.routes.ReportsApi.retrieveReport(projectId, "wf", "2026-08-26T09:52:08.126Z").url
    run.links mustBe Seq(ItemLink("report", "Execution report", reportUrl, openInNewTab = true))
    run.revertible mustBe false

    // The run fulfilled the open proposal, which is final from then on
    val proposed = changes().find(_.seq == proposal.seq).get
    proposed.fulfilledBy mustBe Some(run.seq)
    proposed.revertible mustBe false
    checkResponseExactStatusCode(client.url(revertUrl(proposal.seq)).post(""), CONFLICT)
  }

  it should "track the reviewed watermark and revert batches" in {
    val watermarkProjectId = "changeJournalWatermarkProject"
    val project = WorkspaceFactory().workspace.createProject(ProjectConfig(watermarkProjectId))
    val agent = SimpleUserContext(Some(DefaultUserManager.get("urn:agent")), UserExecutionContext(origin = Some("mcp:test")))
    def transform(ruleName: String): TransformSpec =
      TransformSpec(mappingRule = RootMappingRule(MappingRules(propertyRules = Seq(rule(ruleName)))))
    project.addTask[TransformSpec]("first", transform("a"))(implicitly, agent)
    project.addTask[TransformSpec]("second", transform("b"))(implicitly, agent)

    // Both agent entries are unreviewed until the watermark passes them; the summary counts them without listing
    val summaryUrl = baseUrl + controllers.projectApi.routes.ChangeJournalApi.summary(watermarkProjectId).url
    def summary(): ChangeSummaryJson = checkResponse(client.url(summaryUrl).get()).json.as[ChangeSummaryJson]
    val listed = checkResponse(client.url(changesUrl(watermarkProjectId)).get()).json.as[ChangeListJson]
    listed.reviewedUpTo mustBe 0
    listed.changes.map(_.unreviewed) mustBe Seq(Some(true), Some(true))
    summary() mustBe ChangeSummaryJson(reviewedUpTo = 0, latestSeq = 2, unreviewed = 2)

    val reviewedUrl = baseUrl + controllers.projectApi.routes.ChangeJournalApi.markReviewed(watermarkProjectId).url
    checkResponse(client.url(reviewedUrl).put(Json.obj("upTo" -> 1))).json mustBe Json.obj("reviewedUpTo" -> 1)
    val reviewed = checkResponse(client.url(changesUrl(watermarkProjectId)).get()).json.as[ChangeListJson]
    reviewed.reviewedUpTo mustBe 1
    reviewed.changes.map(_.unreviewed) mustBe Seq(Some(true), None)
    summary() mustBe ChangeSummaryJson(reviewedUpTo = 1, latestSeq = 2, unreviewed = 1)
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
    // A removed task has no page to link, on the removal and on the entries before it
    results.last.entry.get.links mustBe empty
    changes(watermarkProjectId).flatMap(_.links) mustBe empty

    // The reverted entries need no review anymore, although the watermark did not move
    val afterRevert = checkResponse(client.url(changesUrl(watermarkProjectId)).get()).json.as[ChangeListJson]
    afterRevert.reviewedUpTo mustBe 1
    afterRevert.changes.flatMap(_.unreviewed) mustBe empty
    summary() mustBe ChangeSummaryJson(reviewedUpTo = 1, latestSeq = 4, unreviewed = 0)
  }

  it should "journal a variable written through the variables API and revert it" in {
    val variablesProjectId = "changeJournalVariablesProject"
    val project = WorkspaceFactory().workspace.createProject(ProjectConfig(variablesProjectId))
    val variable = TemplateVariable("base", "urn:a", scope = VariableScope.project)
    checkResponse(createRequest(TemplateApi.putVariable(variablesProjectId, "base", None)).put(Json.toJson(TemplateVariableJson(variable))))

    val listed = changes(variablesProjectId)
    listed.map(_.`type`) mustBe Seq("SetVariable")
    listed.head.description mustBe "Added variable 'base' = 'urn:a'"
    // The project page holds the variables widget
    listed.head.links mustBe Seq(ItemType.itemDetailsPage(ItemType.project, variablesProjectId, variablesProjectId))

    val revert = checkResponse(client.url(revertUrl(listed.head.seq, variablesProjectId)).post("")).json.as[ChangeEntryJson]
    revert.`type` mustBe "RemoveVariable"
    revert.reverts mustBe Some(listed.head.seq)
    project.templateVariables.all.map.contains("base") mustBe false
  }

  it should "link a file change to the project page and, while the file exists, to its download" in {
    val filesProjectId = "changeJournalFilesProject"
    val project = WorkspaceFactory().workspace.createProject(ProjectConfig(filesProjectId))
    val file = project.resources.get("data.txt")
    ChangeJournal.onBehalfOf(implicitly[UserContext]) {
      file.writeString("content")
    }

    val created = changes(filesProjectId).head
    created.`type` mustBe "ResourceCreated"
    val projectPage = ItemType.itemDetailsPage(ItemType.project, filesProjectId, filesProjectId)
    val downloadUrl = controllers.workspace.routes.ResourceApi.getFileForDownload(filesProjectId, "data.txt").url
    created.links mustBe Seq(projectPage, ItemLink("download", "Download file", downloadUrl, openInNewTab = true))

    // Deleted outside of a request, so not recorded: the entry stays, its download is gone
    file.delete()
    changes(filesProjectId).head.links mustBe Seq(projectPage)
  }
}
