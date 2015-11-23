package org.silkframework.workspace.scripts

import org.silkframework.config.LinkSpecification
import org.silkframework.util.DPair
import org.silkframework.dataset.{Dataset, DataSource}
import org.silkframework.workspace.User
import org.silkframework.workspace.Task

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