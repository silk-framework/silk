package org.silkframework.plugins.dataset.rdf

import org.silkframework.config.Task
import org.silkframework.entity.EntitySchema
import org.silkframework.execution.ExecutionReport
import org.silkframework.execution.local.{LocalEntities, LocalExecution, LocalExecutor}
import org.silkframework.runtime.activity.{ActivityContext, UserContext}

/**
  * Local executor for [[SparqlUpdateCustomTask]].
  */
case class LocalSparqlUpdateExecutor() extends LocalExecutor[SparqlSelectCustomTask] {
  override def execute(task: Task[SparqlSelectCustomTask],
                       inputs: Seq[LocalEntities],
                       outputSchema: Option[EntitySchema],
                       execution: LocalExecution,
                       context: ActivityContext[ExecutionReport])
                      (implicit userContext: UserContext): Option[LocalEntities] = {
    
    None // TODO
  }
}
