package org.silkframework.runtime.templating

import com.typesafe.config.Config
import org.silkframework.config.ConfigValue

import java.io.StringWriter
import java.util
import scala.collection.JavaConverters.{asScalaSetConverter, mapAsJavaMapConverter}

/**
  * Global template variables, which are defined in the configuration.
  */
object GlobalTemplateVariables {

  private final val configNamespace: String = "config.variables"

  final val globalScope = "global"

  private val engine: ConfigValue[TemplateEngine] = (config: Config) => {
    val engineConfigVar = configNamespace + ".engine"
    if(config.hasPath(engineConfigVar)) {
      TemplateEngines.create(config.getString(engineConfigVar))
    } else {
      DisabledTemplateEngine()
    }
  }

  private val templateVariables: ConfigValue[TemplateVariables] = (config: Config) => {
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

  /** Resolves a variable template string.
    * @throws TemplateEvaluationException If the template evaluation failed.
    **/
  def resolveTemplateValue(value: String): String = {
    val writer = new StringWriter()
    engine().compile(value).evaluate(variableMapNested, writer)
    writer.toString
  }

  def variableMapNested: Map[String, util.Map[String, String]] = Map(globalScope -> templateVariables().map.asJava)

  def variableNames: Seq[String] = variableMap.keys.toSeq.sorted

  def variableMap: Map[String, String] = {
    for((key, value) <- templateVariables().map) yield {
      (globalScope + "." + key, value)
    }
  }
}