package de.fuberlin.wiwiss.silk.workspace.scripts

import de.fuberlin.wiwiss.silk.config.LinkSpecification
import de.fuberlin.wiwiss.silk.util.DPair
import de.fuberlin.wiwiss.silk.dataset.{Dataset, DataSource}
import de.fuberlin.wiwiss.silk.workspace.{Task, User}

case class Data(name: String,
                task: Task[LinkSpecification],
                sources: DPair[DataSource])

object Data {

  def fromWorkspace: Seq[Data] = {
    for(project <- User().workspace.projects.toSeq;
        task <- project.tasks[LinkSpecification]) yield {
      Data(
        name = project.name,
        task = task,
        sources = task.data.dataSelections.map(ds => project.task[Dataset](ds.datasetId).data.source)
      )
    }
  }
}