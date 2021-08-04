package org.silkframework.workspace

import org.scalatest.{FlatSpec, Matchers}
import org.scalatestplus.mockito.MockitoSugar
import org.silkframework.config._
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.dataset.{DatasetSpec, MockDataset}
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.entity.{EntitySchema, Restriction}
import org.silkframework.rule._
import org.silkframework.rule.input.PathInput
import org.silkframework.rule.plugins.distance.characterbased.QGramsMetric
import org.silkframework.rule.similarity.Comparison
import org.silkframework.runtime.activity.{SimpleUserContext, UserContext}
import org.silkframework.runtime.plugin.PluginRegistry
import org.silkframework.runtime.plugin.annotations.Plugin
import org.silkframework.runtime.resource.ResourceNotFoundException
import org.silkframework.runtime.users.DefaultUserManager
import org.silkframework.util.{Identifier, Uri}
import org.silkframework.workspace.activity.workflow.{Workflow, WorkflowDataset, WorkflowOperator}
import org.silkframework.workspace.resources.InMemoryResourceRepository

trait WorkspaceProviderTestTrait extends FlatSpec with Matchers with MockitoSugar {

  val PROJECT_NAME = "ProjectName"
  val PROJECT_NAME_OTHER = "ProjectNameOther"
  val CHILD = "child"
  val WORKFLOW_ID = "workflow1"
  val TRANSFORM_ID = "transform1"
  val DATASET_ID = "dataset1"
  val LINKING_TASK_ID = "linking1"
  val CUSTOM_TASK_ID = "custom1"
  val NEW_PREFIX = "newPrefix"
  val DUMMY_DATASET = "dummy"

  // Assume that tested workspace provider have no enabled authentication
  val emptyUserContext: UserContext = UserContext.Empty

  val dummyDataset = MockDataset()

  PluginRegistry.registerPlugin(classOf[TestCustomTask])

  def createWorkspaceProvider(): WorkspaceProvider

  private def refreshTest(ex: => Unit)(implicit userContext: UserContext) = withRefresh(PROJECT_NAME)(ex)

  val workspaceProvider: WorkspaceProvider = createWorkspaceProvider()

  private val repository = InMemoryResourceRepository()

  private val workspace = new Workspace(workspaceProvider, repository)

  private val projectResources = repository.get(PROJECT_NAME)

  private def project(implicit userContext: UserContext) = workspace.project(PROJECT_NAME)

  val rule =
    LinkageRule(
      operator = Some(
        Comparison(
          id = "compareNames",
          threshold = 0.1,
          metric = QGramsMetric(q = 3),
          inputs = PathInput("foafName", UntypedPath("http://xmlns.com/foaf/0.1/name")) ::
              PathInput("foafName2", UntypedPath("http://xmlns.com/foaf/0.1/name")) :: Nil
        )
      ),
      linkType = "http://www.w3.org/2002/07/owl#sameAs"
    )

  val metaData =
    MetaData(
      label = "Task Label",
      description = Some("Some Task Description")
    )

  val metaDataUpdated =
    MetaData(
      label = "Updated Task Label",
      description = Some("Updated Task Description")
    )

  val applicationData = "Some Data"

  val applicationDataUpdated = "Updated Data"

  val dataset = PlainTask(DATASET_ID, DatasetSpec(MockDataset("default")), metaData = MetaData(DATASET_ID, Some(DATASET_ID + " description")))

  val datasetUpdated = PlainTask(DATASET_ID, DatasetSpec(MockDataset("updated"), uriAttribute = Some("uri")), metaData = MetaData(DATASET_ID))

  private val dummyType = "urn:test:dummyType"
  private val dummyRestriction = Restriction.custom("  ?a <urn:test:prop1> 1 .\n\n  ?a <urn:test:prop2> true .\n")(Prefixes.default)

