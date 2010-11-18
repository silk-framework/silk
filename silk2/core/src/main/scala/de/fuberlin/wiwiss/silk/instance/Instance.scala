package de.fuberlin.wiwiss.silk.instance

import xml.Node

/**
 * A single instance.
 */
@serializable
@SerialVersionUID(0)
class Instance(val variable : String, val uri : String, values : Map[Int, Set[String]])
{
  def evaluate(path : Path) : Set[String] = values.get(path.id).getOrElse(Set())

  override def toString = uri + "\n{\n  " + values.values.mkString("\n  ") + "\n}"

  def toXML =
  {
    <Instance uri={uri} var={variable}>{
      for((key, valueSet) <- values) yield
      {
        <Val key={key.toString}>{
          for(value <- valueSet) yield
          {
            <e>{value}</e>
          }
        }</Val>
      }
    }</Instance>
  }
}

object Instance
{
  def fromXML(node : Node) =
  {
    new Instance(
      variable = node \ "@var" text,
      uri = node \ "@uri" text,
      values =
      {
        for(valNode <- node \ "Val") yield
        {
          ((valNode \ "@key" text).toInt, {for(e <- valNode \ "e") yield e text}.toSet)
        }
      }.toMap
    )
  }
}
