package org.silkframework.plugins.templating.jinja

import com.hubspot.jinjava.interpret.{Context, JinjavaInterpreter}
import com.hubspot.jinjava.lib.filter.Filter
import org.silkframework.rule.input.Transformer
import org.silkframework.runtime.plugin.{ParameterValues, PluginContext, PluginDescription, PluginRegistry}
import org.silkframework.runtime.templating.IterableTemplateValues

/**
  * Makes transformer plugins available as Jinja filters.
  */
object TransformFilters {

  /**
    * Registers all available transformers as Jinja filters.
    */
  def register(context: Context): Unit = {
    for(transformerPlugin <- PluginRegistry.availablePlugins[Transformer]) {
      if(context.getFilter(transformerPlugin.id) == null) {
        context.registerFilter(new TransformFilter(transformerPlugin))
      }
    }
  }

  /**
    * A Jinja filter that is based on a transformer.
    */
  class TransformFilter(transformerPlugin: PluginDescription[Transformer]) extends Filter {

    override def getName: String = transformerPlugin.id

    override def filter(value: Any, interpreter: JinjavaInterpreter, args: String*): AnyRef = {
      // Create transformer instance with parameters
      implicit val pluginContext: PluginContext = PluginContext.empty
      val paramValues =
        for((param, value) <- transformerPlugin.parameters zip args) yield {
          (param.name, value)
        }
      val transformer = transformerPlugin(ParameterValues.fromStringMap(paramValues.toMap))

      // Evaluate transformer
      val inputValues = value match {
        case r: IterableTemplateValues => r.values
        case v: Any => Seq(v.toString)
      }
      val transformedValues = transformer(Seq(inputValues))

      // Return result
      IterableTemplateValues.fromValues(transformedValues)
    }
  }

}
