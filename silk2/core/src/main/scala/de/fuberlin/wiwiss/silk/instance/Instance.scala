package de.fuberlin.wiwiss.silk.instance

import xml.Node
import java.io.{DataInputStream, InputStream, DataOutputStream, OutputStream}

/**
 * A single instance.
 */
class Instance(val uri : String, values : Array[Set[String]], val spec : InstanceSpecification)
{
  def evaluate(path : Path) : Set[String] = values(spec.pathIndex(path))

  def evaluate(pathIndex : Int) : Set[String] = values(pathIndex)

  override def toString = uri + "\n{\n  " + values.mkString("\n  ") + "\n}"

  def toXML =
  {
    <Instance uri={uri}>{
      for(valueSet <- values) yield
      {
        <Val>{
          for(value <- valueSet) yield
          {
            <e>{value}</e>
          }
        }</Val>
      }
    }</Instance>
  }

  def serialize(stream : OutputStream)
  {
    val dataSream = new DataOutputStream(stream)
    dataSream.writeUTF(uri)
    for(valueSet <- values)
    {
      dataSream.writeInt(valueSet.size)
      for(value <- valueSet)
      {
        dataSream.writeUTF(value)
      }
    }
  }
}

object Instance
{
  def fromXML(node : Node, spec : InstanceSpecification) =
  {
    new Instance(
      uri = node \ "@uri" text,
      values =
      {
        for(valNode <- node \ "Val") yield
        {
          {for(e <- valNode \ "e") yield e text}.toSet
        }
      }.toArray,
      spec = spec
    )
  }

  def deserialize(stream : InputStream, spec : InstanceSpecification) =
  {
    val dataStream = new DataInputStream(stream)

    val uri = dataStream.readUTF()
    val values = Array.fill(spec.paths.size)(Traversable.fill(dataStream.readInt)(dataStream.readUTF).toSet)

    new Instance(uri, values, spec)
  }
}
