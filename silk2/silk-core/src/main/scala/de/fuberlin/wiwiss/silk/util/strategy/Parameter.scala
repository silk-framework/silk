package de.fuberlin.wiwiss.silk.util.strategy

case class Parameter(name: String, dataType: Parameter.Type, description: String = "No description", defaultValue: Option[AnyRef] = None) {
  def apply(obj: AnyRef): AnyRef = {
    obj.getClass.getMethod(name).invoke(obj)
  }
}

object Parameter {

  object Type extends Enumeration {
    val String = Value("String")
    val Char = Value("Char")
    val Int = Value("Int")
    val Double = Value("Double")
    val Boolean = Value("Boolean")
  }

  type Type = Type.Value
}