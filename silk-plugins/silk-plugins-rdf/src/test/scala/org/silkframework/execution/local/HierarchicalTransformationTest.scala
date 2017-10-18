package org.silkframework.execution.local

import java.io.StringWriter

import com.hp.hpl.jena.rdf.model.{Model, ModelFactory}
import org.apache.jena.riot.{RDFDataMgr, RDFLanguages}
import org.scalatest.{FlatSpec, ShouldMatchers}
import org.silkframework.config.Prefixes
import org.silkframework.dataset.{Dataset, DatasetTask}
import org.silkframework.dataset.rdf.SparqlParams
import org.silkframework.entity.{BackwardOperator, Path}
import org.silkframework.execution.ExecutionReport
import org.silkframework.plugins.dataset.rdf.endpoint.JenaModelEndpoint
import org.silkframework.plugins.dataset.rdf.{FileDataset, JenaModelDataset, SparqlSink}
import org.silkframework.rule._
import org.silkframework.rule.execution.ExecuteTransform
import org.silkframework.rule.input.{PathInput, TransformInput}
import org.silkframework.rule.plugins.transformer.combine.ConcatTransformer
import org.silkframework.runtime.activity.{Activity, ActivityMonitor}
import org.silkframework.runtime.resource.{ClasspathResourceLoader, ReadOnlyResourceManager}
import org.silkframework.workspace._
import org.silkframework.workspace.activity.workflow._
import org.silkframework.workspace.resources.InMemoryResourceRepository

/**
  * Tests hierarchical mappings.
  */
class HierarchicalTransformationTest extends FlatSpec with ShouldMatchers {

  behavior of "Executor for hierarchical mappings"

  private val flatToNested =
    MappingTestCase(
      inputResource = "flatToNestedInput.ttl",
      outputResource = "flatToNestedOutput.ttl",
      transform =
        TransformSpec(
          selection = DatasetSelection("id", uri("Person")),
          mappingRule = RootMappingRule("root",
            MappingRules(
              uriRule = None,
              typeRules = Seq(TypeMapping(typeUri = uri("Person"))),
              propertyRules = Seq(
                DirectMapping(sourcePath = Path(uri("name")), mappingTarget = MappingTarget(uri("name"))),
                ObjectMapping(
                  sourcePath = Path.empty,
                  target = Some(MappingTarget(uri("address"))),
                  rules = MappingRules(
                    uriRule = Some(PatternUriMapping(pattern = s"https://silkframework.org/ex/Address_{<${uri("city")}>}_{<${uri("country")}>")),
                    typeRules = Seq.empty,
                    propertyRules = Seq(
                      DirectMapping(sourcePath = Path(uri("city")), mappingTarget = MappingTarget(uri("city"))),
                      DirectMapping(sourcePath = Path(uri("country")), mappingTarget = MappingTarget(uri("country")))
                    )
                  )
                )
            )
          )
        )
      )
    )

  private val nestedToFlat = {
    MappingTestCase(
      inputResource = "nestedToFlatInput.ttl",
      outputResource = "nestedToFlatOutput.ttl",
      transform =
        TransformSpec(
          selection = DatasetSelection("id", uri("Person")),
          mappingRule = RootMappingRule("root",
            MappingRules(
              uriRule = None,
              typeRules = Seq(TypeMapping(typeUri = uri("Person"))),
              propertyRules = Seq(
                DirectMapping(sourcePath = Path(uri("name")), mappingTarget = MappingTarget(uri("name"))),
                ObjectMapping(
                  sourcePath = Path(uri("address")),
                  target = None,
                  rules = MappingRules(
                    ComplexMapping(operator = PathInput(path = Path(BackwardOperator(uri("address")) :: Nil)), target = None),
                    ComplexMapping(
                      operator =
                        TransformInput(
                          transformer = ConcatTransformer("-"),
                          inputs = Seq(
                            PathInput(path = Path(uri("city"))),
                            PathInput(path = Path(uri("country")))
                          )
                        ),
                      target = Some(MappingTarget(uri("address"))))
                  )
                )
              )
            )
          )
        )
    )
  }

