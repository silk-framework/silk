package org.silkframework.plugins.dataset.xml

import javax.xml.transform.TransformerConfigurationException
import javax.xml.transform.stream.{StreamResult, StreamSource}

import net.sf.saxon.TransformerFactoryImpl
import net.sf.saxon.s9api.SaxonApiException
import org.silkframework.config.Task
import org.silkframework.dataset.DatasetResourceEntityTable
import org.silkframework.entity.EntitySchema
import org.silkframework.execution.ExecutionReport
import org.silkframework.execution.local.{EntityTable, LocalExecution, LocalExecutor}
import org.silkframework.runtime.activity.ActivityContext
import org.silkframework.runtime.resource.InMemoryResourceManager
import org.silkframework.runtime.validation.ValidationException


/**
  * Execute XSLT script on the XML file of the input dataset and returns a [[org.silkframework.runtime.resource.Resource]]
  * which is written to the resource based dataset. The writing to the target dataset happens in the dataset executor.
  */
case class LocalXSLTOperatorExecutor() extends LocalExecutor[XSLTOperator] {
  override def execute(task: Task[XSLTOperator],
                       inputs: Seq[EntityTable],
                       outputSchema: Option[EntitySchema],
                       execution: LocalExecution,
                       context: ActivityContext[ExecutionReport]): Option[EntityTable] = {
    inputs.headOption match {
      case Some(et: DatasetResourceEntityTable) =>
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

        val text = new StreamSource(et.datasetResource.inputStream)
        val inMemoryResource = InMemoryResourceManager().get("tempResource.xml")
        inMemoryResource.write() { os =>
          transformer.transform(text, new StreamResult(os))
        }
        Some(new DatasetResourceEntityTable(inMemoryResource, task))
      case _ =>
        throw new ValidationException("XSLT operator executor did not receive a dataset resource table as input!")
    }
  }
}
