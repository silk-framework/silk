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

  /**
    * Global template variables are nested under this name.
    */
  final val globalScope = "global"

  /**
    * The configured template engine.
    */
  private val engine: ConfigValue[TemplateEngine] = (config: Config) => {
    val engineConfigVar = configNamespace + ".engine"
    if(config.hasPath(engineConfigVar)) {
      TemplateEngines.create(config.getString(engineConfigVar))
    } else {
      DisabledTemplateEngine()
    }
  }

  /**
    * The configured global template variables.
    */
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
    engine().compile(value).evaluate(variableMap, writer)
    writer.toString
  }

  /**
    * Lists all available variable names.
    */
  def variableNames: Seq[String] = {
    for(key <- templateVariables().map.keys.toSeq.sorted) yield {
      globalScope + "." + key
    }
  }

  /**
    * Returns variables as a map to be used in template evaluation.
    */
  def variableMap: Map[String, util.Map[String, String]] = Map(globalScope -> templateVariables().map.asJava)

  /** If the templating mechanism is enabled. */
  def isEnabled: Boolean = !engine().isInstanceOf[DisabledTemplateEngine]
}
