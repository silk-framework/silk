package de.fuberlin.wiwiss.silk.workspace.scripts

import de.fuberlin.wiwiss.silk.workspace.modules.linking.LinkingTask
import de.fuberlin.wiwiss.silk.util.DPair
import de.fuberlin.wiwiss.silk.datasource.Source
import de.fuberlin.wiwiss.silk.workspace.User

case class Dataset(name: String,
                   task: LinkingTask,
                   sources: DPair[Source])

object Dataset {

  def fromWorkspace: Seq[Dataset] = {
    for(project <- User().workspace.projects.toSeq;
        task <- project.linkingModule.tasks) yield {
      Dataset(
        name = project.name,
        task = task,
        sources = task.linkSpec.datasets.map(ds => project.sourceModule.task(ds.sourceId).source)
      )
    }
  }
}