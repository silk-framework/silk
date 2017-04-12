package org.silkframework.execution.local

import java.io.File

import org.silkframework.entity.{BackwardOperator, Path}
import org.silkframework.plugins.dataset.rdf.FileDataset
import org.silkframework.rule._
import org.silkframework.rule.execution.ExecuteTransform
import org.silkframework.rule.input.{PathInput, TransformInput}
import org.silkframework.rule.plugins.transformer.combine.ConcatTransformer
import org.silkframework.runtime.activity.Activity
import org.silkframework.runtime.resource.{ClasspathResourceLoader, FileResourceManager, ReadOnlyResourceManager}

/**
  * Created by risele on 4/11/2017.
  */
object TransformationTest extends App {

  val resources = FileResourceManager(new File(getClass.getClassLoader.getResource("org/silkframework/plugins/dataset/rdf/test.nt").getFile).getParentFile)

  execute(
    resourceName = "transformInputFlat.ttl",
    transform =
      TransformSpec(
        selection = DatasetSelection("id", uri("Person")),
        rules = Seq(
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

  execute(
    resourceName = "transformInputNested.ttl",
    transform =
      TransformSpec(
        selection = DatasetSelection("id", uri("Person")),
        rules = Seq(
          DirectMapping(sourcePath = Path(uri("name")), mappingTarget = MappingTarget(uri("name"))),
          HierarchicalMapping(
            relativePath = Path(uri("address")),
            targetProperty =  None,
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


  private def execute(resourceName: String, transform: TransformSpec): Unit = {
    resources.get("transformOutput.nt").write("")

    val source = FileDataset(resources.get(resourceName), "Turtle")

    val target = FileDataset(resources.get("transformOutput.nt"), "N-Triples")

    val executor = new ExecuteTransform(source.source, transform, Seq(target.entitySink))
    Activity(executor).startBlocking()

    println("Output:\n" + resources.get("transformOutput.nt").loadAsString)
  }

  private def uri(name: String) = "https://silkframework.org/ex/" + name

}
