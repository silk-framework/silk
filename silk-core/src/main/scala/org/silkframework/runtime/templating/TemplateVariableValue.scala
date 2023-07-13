package org.silkframework.runtime.templating

/**
  * Holds the full name and value of a template variable.
  *
  * @param name   The local name of the variable.
  * @param scope  The scope. May be empty.
  * @param values The values for this variable.
  */
class TemplateVariableValue(name: String, scope: String, val values: Seq[String]) extends TemplateVariableName(name, scope) {

  def asName: TemplateVariableName = {
    new TemplateVariableName(name, scope)
  }

  override def toString: String = {
    s"$scopedName=${values.mkString(", ")}"
  }

  override def equals(other: Any): Boolean = other match {
    case that: TemplateVariableValue =>
      super.equals(that) && values == that.values
    case _ => false
  }

  override def hashCode(): Int = {
    Seq(super.hashCode(), values).map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}
