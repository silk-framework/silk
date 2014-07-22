package de.fuberlin.wiwiss.silk.workspace.scripts

import de.fuberlin.wiwiss.silk.plugins.CorePlugins
import de.fuberlin.wiwiss.silk.plugins.jena.JenaPlugins
import java.util.logging.Logger
import de.fuberlin.wiwiss.silk.workspace.modules.linking.LinkingTask
import de.fuberlin.wiwiss.silk.util.{Table, DPair, Timer}
import java.io.FileWriter
import de.fuberlin.wiwiss.silk.entity.{Path, Entity}

/**
 * Collects various statistics about the projects in the workspace.
 */
object DatasetStatistics extends App {
  implicit val log = Logger.getLogger(getClass.getName)
  CorePlugins.register()
  JenaPlugins.register()

  val datasets = Data.fromWorkspace

  val measures = SourceEntities :: TargetEntities :: SourceProperties :: TargetProperties :: SourceCoverage :: TargetCoverage :: PosReferenceLinks :: NegReferenceLinks :: Nil

  val table = Table("Dataset Statistics", measures.map(_.name), datasets.map(_.name), datasets.map(collect))

  val writer = new FileWriter("statistics")
  writer.write(table.toLatex)
  writer.close()

  println("Finished")
  
  private def collect(ds: Data) = Timer("Collecting statistics for " + ds.name) {
    //Retrieve frequent paths
    val entityDescs = ds.task.linkSpec.entityDescriptions
    val paths = for((source, desc) <- ds.sources zip entityDescs) yield source.source.retrievePaths(restriction = desc.restrictions, depth = 1, limit = None).map(_._1).toIndexedSeq
    //Retrieve entities
    val fullEntityDescs = for((desc, p) <- entityDescs zip paths) yield desc.copy(paths = p)
    val entities = for((source, desc) <- ds.sources zip fullEntityDescs) yield source.source.retrieve(desc).toSeq
    //Apply all measures
    val data = TaskData(ds.task, paths, entities)
    measures.map(_(data))
  }

  /**
   * Holds the base information about a task.
   */
  case class TaskData(task: LinkingTask, paths: DPair[Seq[Path]], entities: DPair[Seq[Entity]])

  /**
   * A measure.
   */
  trait Measure extends (TaskData => Any) {
    def name: String
  }

  /**
   * The number of entities in both datasets.
   */
  object SourceEntities extends Measure {
    def name = "Source entities"
    def apply(data: TaskData) = data.entities.source.size
  }
  
  /**
   * The number of entities in both datasets.
   */
  object TargetEntities extends Measure {
    def name = "Target entities"
    def apply(data: TaskData) = data.entities.target.size
  }

  /**
   * The number of properties in total in the source data set.
   */
  object SourceProperties extends Measure {
    def name = "Source Properties"
    def apply(data: TaskData) = {
      val sourcePaths = data.paths.source.map(serialize)
      sourcePaths.distinct.size
    }
    /** Serializes the operators of a path without the variable. */
    private def serialize(p: Path) = p.operators.map(_.serialize).mkString
  }
  /**
   * The number of properties in total in the target data set.
   */
  object TargetProperties extends Measure {
    def name = "Target Properties"
    def apply(data: TaskData) = {
      val targetPaths = data.paths.target.map(serialize)
      targetPaths.distinct.size
    }
    /** Serializes the operators of a path without the variable. */
    private def serialize(p: Path) = p.operators.map(_.serialize).mkString
  }

  /**
   * The average number of properties per entity.
   */
  object SourceCoverage extends Measure {
    def name = "Source Coverage"
    def apply(data: TaskData) = {
      val entities = data.entities.source
      val propertyCount = SourceProperties(data)
      val propertyOccurrences = entities.map(_.values.count(!_.isEmpty)).sum
      val coverage = propertyOccurrences.toDouble / (propertyCount * entities.size)

      "%.1f".format(coverage)
    }
  }

  /**
   * The average number of properties per entity.
   */
  object TargetCoverage extends Measure {
    def name = "Target Coverage"
    def apply(data: TaskData) = {
      val entities = data.entities.target
      val propertyCount = TargetProperties(data)
      val propertyOccurrences = entities.map(_.values.count(!_.isEmpty)).sum
      val coverage = propertyOccurrences.toDouble / (propertyCount * entities.size)

      "%.1f".format(coverage)
    }
  }

  /**
   * The number of positive reference links.
   */
  object PosReferenceLinks extends Measure {
    def name = "Pos. Reference links"
    def apply(data: TaskData) = data.task.referenceLinks.positive.size
  }

  /**
   * The number of negative reference links.
   */
  object NegReferenceLinks extends Measure {
    def name = "Neg. Reference links"
    def apply(data: TaskData) = data.task.referenceLinks.negative.size
  }
}