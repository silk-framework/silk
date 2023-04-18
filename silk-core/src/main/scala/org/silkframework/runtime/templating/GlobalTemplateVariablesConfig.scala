package org.silkframework.runtime.templating

import com.typesafe.config.Config
import org.silkframework.config.ConfigValue

import scala.collection.JavaConverters.asScalaSetConverter

/**
  * Global template variables, which are defined in the configuration.
  */
object GlobalTemplateVariablesConfig {

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
          (entry.getKey, TemplateVariable(entry.getKey, entry.getValue.unwrapped().toString, globalScope, isSensitive = false))
        }
      TemplateVariables(map.toMap)
    } else {
      TemplateVariables.empty
    }
  }

  /**
    * Retrieves the configured template engine.
    */
  def templateEngine(): TemplateEngine = {
    engine()
  }

  /** If the templating mechanism is enabled. */
  def isEnabled: Boolean = !engine().isInstanceOf[DisabledTemplateEngine]


  def variables(): TemplateVariables = {
    templateVariables()
  }

}

object GlobalTemplateVariables extends TemplateVariablesReader with Serializable {

  /**
    * The available variable scopes.
    */
  override def scopes: Set[String] = Set(GlobalTemplateVariablesConfig.globalScope)

  /**
    * Retrieves all template variables.
    */
  override def all: TemplateVariables = GlobalTemplateVariablesConfig.variables()

}
