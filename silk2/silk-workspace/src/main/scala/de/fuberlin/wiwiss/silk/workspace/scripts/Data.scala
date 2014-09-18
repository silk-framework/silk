package de.fuberlin.wiwiss.silk.workspace.scripts

import de.fuberlin.wiwiss.silk.workspace.modules.linking.LinkingTask
import de.fuberlin.wiwiss.silk.util.DPair
import de.fuberlin.wiwiss.silk.dataset.DataSource
import de.fuberlin.wiwiss.silk.workspace.User
import de.fuberlin.wiwiss.silk.workspace.modules.dataset.DatasetTask

case class Data(name: String,
                task: LinkingTask,
                sources: DPair[DataSource])

object Data {

  def fromWorkspace: Seq[Data] = {
    for(project <- User().workspace.projects.toSeq;
        task <- project.tasks[LinkingTask]) yield {
      Data(
        name = project.name,
        task = task,
        sources = task.linkSpec.datasets.map(ds => project.task[DatasetTask](ds.datasetId).dataset.source)
      )
    }
  }
}