  it should "transform flat to nested structures when executed directly" in {
    executeDirect(flatToNested)
  }

  it should "transform nested to flat structures when executed directly" in {
    executeDirect(nestedToFlat)
  }

  it should "transform flat to nested structures when executed in a workflow" in {
    executeUsingWorkflow(flatToNested)
  }

  it should "transform nested to flat structures when executed in a workflow" in {
    executeUsingWorkflow(nestedToFlat)
  }

  private lazy val resources = ReadOnlyResourceManager(ClasspathResourceLoader(getClass.getPackage.getName.replace('.', '/')))

  private def executeDirect(test: MappingTestCase): Unit = {
    val source = FileDataset(resources.get(test.inputResource), "Turtle")
    val targetModel = createModel
    val endpoint = new JenaModelEndpoint(targetModel)
    val dataSink = new SparqlSink(SparqlParams(), endpoint)

    val executor = new ExecuteTransform(source.source, test.transform, Seq(dataSink))
    Activity(executor).startBlocking()

    compareWithExpected(targetModel, test.outputResource)
  }

  private def executeUsingWorkflow(test: MappingTestCase): Unit = {
    val project = createProject
    val targetModel = createModel
    val workflow = createWorkflow(project, FileDataset(resources.get(test.inputResource), "Turtle"), JenaModelDataset(targetModel), test.transform)
    executeWorkflow(workflow)
    compareWithExpected(targetModel, test.outputResource)
  }

  private def createModel: Model = {
    val targetModel = ModelFactory.createDefaultModel()
    targetModel.setNsPrefix("", "https://silkframework.org/ex/")
    targetModel
  }

  private def createProject: Project = {
    val workspace = new Workspace(InMemoryWorkspaceProvider(), InMemoryResourceRepository())
    workspace.createProject(ProjectConfig())
  }

  private def createWorkflow(project: Project, inputDataset: Dataset, outputDataset: Dataset, transform: TransformSpec): ProjectTask[Workflow] = {
    // Add tasks to project
    project.addTask("inputDataset", inputDataset)
    project.addTask("outputDataset", outputDataset)
    project.addTask("transform", transform)

    // Create workflow
    val inputNode = WorkflowDataset(Seq.empty, "inputDataset", Seq("transform"), (0,0), "inputDataset", None)
    val outputNode = WorkflowDataset(Seq("transform"), "outputDataset", Seq.empty, (0,0),  "outputDataset", None)
    val transformNode = WorkflowOperator(Seq("inputDataset"), "transform", Seq("outputDataset"), Seq.empty, (0,0), "transform", None)
    val workflow = Workflow(Seq(transformNode), Seq(inputNode, outputNode))
    project.addTask("workflow", workflow)

    project.task[Workflow]("workflow")
  }

  private def executeWorkflow(workflow: ProjectTask[Workflow]): Unit = {
    val executor = LocalWorkflowExecutor(workflow)
    val monitor = new ActivityMonitor[WorkflowExecutionReport]("monitor")
    monitor.value() = WorkflowExecutionReport()
    executor.run(monitor)
  }

  private def compareWithExpected(model: Model, expectedResource: String): Unit = {
    val expectedModel = createModel
    RDFDataMgr.read(expectedModel, resources.get(expectedResource).inputStream, RDFLanguages.resourceNameToLang(expectedResource))

    if(!model.isIsomorphicWith(expectedModel)) {
      val stringWriter = new StringWriter()
      model.write(stringWriter, "TURTLE")
      fail("Generate data is different from expected data. Got:\n" + stringWriter.toString)
    }
  }

  private def uri(name: String) = "https://silkframework.org/ex/" + name

  private case class MappingTestCase(inputResource: String, outputResource: String, transform: TransformSpec)

}
