package org.silkframework.runtime.plugin.types

import org.silkframework.runtime.templating.{TemplateVariables, TemplateVariablesReader}

case class TemplateParameter(templateStr: String, templateVariables: TemplateVariablesReader) {

  def nonSensitiveVariables: TemplateVariables = {
    templateVariables.all.withoutSensitiveVariables()
  }

}
