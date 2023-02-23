package org.silkframework.runtime.templating

import com.typesafe.config.Config
import org.silkframework.config.ConfigValue
import scala.collection.JavaConverters.asScalaSetConverter
import java.io.StringWriter

/**
  * Global template variables, which are defined in the configuration.
  */
object GlobalTemplateVariables {

  private final val configNamespace: String = "config.variables"

  private val engine: ConfigValue[TemplateEngine] = (config: Config) => {
    val engineConfigVar = configNamespace + ".engine"
    if(config.hasPath(engineConfigVar)) {
      TemplateEngines.create(config.getString(engineConfigVar))
    } else {
      DisabledTemplateEngine()
    }
  }

  private val variables: ConfigValue[TemplateVariables] = (config: Config) => {
    val variablesConfigVar = configNamespace + ".global"
    if(config.hasPath(variablesConfigVar)) {
      val map =
        for (entry <- config.getConfig(variablesConfigVar).entrySet().asScala) yield {
          (entry.getKey, entry.getValue.unwrapped().toString)
        }
      TemplateVariables(map.toMap)
    } else {
      TemplateVariables.empty
    }
  }

  def resolveParameter(template: String): String = {
    val writer = new StringWriter()
    engine().compile(template).evaluate(variables().map, writer)
    writer.toString
  }

  def resolveParameters(parameterTemplates: Map[String, String]): Map[String, String] = {
    parameterTemplates.mapValues(resolveParameter)
  }

}
