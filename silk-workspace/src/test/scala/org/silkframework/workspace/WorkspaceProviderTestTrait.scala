package org.silkframework.workspace

import org.scalatest.{FlatSpec, ShouldMatchers}
import org.silkframework.config._
import org.silkframework.dataset.{Dataset, DatasetTask}
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
import org.silkframework.workspace.resources.{InMemoryResourceRepository, ResourceRepository}

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
  val NEW_PREFIX = "newPrefix"

  PluginRegistry.registerPlugin(classOf[TestCustomTask])

  def createWorkspaceProvider(): WorkspaceProvider

  val refreshTest = withRefresh(PROJECT_NAME)(_)

  private val workspace = createWorkspaceProvider()

  private val repository = InMemoryResourceRepository()

  private val projectResources = repository.get(PROJECT_NAME)

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

  val metaData =
    MetaData(
      label = "Some Label",
      description = "Some Description"
    )

  val metaDataUpdated =
    MetaData(
      label = "Updated Label",
      description = "Updated Description"
    )

  val dataset = new DatasetTask(DATASET_ID, InternalDataset("default"))

  val datasetUpdated = new DatasetTask(DATASET_ID, InternalDataset("updated"))

  val linkTask = PlainTask(LINKING_TASK_ID, LinkSpec(rule = rule), metaData)

  val linkTaskUpdated = PlainTask(LINKING_TASK_ID, LinkSpec(rule = rule.copy(operator = None)), metaDataUpdated)

  val transformTask =
    PlainTask(
      id = TRANSFORM_ID,
      data =
        TransformSpec(
          selection = DatasetSelection("InputDS", "http://type1"),
          mappingRule = RootMappingRule(MappingRules(
            DirectMapping(
              id = TRANSFORM_ID,
              sourcePath = Path("prop1"),
              metaData = metaData
            )
          ))
        ),
      metaData = metaData
  )

  val transformTaskUpdated =
    PlainTask(
      id = TRANSFORM_ID,
      data = transformTask.data.copy(mappingRule = RootMappingRule(MappingRules(
        DirectMapping(
          id = TRANSFORM_ID + 2,
          sourcePath = Path("prop5"),
          metaData = metaDataUpdated
        )
      ))),
      metaData = metaDataUpdated
    )

  val miniWorkflow =
    PlainTask(
      id = WORKFLOW_ID,
      data =
        Workflow(
          operators = Seq(
            WorkflowOperator(inputs = Seq(DATASET_ID), task = TRANSFORM_ID, outputs = Seq(), Seq(), (0, 0), TRANSFORM_ID, None)
          ),
          datasets = Seq(
            WorkflowDataset(Seq(), DATASET_ID, Seq(TRANSFORM_ID), (1,2), DATASET_ID, Some(1.0))
          )),
      metaData = metaData
    )

  val miniWorkflowUpdated =
    PlainTask(
      id = WORKFLOW_ID,
      data = miniWorkflow.data.copy(
        operators = miniWorkflow.operators.map(_.copy(position = (100, 100))),
        datasets = miniWorkflow.datasets.map(_.copy(position = (100, 100)))
      ),
      metaData = metaDataUpdated
    )

  val customTask =
    PlainTask(
      id = CUSTOM_TASK_ID,
      data = TestCustomTask(stringParam = "xxx", numberParam = 12),
      metaData = metaData
    )

  it should "read and write projects" in {
    val project = createProject(PROJECT_NAME)
    val project2 = createProject(PROJECT_NAME_OTHER)
    getProject(project.id) should be (Some(project.copy(projectResourceUriOpt = Some(project.generateDefaultUri))))
    getProject(project2.id) should be (Some(project2.copy(projectResourceUriOpt = Some(project2.generateDefaultUri))))
  }

  private def getProject(projectId: String): Option[ProjectConfig] = {
    workspace.readProjects().find(_.id == projectId)
  }

  private def createProject(projectName: String): ProjectConfig = {
    val project = ProjectConfig(projectName)
    workspace.putProject(project)
    project
  }

  it should "read and write linking tasks" in {
    workspace.putTask(PROJECT_NAME, linkTask)
    refreshTest {
      workspace.readTasks[LinkSpec](PROJECT_NAME, projectResources).headOption shouldBe Some(linkTask)
    }
  }

  it should "update linking tasks" in {
    workspace.putTask(PROJECT_NAME, linkTaskUpdated)
    refreshTest {
      workspace.readTasks[LinkSpec](PROJECT_NAME, projectResources).headOption shouldBe Some(linkTaskUpdated)
    }
  }

  it should "read and write dataset tasks" in {
    workspace.putTask(PROJECT_NAME, dataset)
    refreshTest {
      val ds = workspace.readTasks[Dataset](PROJECT_NAME, projectResources).headOption.get
      ds shouldBe dataset
    }
  }

  it should "update dataset tasks" in {
    workspace.putTask(PROJECT_NAME, datasetUpdated)
    refreshTest {
      val ds = workspace.readTasks[Dataset](PROJECT_NAME, projectResources).headOption.get
      ds shouldBe datasetUpdated
    }
  }

  it should "read and write transformation tasks" in {
    workspace.putTask(PROJECT_NAME, transformTask)
    refreshTest {
      workspace.readTasks[TransformSpec](PROJECT_NAME, projectResources).headOption shouldBe Some(transformTask)
    }
  }

  it should "update transformation tasks" in {
    workspace.putTask(PROJECT_NAME, transformTaskUpdated)
    refreshTest {
      workspace.readTasks[TransformSpec](PROJECT_NAME, projectResources).headOption shouldBe Some(transformTaskUpdated)
    }
  }

  it should "read and write workflows" in {
    workspace.putTask(PROJECT_NAME, miniWorkflow)
    refreshTest {
      workspace.readTasks[Workflow](PROJECT_NAME, projectResources).headOption shouldBe Some(miniWorkflow)
    }
  }

  it should "update workflow task correctly" in {
    workspace.putTask(PROJECT_NAME, miniWorkflowUpdated)
    refreshTest {
      workspace.readTasks[Workflow](PROJECT_NAME, projectResources).headOption shouldBe Some(miniWorkflowUpdated)
    }
  }

  it should "read and write Custom tasks" in {
    workspace.putTask(PROJECT_NAME, customTask)
    refreshTest {
      workspace.readTasks[CustomTask](PROJECT_NAME, projectResources).headOption shouldBe Some(customTask)
    }
  }

  it should "delete custom tasks" in {
    refreshProject(PROJECT_NAME)
    workspace.readTasks[CustomTask](PROJECT_NAME, projectResources).headOption shouldBe defined
    workspace.deleteTask[CustomTask](PROJECT_NAME, CUSTOM_TASK_ID)
    refreshTest {
      workspace.readTasks[CustomTask](PROJECT_NAME, projectResources).headOption shouldBe empty
    }
  }

  it should "delete workflow tasks" in {
    refreshProject(PROJECT_NAME)
    workspace.readTasks[Workflow](PROJECT_NAME, projectResources).headOption shouldBe defined
    workspace.deleteTask[Workflow](PROJECT_NAME, WORKFLOW_ID)
    refreshTest {
      workspace.readTasks[Workflow](PROJECT_NAME, projectResources).headOption shouldBe empty
    }
  }

  it should "delete linking tasks" in {
    workspace.readTasks[LinkSpec](PROJECT_NAME, projectResources).headOption shouldBe Some(linkTaskUpdated)
    refreshProject(PROJECT_NAME)
    workspace.readTasks[LinkSpec](PROJECT_NAME, projectResources).headOption shouldBe defined
    workspace.deleteTask[LinkSpec](PROJECT_NAME, LINKING_TASK_ID)
    refreshTest {
      workspace.readTasks[LinkSpec](PROJECT_NAME, projectResources).headOption shouldBe empty
    }
  }

  it should "delete transform tasks" in {
    refreshProject(PROJECT_NAME)
    workspace.readTasks[TransformSpec](PROJECT_NAME, projectResources).headOption shouldBe defined
    workspace.deleteTask[TransformSpec](PROJECT_NAME, TRANSFORM_ID)
    refreshTest {
      workspace.readTasks[TransformSpec](PROJECT_NAME, projectResources).headOption shouldBe empty
    }
  }

  it should "delete dataset tasks" in {
    refreshProject(PROJECT_NAME)
    workspace.readTasks[Dataset](PROJECT_NAME, projectResources).headOption shouldBe defined
    workspace.deleteTask[Dataset](PROJECT_NAME, DATASET_ID)
    refreshTest {
      workspace.readTasks[Dataset](PROJECT_NAME, projectResources).headOption shouldBe empty
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

  it should "update prefixes" in {
    val projectOpt = getProject(PROJECT_NAME)
    projectOpt shouldBe defined
    val project = projectOpt.get
    project.prefixes.prefixMap.get(NEW_PREFIX) should not be defined
    // Add new prefix
    workspace.putProject(project.copy(prefixes = project.prefixes ++ Map(NEW_PREFIX -> "http://new_prefix")))
    val updatedProjectOpt = getProject(PROJECT_NAME)
    updatedProjectOpt shouldBe defined
    val updatedProject = updatedProjectOpt.get
    updatedProject.prefixes.prefixMap.get(NEW_PREFIX) shouldBe Some("http://new_prefix")
    // Change existing prefix
    workspace.putProject(project.copy(prefixes = project.prefixes ++ Map(NEW_PREFIX -> "http://new_prefix_updated")))
    val updatedProjectOpt2 = getProject(PROJECT_NAME)
    updatedProjectOpt2 shouldBe defined
    val updatedProject2 = updatedProjectOpt2.get
    updatedProject2.prefixes.prefixMap.get(NEW_PREFIX) shouldBe Some("http://new_prefix_updated")
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