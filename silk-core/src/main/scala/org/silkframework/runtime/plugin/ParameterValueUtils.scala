package org.silkframework.runtime.plugin

/**
 * Extension methods on parameter values to access parameters by path.
 */
object ParameterValueUtils {

  implicit class ExtendedParameterValues(params: ParameterValues) {

    /**
     * Retrieves a (nested) parameter value.
     *
     * @param path A path of the form a/b/value
     * @return The value if the path refers to a simple value. None, otherwise.
     */
    def valueAtPath(path: String)(implicit pluginContext: PluginContext): Option[String] = {
      val parts = path.split('/')
      if(parts.isEmpty) {
        None
      } else if(parts.length == 1) {
        params.values.get(parts.head) match {
          case Some(ParameterStringValue(value)) =>
            Some(value)
          case Some(template: ParameterTemplateValue) =>
            Some(template.evaluate(pluginContext.templateVariables.all))
          case _ =>
            None
        }
      } else {
        params.values.get(parts.head) match {
          case Some(subValue: ParameterValues) =>
            subValue.valueAtPath(parts.tail.mkString("/"))
          case _ =>
            None
        }
      }
    }

    /**
     * Enumerates all available paths.
     */
    def availablePaths: Iterable[String] = {
      for((key, value) <- params.values) yield {
        value match {
          case subParameters: ParameterValues =>
            subParameters.availablePaths.map(key + "/" + _)
          case _ =>
            Iterable(key)
        }
      }
    }.flatten

  }

}
