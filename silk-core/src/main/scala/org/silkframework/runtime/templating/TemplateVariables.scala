package org.silkframework.runtime.templating

import java.io.StringWriter
import java.util
import scala.collection.JavaConverters.mapAsJavaMapConverter

/**
  * Holds a set of variables that can be used in parameter value templates.
  */
case class TemplateVariables(map: Map[String, TemplateVariable]) {

  /**
    * Returns variables as a map to be used in template evaluation.
    * The key is the scope and the values are all variables for this scope as a Java map.
    */
  def variableMap: Map[String, util.Map[String, String]] = {
    map.groupBy(_._2.scope).mapValues(_.mapValues(_.value).asJava)
  }

  /**
    * Lists all available variable names.
    */
  def variableNames: Seq[String] = {
    for (variable <- map.values.toSeq.sortBy(_.key)) yield {
      variable.scope + "." + variable.key
    }
  }

  /**
    * Resolves a template string.
    *
    * @throws TemplateEvaluationException If the template evaluation failed.
    * */
  def resolveTemplateValue(value: String): String = {
    val writer = new StringWriter()
    GlobalTemplateVariablesConfig.templateEngine().compile(value).evaluate(variableMap, writer)
    writer.toString
  }

}

/**
  * A single template variable.
  */
case class TemplateVariable(key: String, value: String, scope: String, isSensitive: Boolean)

object TemplateVariables {

  def empty: TemplateVariables = TemplateVariables(Map.empty)

}
