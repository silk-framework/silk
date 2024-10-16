package org.silkframework.plugins.dataset.xml

import net.sf.saxon.TransformerFactoryImpl
import net.sf.saxon.s9api.SaxonApiException
import org.silkframework.config.Task
import org.silkframework.execution.typed.{FileEntity, FileEntitySchema}
import org.silkframework.execution.local.{LocalEntities, LocalExecution, LocalExecutor}
import org.silkframework.execution.{ExecutionReport, ExecutorOutput}
import org.silkframework.runtime.activity.ActivityContext
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.validation.ValidationException

import javax.xml.transform.TransformerConfigurationException
import javax.xml.transform.stream.{StreamResult, StreamSource}


/**
  * Execute XSLT script on the XML file of the input dataset and returns a [[org.silkframework.runtime.resource.Resource]]
  * which is written to the resource based dataset. The writing to the target dataset happens in the dataset executor.
  */
case class LocalXSLTOperatorExecutor() extends LocalExecutor[XSLTOperator] {
  override def execute(task: Task[XSLTOperator],
                       inputs: Seq[LocalEntities],
                       output: ExecutorOutput,
                       execution: LocalExecution,
                       context: ActivityContext[ExecutionReport])
                      (implicit pluginContext: PluginContext): Option[LocalEntities] = {
    inputs.headOption match {
      case Some(FileEntitySchema(fileEntities)) =>
        val xSLTOperator = task.data
        val factory = new TransformerFactoryImpl()
        val xslt = new StreamSource(xSLTOperator.file.inputStream)
        val transformer = try {
          factory.newTransformer(xslt)
        } catch {
          case ex: TransformerConfigurationException =>
            ex.getCause match {
              case apiException: SaxonApiException =>
                val lineNr = if(apiException.getLineNumber > -1) s" in line ${apiException.getLineNumber}" else ""
                throw new ValidationException(s"There has been an error executing the XSLT stylesheet$lineNr: "
                    + apiException.getMessage)
              case _ =>
                throw new ValidationException(s"There has been an error execution the XSLT stylesheet: " + ex.getMessage)
            }
        }

        val outputFiles =
          for(inputEntity <- fileEntities.typedEntities) yield {
            val text = new StreamSource(inputEntity.file.inputStream)
            val tempFile = FileEntity.createTemp("xsltResult", ".xml")
            tempFile.file.write() { os =>
              transformer.transform(text, new StreamResult(os))
            }
            tempFile
          }
        Some(FileEntitySchema.create(outputFiles, task))
      case _ =>
        throw new ValidationException("XSLT operator executor did not receive a dataset resource table as input!")
    }
  }
}
