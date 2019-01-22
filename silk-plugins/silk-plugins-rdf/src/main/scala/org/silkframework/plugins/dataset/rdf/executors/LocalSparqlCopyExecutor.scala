package org.silkframework.plugins.dataset.rdf.executors

import java.io.File

import org.apache.commons.io.FileUtils
import org.silkframework.config.Task
import org.silkframework.dataset.rdf.{QuadIterator, SparqlEndpointEntityTable}
import org.silkframework.entity.EntitySchema
import org.silkframework.execution.local._
import org.silkframework.execution.{ExecutionReport, TaskException}
import org.silkframework.plugins.dataset.rdf.QuadIteratorImpl
import org.silkframework.plugins.dataset.rdf.formatters.NTriplesQuadFormatter
import org.silkframework.plugins.dataset.rdf.tasks.SparqlCopyCustomTask
import org.silkframework.runtime.activity.{ActivityContext, UserContext}

/**
  * Local executor for [[SparqlCopyCustomTask]].
  */
class LocalSparqlCopyExecutor() extends LocalExecutor[SparqlCopyCustomTask] {
    override def execute(task: Task[SparqlCopyCustomTask],
                         inputs: Seq[LocalEntities],
                         outputSchema: Option[EntitySchema],
                         execution: LocalExecution,
                         context: ActivityContext[ExecutionReport])
                        (implicit userContext: UserContext): Option[LocalEntities] = {
        inputs match {
            case Seq(sparql: SparqlEndpointEntityTable) =>
                val internalTaskId = "counstruct_copy_tmp"
                val results: QuadIterator = sparql.construct(task.selectQuery.str)
                // if we have to safe construct graph as temp file before propagation
                val temporaryEntities = if(task.tempFile){
                    val tempFile = File.createTempFile(internalTaskId, "nt")
                    tempFile.delete
                    tempFile.deleteOnExit()
                    // adding file deletion shutdown hook
                    execution.addShutdownHook(() => FileUtils.forceDelete(tempFile))
                    // save to temp file
                    results.saveToFile(tempFile)

                    val qi = QuadIteratorImpl.apply(tempFile, new NTriplesQuadFormatter)
                    Some(QuadEntityTable(qi, task))
                }
                // else we just stream it to the output
                else{
                    Some(QuadEntityTable(results, task))
                }
                temporaryEntities
            case _ =>
                throw TaskException("SPARQL select executor did not receive a SPARQL endpoint as requested!")
        }
    }
}