  val linkSpec = LinkSpec(rule = rule, source = DatasetSelection(DUMMY_DATASET, dummyType, dummyRestriction), target = DatasetSelection(DUMMY_DATASET, dummyType, dummyRestriction),
    linkLimit = LinkSpec.DEFAULT_LINK_LIMIT + 1, matchingExecutionTimeout = LinkSpec.DEFAULT_EXECUTION_TIMEOUT_SECONDS + 1, applicationData = Some(applicationData))

  val linkTask = PlainTask(LINKING_TASK_ID, linkSpec, metaData)

  val linkTaskUpdated = PlainTask(LINKING_TASK_ID, LinkSpec(rule = rule.copy(operator = None), applicationData = Some(applicationDataUpdated)), metaDataUpdated)

  val transformTask =
    PlainTask(
      id = TRANSFORM_ID,
      data =
        TransformSpec(
          selection = DatasetSelection(DATASET_ID, "http://type1", dummyRestriction),
          mappingRule =
            RootMappingRule(
              id = "root",
              rules =
                MappingRules(DirectMapping(
                  id = TRANSFORM_ID,
                  sourcePath = UntypedPath("prop1"),
                  metaData = MetaData("Direct Rule Label", Some("Direct Rule Description"))
                )),
              metaData = MetaData("Root Rule Label", Some("Root Rule Description")))
        ),
      metaData = metaData
  )

  val transformTaskUpdated =
    PlainTask(
      id = TRANSFORM_ID,
      data = transformTask.data.copy(
        mappingRule =
          RootMappingRule(
            id = "root",
            rules =
              MappingRules(DirectMapping(
                id = TRANSFORM_ID + 2,
                sourcePath = UntypedPath("prop5"),
                metaData = MetaData("Direct Rule New Label", Some("Direct Rule New Description"))
              )),
            mappingTarget = transformTask.data.mappingRule.mappingTarget.copy(isAttribute = true),
            metaData = MetaData("Root Rule New Label", Some("Root Rule New Description"))
          ),
       abortIfErrorsOccur = true
      ),
      metaData = metaDataUpdated
    )

  val transformTaskHierarchical =
    PlainTask(
      id = TRANSFORM_ID,
      data =
        TransformSpec(
          selection = DatasetSelection(DATASET_ID, "Person"),
          mappingRule = RootMappingRule(
            MappingRules(
              uriRule = None,
              typeRules = Seq(TypeMapping(typeUri = "Person", metaData = MetaData("type"))),
              propertyRules = Seq(
                DirectMapping("name", sourcePath = UntypedPath("name"), mappingTarget = MappingTarget("name"), MetaData("name")),
                ObjectMapping(
                  sourcePath = UntypedPath.empty,
                  target = Some(MappingTarget("address")),
                  rules = MappingRules(
                    uriRule = Some(PatternUriMapping(pattern = s"https://silkframework.org/ex/Address_{city}_{country}", metaData = MetaData("uri"))),
                    typeRules = Seq.empty,
                    propertyRules = Seq(
                      DirectMapping("city", sourcePath = UntypedPath("city"), mappingTarget = MappingTarget("city"), MetaData("city")),
                      DirectMapping("country", sourcePath = UntypedPath("country"), mappingTarget = MappingTarget("country"), MetaData("country"))
                    )
                  ),
                  metaData = MetaData("object")
                )
              )
            ),
            metaData = MetaData("root")
          )
        ),
      metaData = metaData
    )

