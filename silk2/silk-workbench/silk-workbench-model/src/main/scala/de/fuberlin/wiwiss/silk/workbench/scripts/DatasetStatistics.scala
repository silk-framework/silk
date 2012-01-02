package de.fuberlin.wiwiss.silk.workbench.scripts

import de.fuberlin.wiwiss.silk.plugins.Plugins
import de.fuberlin.wiwiss.silk.workbench.workspace.User
import de.fuberlin.wiwiss.silk.plugins.jena.JenaPlugins
import de.fuberlin.wiwiss.silk.datasource.{Source, DataSource}
import de.fuberlin.wiwiss.silk.entity.{Entity, EntityDescription, SparqlRestriction}
import java.util.logging.Logger
import de.fuberlin.wiwiss.silk.workbench.workspace.modules.linking.LinkingTask
import de.fuberlin.wiwiss.silk.util.{DPair, Timer}
import java.io.FileWriter

object DatasetStatistics extends App {
  Plugins.register()
  JenaPlugins.register()

  implicit val log = Logger.getLogger(getClass.getName)

  val writer = new FileWriter("statistics.csv")

  for(line <- collect().transpose) {
    writer.write(line.head._1 + "," + line.map(_._2).mkString(",") + "\n")
  }
  
  writer.close()
  
  private def collect(): Traversable[Seq[(String, String)]] = {
    for(project <- User().workspace.projects;
        linkingTask <- project.linkingModule.tasks) yield {
      val sources = linkingTask.linkSpec.datasets.map(ds => project.sourceModule.task(ds.sourceId).source)
      collect(linkingTask, sources)
    }
  }
  
  private def collect(linkingTask: LinkingTask, sources: DPair[Source]) = Timer("Collecting statistics for " + linkingTask.name) {
    val entityDescs = linkingTask.linkSpec.entityDescriptions
    var results = Seq[(String, String)]()

    results :+= ("Name", linkingTask.name.toString)
    results ++= collectStatistics(sources.source, entityDescs.source).map(r => r.copy(_1 = "Source: " + r._1))
    results ++= collectStatistics(sources.target, entityDescs.target).map(r => r.copy(_1 = "Target: " + r._1))
    results :+= ("Reference links (Pos/Neg)", linkingTask.referenceLinks.positive.size + "/" + linkingTask.referenceLinks.negative.size)

    results
  }

  private def collectStatistics(source: Source, entityDesc: EntityDescription) = {
    val paths = source.retrievePaths(restriction = entityDesc.restrictions, depth = 1, limit = None).map(_._1).toIndexedSeq
    val entities = source.retrieve(entityDesc.copy(paths = paths)).toStream
    var results = List[(String, String)]()
    
    results ::= ("Number of entities", entities.size.toString)
    results ::= ("Total number of properties", paths.size.toString)
    
    val averageProperties = entities.map(_.values.count(!_.isEmpty).toDouble).sum / entities.size
    results ::= ("Average number of defined properties", averageProperties.toString)
    
    results.reverse
  }
}