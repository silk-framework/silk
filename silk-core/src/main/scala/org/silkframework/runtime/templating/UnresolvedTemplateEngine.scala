package org.silkframework.runtime.templating

import org.silkframework.runtime.plugin.annotations.Plugin

import java.io.Writer

@Plugin(
  id = UnresolvedTemplateEngine.id,
  label = "Unresolved",
  description = "Returns the template itself without resolving it."
)
case class UnresolvedTemplateEngine() extends TemplateEngine {

  override def compile(templateString: String): CompiledTemplate = {
    new CompiledTemplate {
      override def evaluate(values: Map[String, AnyRef], writer: Writer): Unit = writer.write(templateString)
      override def evaluate(values: Seq[TemplateVariableValue], writer: Writer, evaluationConfig: EvaluationConfig): Unit = writer.write(templateString)
    }
  }
}

object UnresolvedTemplateEngine {
  final val id = "unresolved"
}
