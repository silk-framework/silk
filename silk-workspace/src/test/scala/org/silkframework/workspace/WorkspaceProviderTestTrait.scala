package org.silkframework.workspace


import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.config._
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.dataset.{DatasetSpec, MockDataset}
import org.silkframework.entity.Restriction
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.plugins.dataset.csv.CsvDataset
import org.silkframework.rule._
import org.silkframework.rule.input.{PathInput, TransformInput}
import org.silkframework.rule.plugins.distance.characterbased.QGramsMetric
import org.silkframework.rule.plugins.transformer.combine.ConcatTransformer
import org.silkframework.rule.plugins.transformer.normalize.LowerCaseTransformer
import org.silkframework.rule.similarity.Comparison
import org.silkframework.runtime.activity.{SimpleUserContext, UserContext}
import org.silkframework.runtime.plugin.annotations.Plugin
import org.silkframework.runtime.plugin._
import org.silkframework.runtime.resource.ResourceNotFoundException
import org.silkframework.runtime.templating.{TemplateVariable, TemplateVariables}
import org.silkframework.runtime.users.DefaultUserManager
import org.silkframework.util.{Identifier, MockitoSugar, Uri}
import org.silkframework.workspace.WorkspaceProviderTestPlugins.{FailingCustomTask, FailingTaskException}
import org.silkframework.workspace.activity.workflow.{Workflow, WorkflowDataset, WorkflowOperator}
import org.silkframework.workspace.annotation.{StickyNote, UiAnnotations}
import org.silkframework.workspace.resources.InMemoryResourceRepository


trait WorkspaceProviderTestTrait extends AnyFlatSpec with Matchers with MockitoSugar with BeforeAndAfterAll {

  val PROJECT_NAME = "ProjectName"
  val PROJECT_NAME_OTHER = "ProjectNameOther"
  val CHILD = "child"
  val WORKFLOW_ID = "workflow1"
  val TRANSFORM_ID = "transform1"
  val DATASET_ID = "dataset1"
  val OUTPUTS_DATASET_ID = "outputDataset1"
  val LINKING_TASK_ID = "linking1"
  val CUSTOM_TASK_ID = "custom1"
  val NEW_PREFIX = "newPrefix"
  val DUMMY_DATASET = "dummy"
  val hierarchicalFileDatasetId = "hierarchicalFileDataset"

  // Assume that tested workspace provider have no enabled authentication
  val emptyUserContext: UserContext = UserContext.Empty

  private val dummyDataset = MockDataset()
  private lazy val fileBasedDatasetWithHierarchicalFilePath = CsvDataset(project(emptyUserContext).resources.getInPath("some/nested/file.csv"))

  PluginRegistry.unregisterPlugin(classOf[TestCustomTask])
  PluginRegistry.unregisterPlugin(classOf[FailingCustomTask])
  PluginRegistry.registerPlugin(classOf[TestCustomTask])
  PluginRegistry.registerPlugin(classOf[FailingCustomTask])

  def createWorkspaceProvider(workspacePrefixes: Prefixes): WorkspaceProvider

  private def refreshTest(ex: => Unit)(implicit userContext: UserContext): Unit = withRefresh(PROJECT_NAME)(ex)

  private val workspacePrefix = "initialWorkspacePrefix"
  val workspaceProvider: WorkspaceProvider = createWorkspaceProvider(Prefixes(Map(workspacePrefix -> "urn:initialPrefix:")))

  private val repository = InMemoryResourceRepository()

  private val workspace = new Workspace(workspaceProvider, repository)

  private val projectResources = repository.get(PROJECT_NAME)

  protected implicit val pluginContext: PluginContext = TestPluginContext(resources = projectResources)

  private def project(implicit userContext: UserContext) = workspace.project(PROJECT_NAME)

