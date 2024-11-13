package org.silkframework.runtime.templating

import com.typesafe.config.Config
import org.silkframework.config.ConfigValue

import scala.jdk.CollectionConverters.SetHasAsScala

/**
  * Global template variables, which are defined in the configuration.
  */
object GlobalTemplateVariablesConfig {

  private final val configNamespace: String = "config.variables"

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
      val variables =
        for (entry <- config.getConfig(variablesConfigVar).entrySet().asScala.toSeq) yield {
          TemplateVariable(entry.getKey, entry.getValue.unwrapped().toString, None, None, isSensitive = false, TemplateVariableScopes.global)
        }
      TemplateVariables(variables)
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
  override def scopes: Set[String] = Set(TemplateVariableScopes.global)

  /**
    * Retrieves all template variables.
    */
  override def all: TemplateVariables = GlobalTemplateVariablesConfig.variables()

}
