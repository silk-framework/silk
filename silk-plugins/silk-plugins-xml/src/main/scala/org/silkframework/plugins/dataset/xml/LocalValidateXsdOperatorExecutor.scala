package org.silkframework.plugins.dataset.xml

import org.silkframework.config.Task
import org.silkframework.dataset.DatasetResourceEntityTable
import org.silkframework.entity.Entity
import org.silkframework.execution.local.{GenericEntityTable, LocalEntities, LocalExecution, LocalExecutor}
import org.silkframework.execution.{ExecutionReport, ExecutorOutput}
import org.silkframework.runtime.activity.ActivityContext
import org.silkframework.runtime.iterator.CloseableIterator
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.validation.ValidationException
import org.xml.sax.{ErrorHandler, SAXParseException}

import javax.xml.XMLConstants
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory
import scala.collection.mutable


case class LocalValidateXsdOperatorExecutor() extends LocalExecutor[ValidateXsdOperator] {

  override def execute(task: Task[ValidateXsdOperator],
                       inputs: Seq[LocalEntities],
                       output: ExecutorOutput,
                       execution: LocalExecution,
                       context: ActivityContext[ExecutionReport])
                      (implicit pluginContext: PluginContext): Option[LocalEntities] = {
    inputs.headOption match {
      case Some(et: DatasetResourceEntityTable) =>
        // Create XSD validator
        val operator = task.data
        val factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
        val schema = factory.newSchema(new StreamSource(operator.file.inputStream))
        val validator = schema.newValidator

        // Execute validator
        val errorHandler = new XsdErrorHandler()
        validator.setErrorHandler(errorHandler)
        validator.validate(new StreamSource(et.datasetResource.inputStream))

        // Return errors
        Some(GenericEntityTable(errorHandler.entities, ValidateXsdOperator.outputSchema, task))
      case _ =>
        throw new ValidationException("XSD operator executor did not receive a dataset resource table as input!")
    }
  }

  /**
   * Collects validation errors and generates entities for them.
   */
  private class XsdErrorHandler extends ErrorHandler {

    private val errors = mutable.Buffer[Entity]()

    private var counter = 0

    def entities: CloseableIterator[Entity] = CloseableIterator(errors)

    override def warning(exception: SAXParseException): Unit = {
      addError("warning", exception)
    }

    override def error(exception: SAXParseException): Unit = {
      addError("error", exception)
    }

    override def fatalError(exception: SAXParseException): Unit = {
      addError("fatalError", exception)
    }

    private def addError(severity: String, ex: SAXParseException): Unit = {
      errors.append(
        new Entity(
          uri = "urn:instance:Error" + counter,
          values = IndexedSeq(Seq(severity), Seq(ex.getMessage), Seq(ex.getLineNumber.toString), Seq(ex.getColumnNumber.toString)),
          schema = ValidateXsdOperator.outputSchema
        )
      )
      counter += 1
    }
  }
}
