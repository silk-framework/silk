package org.silkframework.execution.local

import java.io.StringWriter

import com.hp.hpl.jena.rdf.model.ModelFactory
import org.apache.jena.riot.{RDFDataMgr, RDFLanguages}
import org.scalatest.{FlatSpec, ShouldMatchers}
import org.silkframework.dataset.rdf.SparqlParams
import org.silkframework.entity.{BackwardOperator, Path}
import org.silkframework.plugins.dataset.rdf.endpoint.JenaModelEndpoint
import org.silkframework.plugins.dataset.rdf.{FileDataset, SparqlSink}
import org.silkframework.rule._
import org.silkframework.rule.execution.ExecuteTransform
import org.silkframework.rule.input.{PathInput, TransformInput}
import org.silkframework.rule.plugins.transformer.combine.ConcatTransformer
import org.silkframework.runtime.activity.Activity
import org.silkframework.runtime.resource.{ClasspathResourceLoader, ReadOnlyResourceManager}

/**
  * Tests hierarchical mappings.
  */
class HierarchicalTransformationTest extends FlatSpec with ShouldMatchers {

  val resources = ReadOnlyResourceManager(ClasspathResourceLoader(getClass.getPackage.getName.replace('.', '/')))

  behavior of "Executor for hierarchical mappings"

  it should "transform flat to nested structures" in {
    execute(
      inputResource = "flatToNestedInput.ttl",
      outputResource = "flatToNestedOutput.ttl",
      transform =
        TransformSpec(
          selection = DatasetSelection("id", uri("Person")),
          rules = Seq(
            TypeMapping(typeUri = uri("Person")),
            DirectMapping(sourcePath = Path(uri("name")), mappingTarget = MappingTarget(uri("name"))),
            HierarchicalMapping(
              relativePath = Path.empty,
              targetProperty = Some(uri("address")),
              childRules = Seq(
                UriMapping(pattern = s"https://silkframework.org/ex/Address_{<${uri("city")}>}_{<${uri("country")}>"),
                DirectMapping(sourcePath = Path(uri("city")), mappingTarget = MappingTarget(uri("city"))),
                DirectMapping(sourcePath = Path(uri("country")), mappingTarget = MappingTarget(uri("country")))
              )
            )
          )
        )
    )
  }

  it should "transform nested to flat structures" in {
    execute(
      inputResource = "nestedToFlatInput.ttl",
      outputResource = "nestedToFlatOutput.ttl",
      transform =
        TransformSpec(
          selection = DatasetSelection("id", uri("Person")),
          rules = Seq(
            TypeMapping(typeUri = uri("Person")),
            DirectMapping(sourcePath = Path(uri("name")), mappingTarget = MappingTarget(uri("name"))),
            HierarchicalMapping(
              relativePath = Path(uri("address")),
              targetProperty = None,
              childRules = Seq(
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
  }


  private def execute(inputResource: String, outputResource: String, transform: TransformSpec): Unit = {
    val source = FileDataset(resources.get(inputResource), "Turtle")

    val targetModel = ModelFactory.createDefaultModel()
    targetModel.setNsPrefix("", "https://silkframework.org/ex/")
    val endpoint = new JenaModelEndpoint(targetModel)
    val dataSink = new SparqlSink(SparqlParams(), endpoint)

    val executor = new ExecuteTransform(source.source, transform, Seq(dataSink))
    Activity(executor).startBlocking()

    val expectedModel = ModelFactory.createDefaultModel()
    RDFDataMgr.read(expectedModel, resources.get(outputResource).load, RDFLanguages.resourceNameToLang(outputResource))

    if(!targetModel.isIsomorphicWith(expectedModel)) {
      val stringWriter = new StringWriter()
      targetModel.write(stringWriter, "TURTLE")
      fail("Generate data is different from expected data. Got:\n" + stringWriter.toString)
    }
  }

  private def uri(name: String) = "https://silkframework.org/ex/" + name

}
