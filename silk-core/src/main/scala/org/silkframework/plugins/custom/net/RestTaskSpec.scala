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
                        url: String = "",
                        @Param("The HTTP method. One of GET, PUT or POST")
                        method: String = "GET",
                        @Param("The accept header String.")
                        accept: String = "",
                        @Param("Request timeout in ms.")
                        requestTimeout: Int = 10000,
                        @Param("The content-type header String. This can be set in case of PUT or POST. If another " +
                            "content type comes back, the task will fail.")
                        contentType: String = "",
                        @Param("The content that is send with a POST or PUT request. For handling this payload dynamically " +
                            "this parameter must be overwritten via the task input.")
                        content: String = ""
                       ) extends CustomTask {
  override def inputSchemataOpt: Option[Seq[EntitySchema]] = {
    Some(Seq(inputSchema))
  }

  final private val inputPaths = IndexedSeq[(String, (RestTaskSpec, String) => RestTaskSpec)](
    // The URL that the POST, GET or PUT request should be executed against. This will overwrite the task spec URL if set.
    SilkVocab.RestTaskPropertyURL -> ((task: RestTaskSpec, newVal: String) => task.copy(url = newVal)),
    // The content String that should be send in case of PUT or POST
    SilkVocab.RestTaskPropertyContent -> ((task: RestTaskSpec, newVal: String) => task.copy(content = newVal))
  )

  val inputSchema = EntitySchema(
    typeUri = Uri(SilkVocab.RestTaskData),
    paths = inputPaths.map(p => Path(p._1))
  )

  /**
    * Returns a copy of this [[RestTaskSpec]] with values from the provided config overwriting the config values.
    * @param config
    */
  def customize(config: Map[String, String]): RestTaskSpec = {
    var copy = this.copy()
    for((url, updateFn) <- inputPaths) {
      config.get(url) foreach { value =>
        copy = updateFn(copy, value)
      }
    }
    copy
  }

  val outputSchema = EntitySchema(
    typeUri = Uri(SilkVocab.RestTaskResult),
    paths = IndexedSeq(
      // The URL the request was executed against.
      Path(SilkVocab.RestTaskPropertyURL),
      // The content of the result.
      Path(SilkVocab.RestTaskPropertyContent),
      // The content type of the result.
      Path(SilkVocab.RestTaskPropertyContentType)
    )
  )

  override def outputSchemaOpt: Option[EntitySchema] = {
    Some(outputSchema)
  }
}
