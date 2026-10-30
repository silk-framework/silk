package org.silkframework.runtime.templating

import com.typesafe.config.{Config, ConfigObject, ConfigOrigin, ConfigValueType, ConfigValue => TypesafeConfigValue}
import org.silkframework.config.{ConfigValue, InvalidConfigException}
import org.silkframework.runtime.plugin.StringParameterType.PasswordParameterType
import org.silkframework.runtime.plugin.types.PasswordParameter

import java.util.logging.{Level, Logger}
import scala.jdk.CollectionConverters.SetHasAsScala
import scala.util.control.NonFatal

/**
  * Global template variables, which are defined in the configuration.
  */
object GlobalTemplateVariablesConfig {

  private val log: Logger = Logger.getLogger(getClass.getName)

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
        for (entry <- config.getConfig(variablesConfigVar).root().entrySet().asScala.toSeq;
             variable <- readVariable(entry.getKey, entry.getValue)) yield {
          variable
        }
      TemplateVariables(variables)
    } else {
      TemplateVariables.empty
    }
  }

  private def readVariable(key: String, value: TypesafeConfigValue): Option[TemplateVariable] = {
    value match {
      case objValue: ConfigObject =>
        val configKey = configNamespace + ".global." + key
        val value = Option(objValue.get("value"))
          .getOrElse(throw new InvalidConfigException(configKey, objValue.origin(), "Parameter 'value' is missing."))
        val description = if(objValue.containsKey("description")) Some(objValue.toConfig.getString("description")) else None
        val isSensitive = if(objValue.containsKey("isSensitive")) objValue.toConfig.getBoolean("isSensitive") else false
        val plainValue = if(isSensitive) decrypted(configKey, objValue.origin(), value.unwrapped().toString) else Some(value.unwrapped().toString)
        plainValue.map(TemplateVariable(key, _, None, description, isSensitive = isSensitive, VariableScope.global))
      case _ =>
        Some(TemplateVariable(key, value.unwrapped().toString, None, None, isSensitive = false, VariableScope.global))
    }
  }

  /**
    * A sensitive variable may be configured in the encrypted form that password parameters are serialized in,
    * so that the configuration does not hold the plain value.
    * A value that cannot be decrypted leaves the variable out, so that only the templates that use it fail.
    */
  private def decrypted(configKey: String, origin: ConfigOrigin, value: String): Option[String] = {
    if(value.startsWith(PasswordParameterType.PREAMBLE)) {
      try {
        Some(PasswordParameter.decrypt(value.stripPrefix(PasswordParameterType.PREAMBLE)))
      } catch {
        case NonFatal(ex) =>
          log.log(Level.WARNING, s"The global variable '$configKey' (${origin.description()}) is left out, since its encrypted value " +
            s"cannot be decrypted with the key configured at '${PasswordParameterType.CONFIG_KEY}'.", ex)
          None
      }
    } else {
      Some(value)
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
  override def scopes: Set[VariableScope] = Set(VariableScope.global)

  /**
    * Retrieves all template variables.
    */
  override def all: TemplateVariables = GlobalTemplateVariablesConfig.variables()

}
