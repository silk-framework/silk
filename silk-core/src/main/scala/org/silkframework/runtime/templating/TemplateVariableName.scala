package org.silkframework.runtime.templating

/**
  * Holds the full name of a template variable including it's scope.
  *
  * @param name  The local name of the variable.
  * @param scope The scope as a sequence of strings forming a prefix path. May be empty.
  *              For example, a variable with name "label" and scope Seq("project", "metaData")
  *              is addressed as "project.metaData.label".
  */
class TemplateVariableName(val name: String, val scope: Seq[String] = Seq.empty) {

  /**
    * The variable name including its scope as a dot-separated string, e.g., `project.var` or `project.metaData.var`.
    * If the scope is empty, this is just the local name.
    */
  def scopedName: String = {
    (scope :+ name).mkString(".")
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

  /**
   * Parses a dot-separated full variable name into a [[TemplateVariableName]].
   * All segments except the last form the scope; the last segment is the local name.
   * For example, "project.metaData.label" parses to name="label", scope=Seq("project","metaData").
   */
  def parse(fullName: String): TemplateVariableName = {
    val parts = fullName.split('.')
    if (parts.length > 1) {
      new TemplateVariableName(parts.last, parts.dropRight(1).toSeq)
    } else {
      new TemplateVariableName(fullName, Seq.empty)
    }
  }

}
