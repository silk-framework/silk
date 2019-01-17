package org.silkframework.plugins.dataset.rdf.executors

import java.io.File

import org.silkframework.config.{PlainTask, Task}
import org.silkframework.dataset.DatasetSpec
import org.silkframework.dataset.rdf.{QuadIterator, SparqlEndpointEntityTable}
import org.silkframework.entity.EntitySchema
import org.silkframework.execution.{ExecutionReport, TaskException}
import org.silkframework.execution.local._
import org.silkframework.plugins.dataset.rdf.datasets.RdfFileDataset
import org.silkframework.plugins.dataset.rdf.tasks.SparqlCopyCustomTask
import org.silkframework.runtime.activity.{ActivityContext, UserContext}
import org.silkframework.runtime.resource.FileResource
import org.silkframework.util.Identifier

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
                // we should safe construct as temp file before propagation
                val temporaryEntities = if(task.tempFile){
                    val tempFile = File.createTempFile(internalTaskId, "nt")
                    tempFile.delete
                    tempFile.deleteOnExit()
                    // adding file deletion shutdown hook
                    execution.addShutdownHook(Identifier.fromAllowed("delete_" + tempFile.getName), () => tempFile.delete)

                    val resource = FileResource(tempFile)
                    val nTripleFile = RdfFileDataset(resource, "N-Triples")
                    new LocalDatasetExecutor().execute(
                        PlainTask(internalTaskId, DatasetSpec(nTripleFile)),
                        Seq(QuadEntityTable(results.getQuadEntities, task)),
                        Some(QuadEntityTable.schema),
                        execution,
                        context
                    )
                }
                // else we just stream it to the output
                else{
                    Some(QuadEntityTable(results.getQuadEntities, task))
                }
                temporaryEntities
            case _ =>
                throw TaskException("SPARQL select executor did not receive a SPARQL endpoint as requested!")
        }
    }
}
