package de.fuberlin.wiwiss.silk.workbench.scripts

import de.fuberlin.wiwiss.silk.plugins.Plugins
import de.fuberlin.wiwiss.silk.plugins.jena.JenaPlugins
import java.util.logging.Logger
import de.fuberlin.wiwiss.silk.workbench.workspace.modules.linking.LinkingTask
import de.fuberlin.wiwiss.silk.util.{DPair, Timer}
import java.io.FileWriter
import de.fuberlin.wiwiss.silk.entity.{Path, Entity}

object DatasetStatistics extends App {
  implicit val log = Logger.getLogger(getClass.getName)
  Plugins.register()
  JenaPlugins.register()

  val datasets = Dataset.fromWorkspace

  val measures = SourceEntities :: SourceProperties :: SourceUsedProperties ::
                 TargetEntities :: TargetProperties :: TargetUsedProperties ::
                 ReferenceLinks :: Nil    

  val table = Table("Dataset Statistics", datasets.map(_.name), measures.map(_.name), collect().transpose)

  val writer = new FileWriter("statistics")
  writer.write(table.toLatex)
  writer.close()

  println("Finished")

  private def collect(): Seq[Seq[Any]] = {
    for(ds <- datasets) yield {
      collect(ds)
    }
  }
  
  private def collect(ds: Dataset) = Timer("Collecting statistics for " + ds.name) {
    //Retrieve frequent paths
    val entityDescs = ds.task.linkSpec.entityDescriptions
    val paths = for((source, desc) <- ds.sources zip entityDescs) yield source.retrievePaths(restriction = desc.restrictions, depth = 1, limit = None).map(_._1).toIndexedSeq
    //Retrieve entities
    val fullEntityDescs = for((desc, p) <- entityDescs zip paths) yield desc.copy(paths = p)
    val entities = for((source, desc) <- ds.sources zip fullEntityDescs) yield source.retrieve(desc).toSeq
    //Apply all measures
    val data = TaskData(ds.task, paths, entities)
    measures.map(_(data))
  }
  
  case class TaskData(task: LinkingTask, paths: DPair[Seq[Path]], entities: DPair[Seq[Entity]])
  
  trait Measure extends (TaskData => Any) {
    def name: String
  }
  
  object SourceEntities extends Measure {
    def name = "Source: Number of entities"
    def apply(data: TaskData) = data.entities.source.size
  }

  object SourceProperties extends Measure {
    def name = "Source: Total number of properties"
    def apply(data: TaskData) = data.paths.source.size
  }

  object SourceUsedProperties extends Measure {
    def name = "Source: Properties per entity"
    def apply(data: TaskData) = {
      val entities = data.entities.source
      val count = entities.map(_.values.count(!_.isEmpty).toDouble).sum / entities.size
      "%.1f".format(count)
    }
  }

  object TargetEntities extends Measure {
    def name = "Target: Number of entities"
    def apply(data: TaskData) = data.entities.target.size
  }

  object TargetProperties extends Measure {
    def name = "Target: Total number of properties"
    def apply(data: TaskData) = data.paths.target.size
  }

  object TargetUsedProperties extends Measure {
    def name = "Target: Properties per entitiy"
    def apply(data: TaskData) = {
      val entities = data.entities.target
      val count = entities.map(_.values.count(!_.isEmpty).toDouble).sum / entities.size
      "%.1f".format(count)
    }
  }

  object ReferenceLinks extends Measure {
    def name = "Reference links"
    def apply(data: TaskData) = data.task.referenceLinks.positive.size + data.task.referenceLinks.negative.size
  }
}