package org.silkframework.workspace

import org.scalatest.{FlatSpec, ShouldMatchers}
import org.silkframework.config._
import org.silkframework.dataset.Dataset
import org.silkframework.entity.{EntitySchema, Path}
import org.silkframework.plugins.dataset.InternalDataset
import org.silkframework.rule.input.PathInput
import org.silkframework.rule.plugins.distance.characterbased.QGramsMetric
import org.silkframework.rule.similarity.Comparison
import org.silkframework.rule._
import org.silkframework.runtime.plugin.PluginRegistry
import org.silkframework.runtime.resource.ResourceNotFoundException
import org.silkframework.util.Identifier
import org.silkframework.workspace.activity.workflow.{Workflow, WorkflowDataset, WorkflowOperator}
import org.silkframework.workspace.resources.ResourceRepository

/**
  * Created on 9/13/16.
  */
trait WorkspaceProviderTestTrait extends FlatSpec with ShouldMatchers {
  val PROJECT_NAME = "ProjectName"
  val PROJECT_NAME_OTHER = "ProjectNameOther"
  val CHILD = "child"
  val WORKFLOW_ID = "workflow1"
  val TRANSFORM_ID = "transform1"
  val DATASET_ID = "dataset1"
  val LINKING_TASK_ID = "linking1"
  val CUSTOM_TASK_ID = "custom1"

  PluginRegistry.registerPlugin(classOf[TestCustomTask])

  def createWorkspaceProvider(): WorkspaceProvider

  val refreshTest = withRefresh(PROJECT_NAME)(_)

  private val workspace = createWorkspaceProvider()

  private val repository = ResourceRepository()

  val rule =
    LinkageRule(
      operator = Some(
        Comparison(
          id = "compareNames",
          threshold = 0.1,
          metric = QGramsMetric(q = 3),
          inputs = PathInput("foafName", Path("http://xmlns.com/foaf/0.1/name")) ::
              PathInput("foafName2", Path("http://xmlns.com/foaf/0.1/name")) :: Nil
        )
      ),
      linkType = "http://www.w3.org/2002/07/owl#sameAs"
    )

  val dataset = InternalDataset("default")

  val linkTask = LinkSpec(rule = rule)

  val transformTask = TransformSpec(
    selection = DatasetSelection("InputDS", "http://type1"),
    rules = Seq(DirectMapping(
      name = TRANSFORM_ID,
      sourcePath = Path("prop1")
    ))
  )

  val miniWorkflow: Workflow = {
    Workflow(
      Identifier(WORKFLOW_ID),
      operators = Seq(
        WorkflowOperator(inputs = Seq(DATASET_ID), task = TRANSFORM_ID, outputs = Seq(), Seq(), (0, 0), TRANSFORM_ID, None)
      ),
      datasets = Seq(
        WorkflowDataset(Seq(), DATASET_ID, Seq(TRANSFORM_ID), (1,2), DATASET_ID, Some(1.0))
      ))
  }

  val customTask: CustomTask = TestCustomTask(stringParam = "xxx", numberParam = 12)

  it should "read and write projects" in {
    val project = createProject(PROJECT_NAME)
    val project2 = createProject(PROJECT_NAME_OTHER)
    workspace.readProjects().find(_.id == project.id) should be (Some(project.copy(projectResourceUriOpt = Some(project.generateDefaultUri))))
    workspace.readProjects().find(_.id == project2.id) should be (Some(project2.copy(projectResourceUriOpt = Some(project2.generateDefaultUri))))
  }

  private def createProject(projectName: String): ProjectConfig = {
    val project = ProjectConfig(projectName)
    workspace.putProject(project)
    project
  }

  it should "read and write linking tasks" in {
    workspace.putTask(PROJECT_NAME, LINKING_TASK_ID, linkTask)
    refreshTest {
      workspace.readTasks[LinkSpec](PROJECT_NAME).headOption.map(_._2) shouldBe Some(linkTask)
    }
  }

  it should "read and write dataset tasks" in {
    workspace.putTask(PROJECT_NAME, DATASET_ID, dataset)
    refreshTest {
      val ds = workspace.readTasks[Dataset](PROJECT_NAME).headOption.map(_._2).get
      ds shouldBe dataset
    }
  }

  it should "read and write transformation tasks" in {
    workspace.putTask(PROJECT_NAME, TRANSFORM_ID, transformTask)
    refreshTest {
      workspace.readTasks[TransformSpec](PROJECT_NAME).headOption.map(_._2) shouldBe Some(transformTask)
    }
  }

