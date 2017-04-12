package org.silkframework.execution.local

import java.io.File

import org.silkframework.entity.Path
import org.silkframework.plugins.dataset.rdf.FileDataset
import org.silkframework.rule._
import org.silkframework.rule.execution.ExecuteTransform
import org.silkframework.runtime.activity.Activity
import org.silkframework.runtime.resource.{ClasspathResourceLoader, FileResourceManager, ReadOnlyResourceManager}

/**
  * Created by risele on 4/11/2017.
  */
object TransformationTest extends App {

  val resources = FileResourceManager(new File(getClass.getClassLoader.getResource("org/silkframework/plugins/dataset/rdf/test.nt").getFile).getParentFile)
  resources.get("transformOutput2.nt").write("")

  val source = FileDataset(resources.get("transformInput.ttl"), "Turtle")

  val target = FileDataset(resources.get("transformOutput2.nt"), "N-Triples")

  val transform =
    TransformSpec(
      selection = DatasetSelection("id", uri("Person")),
      rules = Seq(
        DirectMapping(sourcePath = Path(uri("name")), mappingTarget = MappingTarget(uri("name"))),
        HierarchicalMapping(
          relativePath = Path.empty,
          targetProperty = uri("address"),
          childRules = Seq(
            UriMapping(pattern = s"https://silkframework.org/ex/Address_{<${uri("city")}>}_{<${uri("country")}>"),
            DirectMapping(sourcePath = Path(uri("city")), mappingTarget = MappingTarget(uri("city"))),
            DirectMapping(sourcePath = Path(uri("country")), mappingTarget = MappingTarget(uri("country")))
          )
        )
      )
    )

  val executor = new ExecuteTransform(source.source, transform, Seq(target.entitySink))

  Activity(executor).startBlocking()

  println("Output:\n" + resources.get("transformOutput2.nt").loadAsString)

  private def uri(name: String) = "https://silkframework.org/ex/" + name

}
