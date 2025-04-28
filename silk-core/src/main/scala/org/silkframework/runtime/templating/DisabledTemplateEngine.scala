package org.silkframework.runtime.templating

import org.silkframework.runtime.plugin.annotations.Plugin

@Plugin(
  id = DisabledTemplateEngine.id,
  label = "Disabled",
  description = "Disables the support for templates. Will throw an error if templates are used."
)
case class DisabledTemplateEngine() extends TemplateEngine {

  override def compile(templateString: String): CompiledTemplate = {
    throw new UnsupportedOperationException("Templates have been disabled.")
  }
}

object DisabledTemplateEngine {
  final val id = "disabled"
}
