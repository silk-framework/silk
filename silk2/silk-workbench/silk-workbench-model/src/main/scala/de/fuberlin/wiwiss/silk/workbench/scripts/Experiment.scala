package de.fuberlin.wiwiss.silk.workbench.scripts

import de.fuberlin.wiwiss.silk.workbench.workspace.modules.linking.LinkingTask
import de.fuberlin.wiwiss.silk.util.DPair
import de.fuberlin.wiwiss.silk.datasource.Source
import de.fuberlin.wiwiss.silk.workbench.workspace.User

case class Experiment(name: String,
                      task: LinkingTask,
                      sources: DPair[Source])

object Experiment {

  def experiments: Seq[Experiment] = {
    for(project <- User().workspace.projects.toSeq;
        task <- project.linkingModule.tasks) yield {
      Experiment(
        name = project.name + " - " + task.name,
        task = task,
        sources = task.linkSpec.datasets.map(ds => project.sourceModule.task(ds.sourceId).source)
      )
    }
  }
}