package org.silkframework.plugins.custom.net

import org.silkframework.config.{SilkVocab, CustomTask}
import org.silkframework.entity.{Path, EntitySchema}
import org.silkframework.runtime.plugin.{Param, Plugin}
import org.silkframework.util.Uri

/**
  * Specification of a simple REST task that performs a subset of REST operations and can be used in a
  * workflow.
  */
@Plugin(
  id = "RestOperator",
  label = "REST Operator",
  description = "Executes a REST request based on fixed configuration and/or input parameters and returns the result as entity."
)
case class RestTaskSpec(@Param("The URL to execute this request against.")
                        url: String,
                        @Param("The HTTP method. One of GET, PUT or POST")
                        method: String = "GET",
                        @Param("The accept header String.")
                        accept: String,
                        @Param("Request timeout in ms.")
                        requestTimeout: Int = 10000,
                        @Param("The content-type header String. This can be set in case of PUT or POST. If another " +
                            "content type comes back, the task will fail.")
                        contentType: String) extends CustomTask {
  override def inputSchemata: Seq[EntitySchema] = {
    Seq(EntitySchema(
      typeUri = Uri(SilkVocab.RestTaskData),
      paths = IndexedSeq(
        // The URL that the POST, GET or PUT request should be executed against. This will overwrite the task spec URL if set.
        Path(SilkVocab.RestTaskPropertyURL),
        // The content String that should be send in case of PUT or POST
        Path(SilkVocab.RestTaskPropertyContent)
      )
    ))
  }

  override def outputSchema: Option[EntitySchema] = {
    Some(EntitySchema(
      typeUri = Uri(SilkVocab.RestTaskResult),
      paths = IndexedSeq(
        // The URL the request was executed against.
        Path(SilkVocab.RestTaskPropertyURL),
        // The content of the result.
        Path(SilkVocab.RestTaskPropertyContent),
        // The content type of the result.
        Path(SilkVocab.RestTaskPropertyContentType)
      )
    ))
  }
}
