package org.silkframework.runtime.templating

import com.typesafe.config.{Config, ConfigObject, ConfigValueType, ConfigValue => TypesafeConfigValue}
import org.silkframework.config.{ConfigValue, InvalidConfigException}

import scala.jdk.CollectionConverters.SetHasAsScala

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
      val variables =
        for (entry <- config.getConfig(variablesConfigVar).root().entrySet().asScala.toSeq) yield {
          readVariable(entry.getKey, entry.getValue)
        }
      TemplateVariables(variables)
    } else {
      TemplateVariables.empty
    }
  }

  private def readVariable(key: String, value: TypesafeConfigValue): TemplateVariable = {
    value match {
      case objValue: ConfigObject =>
        val value = Option(objValue.get("value"))
          .getOrElse(throw new InvalidConfigException(configNamespace + ".global." + key, objValue.origin(), "Parameter 'value' is missing."))
        val description = if(objValue.containsKey("description")) Some(objValue.toConfig.getString("description")) else None
        val isSensitive = if(objValue.containsKey("isSensitive")) objValue.toConfig.getBoolean("isSensitive") else false
        TemplateVariable(key, value.unwrapped().toString, None, description, isSensitive = isSensitive, globalScope)
      case _ =>
        TemplateVariable(key, value.unwrapped().toString, None, None, isSensitive = false, globalScope)
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
