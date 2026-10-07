package org.silkframework.serialization.json.changes

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.config.CustomTask
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.plugins.operations.SetExecutionVariableOperator
import org.silkframework.rule.{DirectMapping, MappingRules, MappingTarget, RootMappingRule, TransformSpec}
import org.silkframework.runtime.activity.TestUserContextTrait
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.templating.SimpleSubstitutionTemplateEngine
import org.silkframework.util.{ConfigTestTrait, Identifier}
import org.silkframework.workspace.changes.{ChangeEntry, ChangeJournalStore, DiscardedWorkflowRun}
import org.silkframework.workspace.resources.InMemoryResourceRepository
import org.silkframework.workspace.xml.{XmlZipProjectMarshaling, XmlZipWithResourcesProjectMarshaling, XmlZipWithoutResourcesProjectMarshaling}
import org.silkframework.workspace.{InMemoryWorkspaceProvider, Project, ProjectConfig, Workspace}

import java.io.FileOutputStream
import java.nio.file.{Files, Path}
import java.time.Instant
import java.util.Comparator
import java.util.zip.ZipFile
import scala.jdk.CollectionConverters._

/** The journal in project and workspace archives, with the file store. The in-memory store carries none. */
class ChangeJournalExportTest extends AnyFlatSpec with Matchers with ConfigTestTrait with TestUserContextTrait {

  behavior of "The change journal in ZIP archives"

  private val directory = Files.createTempDirectory("changeJournalExport")

  override def propertyMap: Map[String, Option[String]] = Map(
    "config.variables.engine" -> Some(SimpleSubstitutionTemplateEngine.id),
    "workspace.changes.plugin" -> Some("fileChangeJournal"),
    "workspace.changes.fileChangeJournal.dir" -> Some(directory.resolve("store").toString))

  override protected def afterAll(): Unit = {
    try {
      super.afterAll()
    } finally {
      val files = Files.walk(directory)
      try {
        files.sorted(Comparator.reverseOrder()).forEach(path => Files.delete(path))
      } finally {
        files.close()
      }
    }
  }

  private def workspace(): Workspace = new Workspace(new InMemoryWorkspaceProvider(), InMemoryResourceRepository())

  /** A project with two recorded task additions, which the watermark followed as user writes. */
  private def projectWithHistory(workspace: Workspace, id: Identifier): Project = {
    val project = workspace.createProject(ProjectConfig(id))
    project.addTask[TransformSpec]("transform", TransformSpec(mappingRule = RootMappingRule(MappingRules(propertyRules =
      Seq(DirectMapping("name", UntypedPath("name"), MappingTarget("http://example.org/name")))))))
    project.addTask[CustomTask]("setVariable", SetExecutionVariableOperator("myVar"))
    project.changeJournal.reviewedUpTo shouldBe 2
    project
  }

  private def exportProject(project: Project, marshaller: XmlZipProjectMarshaling, exportUserData: Boolean = true): Path = {
    val file = Files.createTempFile(directory, "project", ".zip")
    val out = new FileOutputStream(file.toFile)
    try {
      marshaller.marshalProject(project, out, project.resources, exportUserData = exportUserData)
    } finally {
      out.close()
    }
    file
  }

  private def zipEntries(file: Path): Seq[String] = {
    val zip = new ZipFile(file.toFile)
    try {
      zip.entries().asScala.map(_.getName).toSeq.sorted
    } finally {
      zip.close()
    }
  }

  it should "travel with the user data of a project and replace the journal of the project it is imported over" in {
    val source = workspace()
    val project = projectWithHistory(source, "exported")
    val entries = project.changeJournal.all
    entries.map(_.change.changeType) shouldBe Seq("AddTask", "AddTask")

    // With and without resources the archive carries the journal; without user data it does not
    val withResources = exportProject(project, XmlZipWithResourcesProjectMarshaling())
    zipEntries(withResources) should contain allOf("exported/changes/000000001.jsonl", "exported/changes/reviewed.json")
    zipEntries(exportProject(project, XmlZipWithoutResourcesProjectMarshaling())) should contain("exported/changes/000000001.jsonl")
    val withoutUserData = exportProject(project, XmlZipWithResourcesProjectMarshaling(), exportUserData = false)
    zipEntries(withoutUserData).filter(_.contains("/changes/")) shouldBe empty

    // Imported over a project with a history of its own, the archive's history and watermark replace it
    val target = workspace()
    projectWithHistory(target, "imported").addTask[CustomTask]("third", SetExecutionVariableOperator("other"))
    target.importProject("imported", withResources.toFile, XmlZipWithResourcesProjectMarshaling(), overwrite = true)
    target.project("imported").changeJournal.all shouldBe entries
    target.project("imported").changeJournal.reviewedUpTo shouldBe 2

    // Without user data, the project starts without history and without review state
    target.importProject("plain", withoutUserData.toFile, XmlZipWithResourcesProjectMarshaling())
    target.project("plain").changeJournal.all shouldBe empty
    target.project("plain").changeJournal.reviewedUpTo shouldBe 0
  }

  it should "travel with a workspace export, and not with a new project of a name the store still knows" in {
    val source = workspace()
    val project = projectWithHistory(source, "wsProject")
    val file = Files.createTempFile(directory, "workspace", ".zip")
    val out = new FileOutputStream(file.toFile)
    try {
      XmlZipWithResourcesProjectMarshaling().marshalWorkspace(out, Seq(project), source.repository)
    } finally {
      out.close()
    }
    zipEntries(file) should contain allOf("wsProject/changes/000000001.jsonl", "wsProject/changes/reviewed.json")
    val target = workspace()
    target.importWorkspace(file.toFile, XmlZipWithResourcesProjectMarshaling())
    target.project("wsProject").changeJournal.all shouldBe project.changeJournal.all
    target.project("wsProject").changeJournal.reviewedUpTo shouldBe 2

    // A journal left in the store by an older data directory is not the history of a new project of that name
    ChangeJournalStore().append("fresh", ChangeEntry(1, Instant.now, None, None, DiscardedWorkflowRun("wf")))(PluginContext.empty)
    workspace().createProject(ProjectConfig("fresh")).changeJournal.all shouldBe empty
  }
}
