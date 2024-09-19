package org.silkframework.plugins.dataset.rdf.executors

import org.apache.commons.io.FileUtils
import org.apache.jena.riot.Lang
import org.silkframework.config.Task
import org.silkframework.dataset.rdf.{IteratorFormatter, QuadIterator}
import org.silkframework.entity.rdf.SparqlEntitySchema
import org.silkframework.execution.local._
import org.silkframework.execution.typed.SparqlEndpointEntitySchema
import org.silkframework.execution.{ExecutionReport, ExecutorOutput, TaskException}
import org.silkframework.plugins.dataset.rdf.datasets.FileBasedQuadEntityTable
import org.silkframework.plugins.dataset.rdf.formatters.NTriplesQuadFormatter
import org.silkframework.plugins.dataset.rdf.tasks.SparqlCopyCustomTask
import org.silkframework.runtime.activity.{ActivityContext, UserContext}
import org.silkframework.runtime.plugin.PluginContext

import java.io.File

/**
  * Local executor for [[SparqlCopyCustomTask]].
  */
class LocalSparqlCopyExecutor() extends LocalExecutor[SparqlCopyCustomTask] {
    override def execute(task: Task[SparqlCopyCustomTask],
                         inputs: Seq[LocalEntities],
                         output: ExecutorOutput,
                         execution: LocalExecution,
                         context: ActivityContext[ExecutionReport])
                        (implicit pluginContext: PluginContext): Option[LocalEntities] = {
        implicit val user: UserContext = pluginContext.user
        inputs match {
            case Seq(SparqlEndpointEntitySchema(entities)) =>
                val internalTaskId = "counstruct_copy_tmp"
                val rdfDataset = entities.task.data.plugin
                val results: QuadIterator = rdfDataset.sparqlEndpoint.construct(task.query.str)
                // if we have to safe construct graph as temp file before propagation
                val temporaryEntities = if(task.tempFile){
                    val tempFile = File.createTempFile(internalTaskId, "nt")
                    tempFile.deleteOnExit()
                    // adding file deletion shutdown hook
                    execution.addShutdownHook(() => FileUtils.forceDelete(tempFile))
                    // save to temp file
                    IteratorFormatter.saveToFile(tempFile, results, new NTriplesQuadFormatter())

                    Some(new FileBasedQuadEntityTable(tempFile, Lang.NQUADS, task))
                }
                // else we just stream it to the output
                else{
                    Some(QuadEntityTable(() => results.asQuadEntities, task))
                }
                temporaryEntities
            case _ =>
                throw TaskException("SPARQL select executor did not receive a SPARQL endpoint as requested!")
        }
    }
}
