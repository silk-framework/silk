package org.silkframework.runtime.plugin.types

import scala.language.implicitConversions

case class TemplateParameter(templateStr: String)

object TemplateParameter {
  implicit def str2parameter(str: String): TemplateParameter = TemplateParameter(str)
  implicit def templateParameter2str(templateParameter: TemplateParameter): String = templateParameter.templateStr
}