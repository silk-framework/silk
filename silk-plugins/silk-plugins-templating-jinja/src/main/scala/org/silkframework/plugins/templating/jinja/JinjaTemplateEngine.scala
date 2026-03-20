package org.silkframework.plugins.templating.jinja

import com.hubspot.jinjava.interpret.{InterpretException, JinjavaInterpreter, UnknownTokenException}
import com.hubspot.jinjava.tree.Node
import com.hubspot.jinjava.{Jinjava, JinjavaConfig}
import org.silkframework.runtime.plugin.annotations.Plugin
import org.silkframework.runtime.templating.exceptions.{TemplateEvaluationException, UnboundVariablesException}
import org.silkframework.runtime.templating.{CompiledTemplate, EvaluationConfig, TemplateEngine, TemplateVariableName, TemplateVariableValue}

import java.io.Writer
import java.util.EmptyStackException
import scala.jdk.CollectionConverters.ListHasAsScala
import scala.util.control.Breaks.{break, breakable}

@Plugin(
  id = JinjaTemplateEngine.id,
  label = "Jinja"
)
case class JinjaTemplateEngine() extends TemplateEngine {

  override def compile(templateString: String): JinjaTemplate = {
    new JinjaTemplate(JinjaTemplateEngine.interpreter().parse(templateString))
  }
}

object JinjaTemplateEngine {

  final val id = "jinja"

  private val interpreters = new ThreadLocal[JinjavaInterpreter] {
    override protected def initialValue(): JinjavaInterpreter = {
      // There is a bug in Jinja 2.6.0, if a different context class loader is used: https://github.com/HubSpot/jinjava/issues/317
      val curClassLoader = Thread.currentThread.getContextClassLoader
      try {
        Thread.currentThread.setContextClassLoader(this.getClass.getClassLoader)
        val config = JinjavaConfig.newBuilder.withFailOnUnknownTokens(true).build()
        val jinja = new Jinjava(config)
        TransformFilters.register(jinja.getGlobalContext)
        val interpreter = jinja.newInterpreter()
        JinjavaInterpreter.pushCurrent(interpreter) // Macros will request the current interpreter (thread-local)
        interpreter
      } finally {
        Thread.currentThread.setContextClassLoader(curClassLoader)
      }
    }
  }

  /**
   * Retrieves an interpreter instance.
   */
  def interpreter(): JinjavaInterpreter = {
    val inter = interpreters.get()
    // We need to reset a number of properties.
    // It would be better to change this to create a fresh instance on every call. But then we need to check carefully for memory leaks.
    inter.getContext.reset()
    inter.getContext.clear()
    breakable {
      while(true) {
        try {
          inter.getContext.popRenderStack()
        } catch {
          case _: EmptyStackException =>
            break()
        }
      }
    }
    do {
      inter.removeLastError()
    } while(!inter.getLastError.isEmpty)
    inter
  }

}

class JinjaTemplate(val node: Node) extends CompiledTemplate {

  override val variables: Option[Seq[TemplateVariableName]] = {
    Some(new JinjaVariableCollector().collect(node).unboundVars)
  }

  override def evaluate(values: Seq[TemplateVariableValue], writer: Writer, evaluationConfig: EvaluationConfig = EvaluationConfig()): Unit = {
    // Check if values for all variables are provided
    // We do this explicitly because the Jinja-internal checks are not sufficient
    // (The implementation ignores expressions with filters and only returns the first missing var)
    var missingVars: Seq[TemplateVariableName] = Seq.empty
    for (vars <- variables) {
      // Collect all scoped variables of the form 'scope.name'
      val names = values.map(_.asName)
      // Variables of the form 'scope.name' can also be addressed as just 'scope'
      val scopes = values.map(v => new TemplateVariableName(v.scope, ""))
      // Find missing vars
      val existingVars = (names ++ scopes).toSet
      missingVars = vars.filterNot(existingVars.contains)
    }
    if (missingVars.nonEmpty) {
      if(evaluationConfig.ignoreUnboundVariables) {
        // Leave unbound variables as they are in the result.
        val extendedValues = values ++ missingVars.map(mv => new TemplateVariableValue(mv.name, mv.scope, Seq(mv.scopedName)))
        evaluate(convertValues(extendedValues), writer)
      } else {
        throw new UnboundVariablesException(missingVars)
      }
    } else {
      evaluate(convertValues(values), writer)
    }
  }

  override def evaluate(values: Map[String, AnyRef], writer: Writer): Unit = {
    // Render the template
    val interpreter = JinjaTemplateEngine.interpreter()
    for ((key, value) <- values) {
      interpreter.getContext.put(key, value)
    }
    try {
      writer.write(interpreter.render(node, false))
    } catch {
      case ex: UnknownTokenException =>
        throw new UnboundVariablesException(Seq(TemplateVariableName.parse(ex.getToken)), Some(ex))
      case ex: InterpretException =>
        throw new TemplateEvaluationException(ex.getMessage, Some(ex))
    }

    // For now, we just throw any errors. In the future, we could improve this and add an error collector.
    if (!interpreter.getErrors.isEmpty) {
      val msg = "Errors in template: " + interpreter.getErrors.asScala.map(_.getMessage).mkString(" ")
      val cause = Option(interpreter.getErrors.get(0).getException)
      throw new TemplateEvaluationException(msg, cause)
    }
  }


}