  private val rule =
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
      linkType = "http://www.w3.org/2002/07/owl#sameAs",
      inverseLinkType = Some("http://www.w3.org/2002/07/owl#sameAsInv"),
      excludeSelfReferences = true,
      layout = RuleLayout(
        nodePositions = Map("compareNames" -> NodePosition(1, 2))
      ),
      uiAnnotations = UiAnnotations(
        stickyNotes = Seq(StickyNote("compareNames", "content", "#fff", NodePosition(0, 0, 1, 1)))
      )
    )

  private val metaData =
    MetaData(
      label = Some("Task Label"),
      description = Some("Some Task Description")
    )

  private val metaDataUpdated =
    MetaData(
      label = Some("Updated Task Label"),
      description = Some("Updated Task Description")
    )

  private val dataset = PlainTask(DATASET_ID, DatasetSpec(MockDataset("default")), metaData = MetaData(Some(DATASET_ID), Some(DATASET_ID + " description")))

  val datasetUpdated = PlainTask(DATASET_ID, DatasetSpec(MockDataset("updated"), uriAttribute = Some("uri"), readOnly = true), metaData = MetaData(Some(DATASET_ID)))

  private val dummyType = "urn:test:dummyType"
  private val dummyRestriction = Restriction.custom("  ?a <urn:test:prop1> 1 .\n\n  ?a <urn:test:prop2> true .\n")(Prefixes.default)

  val linkSpec = LinkSpec(rule = rule, source = DatasetSelection(DUMMY_DATASET, dummyType, dummyRestriction), target = DatasetSelection(DUMMY_DATASET, dummyType, dummyRestriction),
    linkLimit = LinkSpec.DEFAULT_LINK_LIMIT + 1, matchingExecutionTimeout = LinkSpec.DEFAULT_EXECUTION_TIMEOUT_SECONDS + 1)

  val linkTask = PlainTask(LINKING_TASK_ID, linkSpec, metaData)

  val linkTaskUpdated = PlainTask(LINKING_TASK_ID, LinkSpec(rule = rule.copy(operator = None)), metaDataUpdated)

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
                MappingRules(
                  DirectMapping(
                    id = TRANSFORM_ID,
                    sourcePath = UntypedPath("prop1"),
                    metaData = MetaData(Some("Direct Rule Label"), Some("Direct Rule Description")),
                    inputId = Some("prop1")
                  ),
                  ComplexMapping(
                    id = "complexId",
                    operator = TransformInput("lower", transformer = LowerCaseTransformer(),
                      inputs = IndexedSeq(
                        TransformInput("concat", transformer = ConcatTransformer(),
                          inputs = IndexedSeq(
                            PathInput("path", UntypedPath.parse("path"))
                          )
                        )
                      )
                    ),
                    target = Some(MappingTarget(Uri("urn:complex:target"))),
                    layout = RuleLayout(
                      nodePositions = Map(
                        "lower" -> NodePosition(0, 1),
                        "concat" -> NodePosition(3, 4, Some(250)),
                        "path" -> NodePosition(5, 6, 250, 300)
                      )
                    ),
                    uiAnnotations = UiAnnotations(
                      stickyNotes = Seq(
                        StickyNote(
                          id = "stickyId",
                          content = "test",
                          color = "#000",
                          NodePosition(12,23, 32, 32)
                        )
                      )
                    )
                  )
                ),
              metaData = MetaData(Some("Root Rule Label"), Some("Root Rule Description")))
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
                metaData = MetaData(Some("Direct Rule New Label"), Some("Direct Rule New Description")),
                inputId = Some("prop5")
              )),
            mappingTarget = transformTask.data.mappingRule.mappingTarget.copy(isAttribute = true),
            metaData = MetaData(Some("Root Rule New Label"), Some("Root Rule New Description"))
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
              typeRules = Seq(TypeMapping(typeUri = "Person", metaData = MetaData(Some("type")))),
              propertyRules = Seq(
                DirectMapping("name", sourcePath = UntypedPath("name"), mappingTarget = MappingTarget("name"), MetaData(Some("name")), Some("name")),
                ObjectMapping(
                  sourcePath = UntypedPath.empty,
                  target = Some(MappingTarget("address")),
                  rules = MappingRules(
                    uriRule = Some(PatternUriMapping(pattern = s"https://silkframework.org/ex/Address_{city}_{country}", metaData = MetaData(Some("uri")))),
                    typeRules = Seq.empty,
                    propertyRules = Seq(
                      DirectMapping("city", sourcePath = UntypedPath("city"), mappingTarget = MappingTarget("city"), MetaData(Some("city")), Some("city")),
                      DirectMapping("country", sourcePath = UntypedPath("country"), mappingTarget = MappingTarget("country"), MetaData(Some("country")), Some("city"))
                    )
                  ),
                  metaData = MetaData(Some("object"))
                )
              )
            ),
            metaData = MetaData(Some("root"))
          )
        ),
      metaData = metaData
    )

  val miniWorkflow: PlainTask[Workflow] =
    PlainTask(
      id = WORKFLOW_ID,
      data =
        Workflow(
          operators = Seq(
            WorkflowOperator(inputs = Seq(Some(DATASET_ID)), task = TRANSFORM_ID, outputs = Seq(OUTPUTS_DATASET_ID), Seq(), (0, 0),
              TRANSFORM_ID, None, configInputs = Seq.empty, dependencyInputs = Seq.empty)
          ),
          datasets = Seq(
            WorkflowDataset(Seq(), DATASET_ID, Seq(TRANSFORM_ID), (1,2), DATASET_ID, Some(1.0), Seq.empty , dependencyInputs = Seq.empty),
            WorkflowDataset(Seq(None, Some(TRANSFORM_ID)), OUTPUTS_DATASET_ID, Seq(), (4,5), OUTPUTS_DATASET_ID, Some(0.5), Seq.empty , dependencyInputs = Seq.empty)
          ),
          uiAnnotations = UiAnnotations(
            stickyNotes = Seq(StickyNote("sticky1", "content", "#fff", NodePosition(0, 0, 1, 1)))
          ),
          replaceableInputs = Seq(DATASET_ID),
          replaceableOutputs = Seq(OUTPUTS_DATASET_ID)
        ),
      metaData = metaData
    )

  val miniWorkflowUpdated =
    PlainTask(
      id = WORKFLOW_ID,
      data = miniWorkflow.data.copy(
        operators = miniWorkflow.operators.map(op => op.copy(position = (100, 100), outputs = op.outputs ++ Seq(CUSTOM_TASK_ID))) ++ Seq(
          WorkflowOperator(inputs = Seq(), task = CUSTOM_TASK_ID, outputs = Seq(), Seq(), (0, 0),
            CUSTOM_TASK_ID, None, configInputs = Seq(TRANSFORM_ID), dependencyInputs = Seq(DATASET_ID))
        ),
        datasets = miniWorkflow.datasets.map(_.copy(position = (100, 100), dependencyInputs = Seq(CUSTOM_TASK_ID)))
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

  override def beforeAll(): Unit = {
    super.beforeAll()
    createProject(PROJECT_NAME)(specificUserContext)
  }

  it should "read and write projects" in {
    val projectConfig2 = createProject(PROJECT_NAME_OTHER)(emptyUserContext)
    val projectConfig = getProject(PROJECT_NAME).getOrElse(ProjectConfig("wrong"))
    val project = getProject(projectConfig.id).get
    val project2 = getProject(projectConfig2.id).get
    project.prefixes shouldBe projectConfig.prefixes
    project.projectResourceUriOpt shouldBe Some(projectConfig.generateDefaultUri)
    checkCreationMetaData(project.metaData, projectConfig.copy(projectResourceUriOpt = Some(projectConfig.generateDefaultUri)).metaData, specificUserContext)
    checkCreationMetaData(project2.metaData, projectConfig2.copy(projectResourceUriOpt = Some(projectConfig2.generateDefaultUri)).metaData, emptyUserContext)
  }

  it should "only persist project prefixes" in {
    val projectWithPrefixes = "projectWithPrefixes"
    val projectPrefix = "projectPrefix"
    val externalPrefix = "externalPrefix"
    val projectConfig = ProjectConfig(
      projectWithPrefixes,
      projectPrefixes = Prefixes(Map(
        projectPrefix -> "urn:ofProject:"
      )),
      // Usually not done this way, should only be added by the workspace provider
      workspacePrefixes = Prefixes(Map(
        externalPrefix -> "urn:external:"
      ))
    )
    projectConfig.prefixes.get(projectPrefix) shouldBe defined
    projectConfig.prefixes.get(externalPrefix) shouldBe defined
    // Only added when added to the workspace
    projectConfig.prefixes.get(workspacePrefix) should not be defined
    val project = workspace.createProject(projectConfig)(emptyUserContext)
    project.config.prefixes.get(projectPrefix) shouldBe defined
    project.config.prefixes.get(externalPrefix) should not be defined
    project.config.prefixes.get(workspacePrefix) shouldBe defined
    project.config.projectPrefixes.get(workspacePrefix) should not be defined
    workspaceProvider.putProject(project.config)(emptyUserContext)
    refreshProject(projectWithPrefixes)(emptyUserContext)
    val projectAfterRefresh = workspace.project(projectWithPrefixes)(emptyUserContext)
    projectAfterRefresh.config.prefixes.get(projectPrefix) shouldBe defined
    projectAfterRefresh.config.prefixes.get(externalPrefix) should not be defined
    projectAfterRefresh.config.prefixes.get(workspacePrefix) shouldBe defined
    // workspace prefixes should never make it into the project serialization and thus into the project prefixes
    projectAfterRefresh.config.projectPrefixes.get(workspacePrefix) should not be defined
  }

  it should "read and write project meta data" in {
    implicit val us: UserContext = specificUserContext2
    Thread.sleep(2) // Wait shortly, so modified time is different than creation time
    val projectLabel = Some("named project")
    val projectDescription = Some("project description")
    val newMetaData = MetaData(projectLabel, description = projectDescription)
    val originalMetaData = project.config.metaData
    project.updateMetaData(newMetaData)
    println(project.config.metaData)
    refreshTest {
      println(project.config.metaData)
      checkUpdateMetaData(project.config.metaData, originalMetaData.copy(label = projectLabel, description = projectDescription), specificUserContext, specificUserContext2)
    }
  }

  private def getProject(projectId: String): Option[ProjectConfig] = {
    implicit val us: UserContext = emptyUserContext
    workspaceProvider.readProjects().find(_.id == Identifier(projectId))
  }

  private def createProject(projectName: String)
                           (implicit userContext: UserContext): ProjectConfig = {
    val project = ProjectConfig(projectName, metaData = MetaData(Some(projectName)))
    workspace.createProject(project)(userContext)
    project
  }

  it should "read and write dataset tasks" in {
    implicit val us: UserContext = emptyUserContext
    PluginRegistry.registerPlugin(classOf[MockDataset])
    project.addTask[GenericDatasetSpec](DUMMY_DATASET, DatasetSpec(dummyDataset, readOnly = true))
    project.addTask[GenericDatasetSpec](hierarchicalFileDatasetId, DatasetSpec(fileBasedDatasetWithHierarchicalFilePath))
    val projectFileManager = project.resources
    workspaceProvider.putTask(PROJECT_NAME, dataset, projectFileManager)
    refreshTest {
      val tasks = workspaceProvider.readTasks[GenericDatasetSpec](PROJECT_NAME).map(_.task)
      val das = tasks.find(_.id.toString == DATASET_ID).get
      das shouldBe dataset
      val ds = tasks.find(_.id.toString == DUMMY_DATASET).get
      ds.data.plugin shouldBe dummyDataset
      val hs = tasks.find(_.id.toString == hierarchicalFileDatasetId).get
      val hsCsvDataset = hs.data.plugin.asInstanceOf[CsvDataset]
      hsCsvDataset.file.path shouldBe fileBasedDatasetWithHierarchicalFilePath.file.path
    }
  }

  it should "update dataset tasks" in {
    implicit val us: UserContext = emptyUserContext
    workspaceProvider.putTask(PROJECT_NAME, datasetUpdated, projectResources)
    refreshTest {
      val ds = workspaceProvider.readTasks[GenericDatasetSpec](PROJECT_NAME).find(_.task.id.toString == DATASET_ID).get.task
      ds shouldBe datasetUpdated
    }
  }

  it should "read and write linking tasks" in {
    implicit val us: UserContext = specificUserContext
    project.addTask[LinkSpec](LINKING_TASK_ID, linkSpec, metaData)
    refreshTest {
      val linkingTask = workspaceProvider.readTasks[LinkSpec](PROJECT_NAME).headOption
      linkingTask shouldBe defined
      linkingTask.get.task.data shouldBe linkTask.data
      checkCreationMetaData(linkingTask.get.task.metaData, linkTask.metaData, specificUserContext)
    }
  }

  it should "update linking tasks" in {
    Thread.sleep(2) // So the modified time will be higher than the creation time
    implicit val us: UserContext = specificUserContext2
    val originalTask = project.anyTask(LINKING_TASK_ID)
    project.updateAnyTask(LINKING_TASK_ID, linkTaskUpdated.data, Some(linkTaskUpdated.metaData))
    refreshTest {
      val linkingTask = workspaceProvider.readTasks[LinkSpec](PROJECT_NAME).headOption
      linkingTask shouldBe defined
      linkingTask.get.task.data shouldBe linkTaskUpdated.data
      checkUpdateMetaData(linkingTask.get.task.metaData, originalTask.metaData, specificUserContext, specificUserContext2)
    }
  }

  it should "read and write transformation tasks" in {
    implicit val us: UserContext = emptyUserContext
    workspaceProvider.putTask(PROJECT_NAME, transformTask, projectResources)
    refreshTest {
      workspaceProvider.readTasks[TransformSpec](PROJECT_NAME).headOption.map(_.task) shouldBe Some(transformTask)
    }
  }

  it should "update transformation tasks" in {
    implicit val us: UserContext = emptyUserContext
    workspaceProvider.putTask(PROJECT_NAME, transformTaskUpdated, projectResources)
    refreshTest {
      workspaceProvider.readTasks[TransformSpec](PROJECT_NAME).headOption.map(_.task) shouldBe Some(transformTaskUpdated)
    }
  }

  it should "remove meta data when a task is deleted" in {
    implicit val us: UserContext = emptyUserContext
    val newTransformTaskLabel = "newTransformTask"
    workspace.project(PROJECT_NAME).removeAnyTask(TRANSFORM_ID, removeDependentTasks = false)
    workspace.project(PROJECT_NAME).addTask[TransformSpec](TRANSFORM_ID, transformTaskUpdated.data, MetaData(Some(newTransformTaskLabel)))
    workspace.reload()
    val oldMetaData = transformTaskUpdated.metaData
    val newMetaData = workspace.project(PROJECT_NAME).anyTask(TRANSFORM_ID).metaData
    newMetaData.label shouldBe Some(newTransformTaskLabel)
    newMetaData.description should not be oldMetaData.description
    newMetaData.modified should not be oldMetaData.modified
  }

  it should "update hierarchical transformation tasks" in {
    implicit val us: UserContext = emptyUserContext
    workspaceProvider.putTask(PROJECT_NAME, transformTaskHierarchical, projectResources)
    refreshTest {
      workspaceProvider.readTasks[TransformSpec](PROJECT_NAME).headOption.map(_.task) shouldBe Some(transformTaskHierarchical)
    }
  }


  it should "read and write workflows" in {
    implicit val us: UserContext = emptyUserContext
    workspaceProvider.putTask(PROJECT_NAME, miniWorkflow, projectResources)
    refreshTest {
      workspaceProvider.readTasks[Workflow](PROJECT_NAME).headOption.map(_.task) shouldBe Some(miniWorkflow)
    }
  }

  it should "update workflow task correctly" in {
    implicit val us: UserContext = emptyUserContext
    workspaceProvider.putTask(PROJECT_NAME, miniWorkflowUpdated, projectResources)
    refreshTest {
      workspaceProvider.readTasks[Workflow](PROJECT_NAME).headOption.map(_.task) shouldBe Some(miniWorkflowUpdated)
    }
  }

  it should "read and write Custom tasks" in {
    implicit val us: UserContext = emptyUserContext
    workspaceProvider.putTask(PROJECT_NAME, customTask, projectResources)
    refreshTest {
      workspaceProvider.readTasks[CustomTask](PROJECT_NAME).headOption.map(_.task) shouldBe Some(customTask)
    }
  }

  it should "update meta data" in {
    implicit val us: UserContext = emptyUserContext
    val project = workspace.project(PROJECT_NAME)
    val linkingTask = project.task[LinkSpec](LINKING_TASK_ID)
    val label = "Linking Task 1"
    val description = "Description of linking task"
    val originalMetaData = linkingTask.metaData
    project.updateTask[LinkSpec](LINKING_TASK_ID, linkingTask, Some(MetaData(Some(label), Some(description))))
    withWorkspaceRefresh(PROJECT_NAME) {
      val task = project.task[LinkSpec](LINKING_TASK_ID)
      checkUpdateMetaData(task.metaData, originalMetaData.copy(label = Some(label), description = Some(description)), specificUserContext, emptyUserContext)
    }
  }

  it should "delete custom tasks" in {
    implicit val us: UserContext = emptyUserContext
    refreshProject(PROJECT_NAME)
    workspaceProvider.readTasks[CustomTask](PROJECT_NAME).headOption shouldBe defined
    workspaceProvider.deleteTask[CustomTask](PROJECT_NAME, CUSTOM_TASK_ID)
    refreshTest {
      workspaceProvider.readTasks[CustomTask](PROJECT_NAME).headOption shouldBe empty
    }
  }

  it should "delete workflow tasks" in {
    implicit val us: UserContext = emptyUserContext
    refreshProject(PROJECT_NAME)
    workspaceProvider.readTasks[Workflow](PROJECT_NAME).headOption shouldBe defined
    workspaceProvider.deleteTask[Workflow](PROJECT_NAME, WORKFLOW_ID)
    refreshTest {
      workspaceProvider.readTasks[Workflow](PROJECT_NAME).headOption shouldBe empty
    }
  }

  it should "delete linking tasks" in {
    implicit val us: UserContext = emptyUserContext
    workspaceProvider.readTasks[LinkSpec](PROJECT_NAME).headOption shouldBe defined
    refreshProject(PROJECT_NAME)
    workspaceProvider.readTasks[LinkSpec](PROJECT_NAME).headOption shouldBe defined
    workspaceProvider.deleteTask[LinkSpec](PROJECT_NAME, LINKING_TASK_ID)
    refreshTest {
      workspaceProvider.readTasks[LinkSpec](PROJECT_NAME).headOption shouldBe empty
    }
  }

  it should "delete transform tasks" in {
    implicit val us: UserContext = emptyUserContext
    refreshProject(PROJECT_NAME)
    workspaceProvider.readTasks[TransformSpec](PROJECT_NAME).headOption shouldBe defined
    workspaceProvider.deleteTask[TransformSpec](PROJECT_NAME, TRANSFORM_ID)
    refreshTest {
      workspaceProvider.readTasks[TransformSpec](PROJECT_NAME).headOption shouldBe empty
    }
  }

  it should "delete dataset tasks" in {
    implicit val us: UserContext = emptyUserContext
    refreshProject(PROJECT_NAME)
    workspaceProvider.readTasks[GenericDatasetSpec](PROJECT_NAME).headOption shouldBe defined
    workspaceProvider.deleteTask[GenericDatasetSpec](PROJECT_NAME, DATASET_ID)
    refreshTest {
      workspaceProvider.readTasks[GenericDatasetSpec](PROJECT_NAME).map(_.task.id.toString) shouldBe Seq(DUMMY_DATASET, hierarchicalFileDatasetId)
    }
  }

  it should "delete projects" in {
    implicit val us: UserContext = emptyUserContext
    refreshProject(PROJECT_NAME)
    workspaceProvider.readProjects().size shouldBe 3
    workspace.removeProject(PROJECT_NAME)
//    workspaceProvider.deleteProject(PROJECT_NAME)
    refreshTest {
      workspaceProvider.readProjects().size shouldBe 2
    }
  }

  it should "manage project resources separately and correctly" in {
    implicit val us: UserContext = emptyUserContext
    workspaceProvider.readProjects().size shouldBe 2
    createProject(PROJECT_NAME)
    workspaceProvider.readProjects().size shouldBe 3
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
    workspaceProvider.putProject(project.copy(projectPrefixes = project.projectPrefixes ++ Map(NEW_PREFIX -> "http://new_prefix")))
    val updatedProjectOpt = getProject(PROJECT_NAME)
    updatedProjectOpt shouldBe defined
    val updatedProject = updatedProjectOpt.get
    updatedProject.prefixes.prefixMap.get(NEW_PREFIX) shouldBe Some("http://new_prefix")
    // Change existing prefix
    workspaceProvider.putProject(project.copy(projectPrefixes = project.projectPrefixes ++ Map(NEW_PREFIX -> "http://new_prefix_updated")))
    val updatedProjectOpt2 = getProject(PROJECT_NAME)
    updatedProjectOpt2 shouldBe defined
    val updatedProject2 = updatedProjectOpt2.get
    updatedProject2.prefixes.prefixMap.get(NEW_PREFIX) shouldBe Some("http://new_prefix_updated")
  }

  it should "allow managing tags" in {
    implicit val us: UserContext = emptyUserContext

    // Make sure that initially the tags are empty
    workspaceProvider.readTags(PROJECT_NAME) shouldBe empty

    // Add tags and read them back
    val tag1 = Tag("urn:tag1", "Some Tag 1")
    val tag2 = Tag("urn:tag2", "Some Tag 2")
    workspaceProvider.putTag(PROJECT_NAME, tag1)
    workspaceProvider.putTag(PROJECT_NAME, tag2)
    refreshTest {
      workspaceProvider.readTags(PROJECT_NAME) should contain theSameElementsAs Iterable(tag1, tag2)
    }

    // Add tags to project
    val project = getProject(PROJECT_NAME).get
    val metaDataWithTags = project.metaData.copy(tags = Set("urn:tag1", "urn:tag2"))
    workspaceProvider.putProject(project.copy(metaData = metaDataWithTags))
    refreshTest {
      workspaceProvider.readProject(PROJECT_NAME).get.metaData shouldBe metaDataWithTags
    }

    // Modify tags on project
    val metaDataWithTags2 = project.metaData.copy(tags = Set("urn:tag1"))
    workspaceProvider.putProject(project.copy(metaData = metaDataWithTags2))
    refreshTest {
      workspaceProvider.readProject(PROJECT_NAME).get.metaData shouldBe metaDataWithTags2
    }

    // Add tags to a new task
    val taskWithTag = customTask.copy(
      metaData =
        MetaData(
          label = Some("Task Label"),
          tags = Set("urn:tag2", "urn:tag1")
        )
    )
    workspaceProvider.putTask(PROJECT_NAME, taskWithTag, projectResources)
    refreshTest {
      workspaceProvider.readTasks[CustomTask](PROJECT_NAME).headOption.map(_.task) shouldBe Some(taskWithTag)
    }

    // Remove tag
    workspaceProvider.deleteTag(PROJECT_NAME, tag1.uri)
    refreshTest {
      workspaceProvider.readTags(PROJECT_NAME) should contain theSameElementsAs Iterable(tag2)
    }
  }

  it should "allow managing project template variables" in {
    implicit val us: UserContext = emptyUserContext

    // Initially, it should return an empty variable list
    val variables = workspaceProvider.projectVariables(PROJECT_NAME)
    variables.readVariables().map shouldBe empty

    // Add variables and read again
    val templateVariables1 = TemplateVariables(Seq(
      TemplateVariable("myVar1", "myValue1", None, None, isSensitive = false, "project"),
      TemplateVariable("myVar2", "myValue2", None, Some("test description"), isSensitive = true, "project"),
      TemplateVariable("myVar3", "myValue2b", Some("{{project.myVar2}}b"), None, isSensitive = true, "project")
    ))
    variables.putVariables(templateVariables1)
    refreshTest {
      variables.readVariables() shouldBe templateVariables1
    }

    // Modify variables and read again
    val templateVariables2 = TemplateVariables(Seq(
      TemplateVariable("myVar2", "myValue2", None, Some("test description 2"), isSensitive = true, "project"),
      TemplateVariable("myVar4", "myValue2b", Some("{{project.myVar2}}b"), None, isSensitive = true, "project"),
      TemplateVariable("myVar1", "myValue1", None, None, isSensitive = false, "project")
    ))
    variables.putVariables(templateVariables2)
    refreshTest {
      variables.readVariables() shouldBe templateVariables2
    }
  }

  it should "load tasks safely and allow to fix them" in {
    implicit val us: UserContext = emptyUserContext
    val failingTaskId = "failingTask1"
    WorkspaceProviderTestPlugins.synchronized {
      try {
        WorkspaceProviderTestPlugins.failingCustomTaskFailing = false
        val failingCustomTask = PlainTask(
          id = failingTaskId,
          data = FailingCustomTask()
        )
        workspaceProvider.putTask(PROJECT_NAME, failingCustomTask, projectResources)
        refreshTest {
          workspaceProvider.readTasks[CustomTask](PROJECT_NAME).filter(_.id.toString == failingTaskId) shouldBe Seq(LoadedTask(Right(failingCustomTask)))
          workspaceProvider.readAllTasks(PROJECT_NAME).filter(_.task.id.toString == failingTaskId) shouldBe Seq(LoadedTask(Right(failingCustomTask)))
        }
        WorkspaceProviderTestPlugins.failingCustomTaskFailing = true
        // Refresh the workspace to make sure that the loading error comes from the workspace provider and not the import workspace provider (XML workspace)
        refreshTest {
          // Test that the loading error contains a factory function that creates a new instance of the task and the original parameters.
          def testLoadedTask(loadedTasks: Seq[LoadedTask[_]], shouldFail: Boolean): Unit = {
            val loadingError = loadedTasks.filter(_.error.isDefined)
            if(shouldFail) {
              loadingError should have size 1
              // Check factory function
              val factoryFunctionOpt = loadingError.head.error.get.factoryFunction
              factoryFunctionOpt shouldBe defined
              factoryFunctionOpt.get(ParameterValues(Map.empty), pluginContext).error
                .map(_.throwable.getCause).getOrElse(new RuntimeException()) shouldBe a[FailingTaskException]
              // Check that original parameters are included
              loadingError.head.error.get.originalParameterValues shouldBe Some(
                OriginalTaskData(
                  "failTask",
                  ParameterValues(Map("alwaysFail" -> ParameterStringValue("true")))
                )
              )
              // Test with fixing parameters
              factoryFunctionOpt.get(ParameterValues(Map("alwaysFail" -> ParameterStringValue("false"))), pluginContext).
                task.data shouldBe a[FailingCustomTask]
              WorkspaceProviderTestPlugins.failingCustomTaskFailing = false
              factoryFunctionOpt.get(ParameterValues(Map.empty), pluginContext).data shouldBe a[FailingCustomTask]
            } else {
              loadingError should have size 0
            }
          }
          testLoadedTask(workspaceProvider.readTasks[CustomTask](PROJECT_NAME), shouldFail = WorkspaceProviderTestPlugins.failingCustomTaskFailing)
          testLoadedTask(workspaceProvider.readAllTasks(PROJECT_NAME), shouldFail = WorkspaceProviderTestPlugins.failingCustomTaskFailing)
          WorkspaceProviderTestPlugins.failingCustomTaskFailing = false
        }
      } finally {
        WorkspaceProviderTestPlugins.failingCustomTaskFailing = false
      }
    }
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
    workspaceProvider.refreshProject(projectName, repository.get(projectName))
    if(workspace.findProject(projectName).isDefined) {
      workspace.reloadProject(projectName)
    }
  }
}

@Plugin(id = "WorkspaceProviderTestTask", label = "test task")
case class TestCustomTask(stringParam: String, numberParam: Int) extends CustomTask {
  override def inputPorts: InputPorts = FixedNumberOfInputs(Seq.empty)
  override def outputPort: Option[Port] = None
}

object WorkspaceProviderTestPlugins {
  class FailingTaskException(msg: String) extends RuntimeException(msg)
  /** Plugin to test task loading failures. */
  @volatile
  var failingCustomTaskFailing = false
  object failingCustomTaskLock

  @Plugin(id = "failTask", label = "Task that always fails loading")
  case class FailingCustomTask(alwaysFail: Boolean = true) extends CustomTask {
    if(failingCustomTaskFailing && alwaysFail) {
      throw new FailingTaskException("Failed!")
    }

    override def inputPorts: InputPorts = FixedNumberOfInputs(Seq.empty)

    override def outputPort: Option[Port] = None
  }

}
