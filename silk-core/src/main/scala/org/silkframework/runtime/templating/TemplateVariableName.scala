package org.silkframework.runtime.templating

/**
  * Holds the full name of a template variable including it's scope.
  *
  * @param name  The local name of the variable.
  * @param scope The scope. May be empty.
  */
class TemplateVariableName(val name: String, val scope: String) {

  /**
    * The variable name including its scope, e.g., `project.var`
    */
  def scopedName: String = {
    if (scope.nonEmpty) {
      scope + "." + name
    } else {
      name
    }
  }

  override def toString: String = {
    scopedName
  }

  override def equals(other: Any): Boolean = other match {
    case that: TemplateVariableName =>
      other.isInstanceOf[TemplateVariableName] && name == that.name && scope == that.scope
    case _ => false
  }

  override def hashCode(): Int = {
    Seq(name, scope).map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}

object TemplateVariableName {

  def parse(fullName: String): TemplateVariableName = {
    val pointIndex = fullName.indexOf('.'.toInt)
    if(pointIndex != -1) {
      new TemplateVariableName(fullName.substring(pointIndex + 1), fullName.substring(0, pointIndex))
    } else {
      new TemplateVariableName(fullName, "")
    }
  }

}