  val miniWorkflow =
    PlainTask(
      id = WORKFLOW_ID,
      data =
        Workflow(
          operators = Seq(
            WorkflowOperator(inputs = Seq(DATASET_ID), task = TRANSFORM_ID, outputs = Seq(), Seq(), (0, 0), TRANSFORM_ID, None, configInputs = Seq.empty)
          ),
          datasets = Seq(
            WorkflowDataset(Seq(), DATASET_ID, Seq(TRANSFORM_ID), (1,2), DATASET_ID, Some(1.0), configInputs = Seq.empty)
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

  private val specificUserUri = "http://specificUser"
  private val specificUserUri2 = "http://specificUser2"
  private val specificUserContext: UserContext = SimpleUserContext(Some(DefaultUserManager.get(specificUserUri)))
  private val specificUserContext2: UserContext = SimpleUserContext(Some(DefaultUserManager.get(specificUserUri2)))


  private def checkCreationMetaData(metaData: MetaData, originalMetaData: MetaData, userContext: UserContext): Unit = {
    metaData.modified should not be empty
    metaData.created should not be empty
    val expectedUserUri = userContext.user.map(u => Uri(u.uri))
    val expectedWithoutDateTime = originalMetaData.copy(created = None, modified = None,
      createdByUser = expectedUserUri, lastModifiedByUser = expectedUserUri)
    val currentWithoutDateTime = metaData.copy(created = None, modified = None)
    currentWithoutDateTime shouldBe expectedWithoutDateTime
  }

  private def checkUpdateMetaData(metaData: MetaData, originalMetaData: MetaData, creationUserContext: UserContext, updateUserContext: UserContext): Unit = {
    metaData.modified should not be empty
    metaData.created should not be empty
    metaData.modified.get should be > metaData.created.get
    metaData.copy(modified = None) shouldBe originalMetaData.copy(modified = None,
      createdByUser = creationUserContext.user.map(u => Uri(u.uri)), lastModifiedByUser = updateUserContext.user.map(u => Uri(u.uri)))
  }

  it should "read and write projects" in {
    val projectConfig2 = createProject(PROJECT_NAME_OTHER)(emptyUserContext)
    val projectConfig = createProject(PROJECT_NAME)(specificUserContext)
    val project = getProject(projectConfig.id).get
    val project2 = getProject(projectConfig2.id).get
    project.prefixes shouldBe projectConfig.prefixes
    project.projectResourceUriOpt shouldBe Some(projectConfig.generateDefaultUri)
    checkCreationMetaData(project.metaData, projectConfig.copy(projectResourceUriOpt = Some(projectConfig.generateDefaultUri)).metaData, specificUserContext)
    checkCreationMetaData(project2.metaData, projectConfig2.copy(projectResourceUriOpt = Some(projectConfig2.generateDefaultUri)).metaData, emptyUserContext)
  }

  it should "read and write project meta data" in {
    implicit val us: UserContext = specificUserContext2
    Thread.sleep(2) // Wait shortly, so modified time is different than creation time
    val projectLabel = "named project"
    val projectDescription = "project description"
    val newMetaData = MetaData(projectLabel, description = Some(projectDescription))
    val originalMetaData = project.config.metaData
    workspace.updateProjectMetaData(PROJECT_NAME, newMetaData)
    refreshTest {
      checkUpdateMetaData(project.config.metaData, originalMetaData.copy(label = projectLabel, description = Some(projectDescription)), specificUserContext, specificUserContext2)
    }
  }

  private def getProject(projectId: String): Option[ProjectConfig] = {
    implicit val us: UserContext = emptyUserContext
    workspaceProvider.readProjects().find(_.id == Identifier(projectId))
  }

  private def createProject(projectName: String)
                           (implicit userContext: UserContext): ProjectConfig = {
    val project = ProjectConfig(projectName, metaData = MetaData(projectName))
    workspace.createProject(project)(userContext)
    project
  }

  it should "read and write dataset tasks" in {
    implicit val us: UserContext = emptyUserContext
    PluginRegistry.registerPlugin(classOf[MockDataset])
    project.addTask[GenericDatasetSpec](DUMMY_DATASET, DatasetSpec(dummyDataset))
    workspaceProvider.putTask(PROJECT_NAME, dataset)
    refreshTest {
      val tasks = workspaceProvider.readTasks[GenericDatasetSpec](PROJECT_NAME, projectResources)
      val ds = tasks.find(_.id.toString == DATASET_ID).get
      ds shouldBe dataset
    }
  }

  it should "update dataset tasks" in {
    implicit val us: UserContext = emptyUserContext
    workspaceProvider.putTask(PROJECT_NAME, datasetUpdated)
    refreshTest {
      val ds = workspaceProvider.readTasks[GenericDatasetSpec](PROJECT_NAME, projectResources).find(_.id.toString == DATASET_ID).get
      ds shouldBe datasetUpdated
    }
  }

  it should "read and write linking tasks" in {
    implicit val us: UserContext = specificUserContext
    project.addTask[LinkSpec](LINKING_TASK_ID, linkSpec, metaData)
    refreshTest {
      val linkingTask = workspaceProvider.readTasks[LinkSpec](PROJECT_NAME, projectResources).headOption
      linkingTask shouldBe defined
      linkingTask.get.data shouldBe linkTask.data
      checkCreationMetaData(linkingTask.get.metaData, linkTask.metaData, specificUserContext)
    }
  }

  it should "update linking tasks" in {
    Thread.sleep(2) // So the modified time will be higher than the creation time
    implicit val us: UserContext = specificUserContext2
    val originalTask = project.anyTask(LINKING_TASK_ID)
    project.updateAnyTask(LINKING_TASK_ID, linkTaskUpdated.data, Some(linkTaskUpdated.metaData))
    refreshTest {
      val linkingTask = workspaceProvider.readTasks[LinkSpec](PROJECT_NAME, projectResources).headOption
      linkingTask shouldBe defined
      linkingTask.get.data shouldBe linkTaskUpdated.data
      checkUpdateMetaData(linkingTask.get.metaData, originalTask.metaData, specificUserContext, specificUserContext2)
    }
  }

  it should "read and write transformation tasks" in {
    implicit val us: UserContext = emptyUserContext
    workspaceProvider.putTask(PROJECT_NAME, transformTask)
    refreshTest {
      workspaceProvider.readTasks[TransformSpec](PROJECT_NAME, projectResources).headOption shouldBe Some(transformTask)
    }
  }

  it should "update transformation tasks" in {
    implicit val us: UserContext = emptyUserContext
    workspaceProvider.putTask(PROJECT_NAME, transformTaskUpdated)
    refreshTest {
      workspaceProvider.readTasks[TransformSpec](PROJECT_NAME, projectResources).headOption shouldBe Some(transformTaskUpdated)
    }
  }

  it should "remove meta data when a task is deleted" in {
    implicit val us: UserContext = emptyUserContext
    val newTransformTaskLabel = "newTransformTask"
    workspace.project(PROJECT_NAME).removeAnyTask(TRANSFORM_ID, removeDependentTasks = false)
    workspace.project(PROJECT_NAME).addTask[TransformSpec](TRANSFORM_ID, transformTaskUpdated.data, MetaData(newTransformTaskLabel))
    workspace.reload()
    val oldMetaData = transformTaskUpdated.metaData
    val newMetaData = workspace.project(PROJECT_NAME).anyTask(TRANSFORM_ID).metaData
    newMetaData.label shouldBe newTransformTaskLabel
    newMetaData.description should not be oldMetaData.description
    newMetaData.modified should not be oldMetaData.modified
  }

  it should "update hierarchical transformation tasks" in {
    implicit val us: UserContext = emptyUserContext
    workspaceProvider.putTask(PROJECT_NAME, transformTaskHierarchical)
    refreshTest {
      workspaceProvider.readTasks[TransformSpec](PROJECT_NAME, projectResources).headOption shouldBe Some(transformTaskHierarchical)
    }
  }


  it should "read and write workflows" in {
    implicit val us: UserContext = emptyUserContext
    workspaceProvider.putTask(PROJECT_NAME, miniWorkflow)
    refreshTest {
      workspaceProvider.readTasks[Workflow](PROJECT_NAME, projectResources).headOption shouldBe Some(miniWorkflow)
    }
  }

  it should "update workflow task correctly" in {
    implicit val us: UserContext = emptyUserContext
    workspaceProvider.putTask(PROJECT_NAME, miniWorkflowUpdated)
    refreshTest {
      workspaceProvider.readTasks[Workflow](PROJECT_NAME, projectResources).headOption shouldBe Some(miniWorkflowUpdated)
    }
  }

  it should "read and write Custom tasks" in {
    implicit val us: UserContext = emptyUserContext
    workspaceProvider.putTask(PROJECT_NAME, customTask)
    refreshTest {
      workspaceProvider.readTasks[CustomTask](PROJECT_NAME, projectResources).headOption shouldBe Some(customTask)
    }
  }

  it should "update meta data" in {
    implicit val us: UserContext = emptyUserContext
    val project = workspace.project(PROJECT_NAME)
    val linkingTask = project.task[LinkSpec](LINKING_TASK_ID)
    val label = "Linking Task 1"
    val description = "Description of linking task"
    val originalMetaData = linkingTask.metaData
    project.updateTask[LinkSpec](LINKING_TASK_ID, linkingTask, Some(MetaData(label, Some(description))))
    withWorkspaceRefresh(PROJECT_NAME) {
      val task = project.task[LinkSpec](LINKING_TASK_ID)
      checkUpdateMetaData(task.metaData, originalMetaData.copy(label = label, description = Some(description)), specificUserContext, emptyUserContext)
    }
  }

  it should "delete custom tasks" in {
    implicit val us: UserContext = emptyUserContext
    refreshProject(PROJECT_NAME)
    workspaceProvider.readTasks[CustomTask](PROJECT_NAME, projectResources).headOption shouldBe defined
    workspaceProvider.deleteTask[CustomTask](PROJECT_NAME, CUSTOM_TASK_ID)
    refreshTest {
      workspaceProvider.readTasks[CustomTask](PROJECT_NAME, projectResources).headOption shouldBe empty
    }
  }

  it should "delete workflow tasks" in {
    implicit val us: UserContext = emptyUserContext
    refreshProject(PROJECT_NAME)
    workspaceProvider.readTasks[Workflow](PROJECT_NAME, projectResources).headOption shouldBe defined
    workspaceProvider.deleteTask[Workflow](PROJECT_NAME, WORKFLOW_ID)
    refreshTest {
      workspaceProvider.readTasks[Workflow](PROJECT_NAME, projectResources).headOption shouldBe empty
    }
  }

  it should "delete linking tasks" in {
    implicit val us: UserContext = emptyUserContext
    workspaceProvider.readTasks[LinkSpec](PROJECT_NAME, projectResources).headOption shouldBe defined
    refreshProject(PROJECT_NAME)
    workspaceProvider.readTasks[LinkSpec](PROJECT_NAME, projectResources).headOption shouldBe defined
    workspaceProvider.deleteTask[LinkSpec](PROJECT_NAME, LINKING_TASK_ID)
    refreshTest {
      workspaceProvider.readTasks[LinkSpec](PROJECT_NAME, projectResources).headOption shouldBe empty
    }
  }

  it should "delete transform tasks" in {
    implicit val us: UserContext = emptyUserContext
    refreshProject(PROJECT_NAME)
    workspaceProvider.readTasks[TransformSpec](PROJECT_NAME, projectResources).headOption shouldBe defined
    workspaceProvider.deleteTask[TransformSpec](PROJECT_NAME, TRANSFORM_ID)
    refreshTest {
      workspaceProvider.readTasks[TransformSpec](PROJECT_NAME, projectResources).headOption shouldBe empty
    }
  }

  it should "delete dataset tasks" in {
    implicit val us: UserContext = emptyUserContext
    refreshProject(PROJECT_NAME)
    workspaceProvider.readTasks[GenericDatasetSpec](PROJECT_NAME, projectResources).headOption shouldBe defined
    workspaceProvider.deleteTask[GenericDatasetSpec](PROJECT_NAME, DATASET_ID)
    refreshTest {
      workspaceProvider.readTasks[GenericDatasetSpec](PROJECT_NAME, projectResources).map(_.id.toString) shouldBe Seq(DUMMY_DATASET)
    }
  }

  it should "delete projects" in {
    implicit val us: UserContext = emptyUserContext
    refreshProject(PROJECT_NAME)
    workspaceProvider.readProjects().size shouldBe 2
    workspace.removeProject(PROJECT_NAME)
//    workspaceProvider.deleteProject(PROJECT_NAME)
    refreshTest {
      workspaceProvider.readProjects().size shouldBe 1
    }
  }

  it should "manage project resources separately and correctly" in {
    implicit val us: UserContext = emptyUserContext
    workspaceProvider.readProjects().size shouldBe 1
    createProject(PROJECT_NAME)
    workspaceProvider.readProjects().size shouldBe 2
    val res1 = repository.get(PROJECT_NAME)
    val res2 = repository.get(PROJECT_NAME_OTHER)
    res1 should not be theSameInstanceAs (res2)
    val child1 = res1.get(CHILD)
    child1.writeString("content")
    intercept[ResourceNotFoundException] {
      res2.get(CHILD, mustExist = true)
    }
    val child1Other = res2.get(CHILD)
    child1 should not be theSameInstanceAs (child1Other)
    val res1Again = repository.get(PROJECT_NAME)
    res1Again should be theSameInstanceAs res1
  }

  it should "update prefixes" in {
    implicit val us: UserContext = emptyUserContext
    val projectOpt = getProject(PROJECT_NAME)
    projectOpt shouldBe defined
    val project = projectOpt.get
    project.prefixes.prefixMap.get(NEW_PREFIX) should not be defined
    // Add new prefix
    workspaceProvider.putProject(project.copy(prefixes = project.prefixes ++ Map(NEW_PREFIX -> "http://new_prefix")))
    val updatedProjectOpt = getProject(PROJECT_NAME)
    updatedProjectOpt shouldBe defined
    val updatedProject = updatedProjectOpt.get
    updatedProject.prefixes.prefixMap.get(NEW_PREFIX) shouldBe Some("http://new_prefix")
    // Change existing prefix
    workspaceProvider.putProject(project.copy(prefixes = project.prefixes ++ Map(NEW_PREFIX -> "http://new_prefix_updated")))
    val updatedProjectOpt2 = getProject(PROJECT_NAME)
    updatedProjectOpt2 shouldBe defined
    val updatedProject2 = updatedProjectOpt2.get
    updatedProject2.prefixes.prefixMap.get(NEW_PREFIX) shouldBe Some("http://new_prefix_updated")
  }

  /** Executes the block before and after project refresh */
  private def withRefresh(projectName: String)(ex: => Unit)(implicit userContext: UserContext): Unit = {
    ex
    refreshProject(projectName)
    ex
  }

  private def withWorkspaceRefresh(projectName: String)(ex: => Unit)(implicit userContext: UserContext): Unit = {
    ex
    workspace.reload()
    ex
  }

  /** Refreshes the project in the workspace, which usually means that it is reloaded from wherever its stored.
    * This should make sure that not only the possible cache version is up to date, but also the background model. */
  private def refreshProject(projectName: String)(implicit userContext: UserContext): Unit = {
    workspaceProvider.refresh()
  }
}

@Plugin(id = "test", label = "test task")
case class TestCustomTask(stringParam: String, numberParam: Int) extends CustomTask {
  override def inputSchemataOpt: Option[Seq[EntitySchema]] = None
  override def outputSchemaOpt: Option[EntitySchema] = None
}