  it should "read and write workflows" in {
    workspace.putTask(PROJECT_NAME, WORKFLOW_ID, miniWorkflow)
    refreshTest {
      workspace.readTasks[Workflow](PROJECT_NAME).headOption.map(_._2) shouldBe Some(miniWorkflow)
    }
  }

  it should "read and write Custom tasks" in {
    workspace.putTask(PROJECT_NAME, CUSTOM_TASK_ID, customTask)
    refreshTest {
      workspace.readTasks[CustomTask](PROJECT_NAME).headOption.map(_._2) shouldBe Some(customTask)
    }
  }

  it should "delete custom tasks" in {
    refreshProject(PROJECT_NAME)
    workspace.readTasks[CustomTask](PROJECT_NAME).headOption shouldBe defined
    workspace.deleteTask[CustomTask](PROJECT_NAME, CUSTOM_TASK_ID)
    refreshTest {
      workspace.readTasks[CustomTask](PROJECT_NAME).headOption shouldBe empty
    }
  }

  it should "delete workflow tasks" in {
    refreshProject(PROJECT_NAME)
    workspace.readTasks[Workflow](PROJECT_NAME).headOption shouldBe defined
    workspace.deleteTask[Workflow](PROJECT_NAME, WORKFLOW_ID)
    refreshTest {
      workspace.readTasks[Workflow](PROJECT_NAME).headOption shouldBe empty
    }
  }

  it should "delete linking tasks" in {
    workspace.readTasks[LinkSpec](PROJECT_NAME).headOption.map(_._2) shouldBe Some(linkTask)
    refreshProject(PROJECT_NAME)
    workspace.readTasks[LinkSpec](PROJECT_NAME).headOption shouldBe defined
    workspace.deleteTask[LinkSpec](PROJECT_NAME, LINKING_TASK_ID)
    refreshTest {
      workspace.readTasks[LinkSpec](PROJECT_NAME).headOption shouldBe empty
    }
  }

  it should "delete transform tasks" in {
    refreshProject(PROJECT_NAME)
    workspace.readTasks[TransformSpec](PROJECT_NAME).headOption shouldBe defined
    workspace.deleteTask[TransformSpec](PROJECT_NAME, TRANSFORM_ID)
    refreshTest {
      workspace.readTasks[TransformSpec](PROJECT_NAME).headOption shouldBe empty
    }
  }

  it should "delete dataset tasks" in {
    refreshProject(PROJECT_NAME)
    workspace.readTasks[Dataset](PROJECT_NAME).headOption shouldBe defined
    workspace.deleteTask[Dataset](PROJECT_NAME, DATASET_ID)
    refreshTest {
      workspace.readTasks[Dataset](PROJECT_NAME).headOption shouldBe empty
    }
  }

  it should "delete projects" in {
    refreshProject(PROJECT_NAME)
    workspace.readProjects().size shouldBe 2
    workspace.deleteProject(PROJECT_NAME)
    refreshTest {
      workspace.readProjects().size shouldBe 1
    }
  }

  it should "manage project resources separately and correctly" in {
    workspace.readProjects().size shouldBe 1
    createProject(PROJECT_NAME)
    workspace.readProjects().size shouldBe 2
    val res1 = repository.get(PROJECT_NAME)
    val res2 = repository.get(PROJECT_NAME_OTHER)
    res1 should not be theSameInstanceAs (res2)
    val child1 = res1.get(CHILD)
    child1.write("content")
    intercept[ResourceNotFoundException] {
      res2.get(CHILD, mustExist = true)
    }
    val child1Other = res2.get(CHILD)
    child1 should not be theSameInstanceAs (child1Other)
    val res1Again = repository.get(PROJECT_NAME)
    res1Again should be theSameInstanceAs res1
  }

  /** Executes the block before and after project refresh */
  private def withRefresh(projectName: String)(ex: => Unit): Unit = {
    ex
    refreshProject(projectName)
    ex
  }

  /** Refreshes the project in the workspace, which usually means that it is reloaded from wherever its stored.
    * This should make sure that not only the possible cache version is up to date, but also the background model. */
  private def refreshProject(projectName: String): Unit = {
    workspace match {
      case w: RefreshableWorkspaceProvider =>
        w.refresh()
      case _ =>
        // Not refreshable
    }
  }
}

case class TestCustomTask(stringParam: String, numberParam: Int) extends CustomTask {
  override def inputSchemataOpt: Option[Seq[EntitySchema]] = None
  override def outputSchemaOpt: Option[EntitySchema] = None
}