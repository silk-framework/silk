package de.fuberlin.wiwiss.silk.instance

import xml.Node
import java.io.{DataInputStream, DataOutputStream}

/**
 * A single instance.
 */
class Instance(val uri: String, val values: IndexedSeq[Set[String]], val spec: InstanceSpecification) {
  def evaluate(path: Path): Set[String] = evaluate(spec.pathIndex(path))

  def evaluate(pathIndex: Int): Set[String] = values(pathIndex)

  override def toString = uri + "\n{\n  " + values.mkString("\n  ") + "\n}"

  def toXML = {
    <Instance uri={uri}> {
      for (valueSet <- values) yield {
        <Val> {
          for (value <- valueSet) yield {
            <e>{value}</e>
          }
        }
        </Val>
      }
    }
    </Instance>
  }

  def serialize(stream: DataOutputStream) {
    stream.writeUTF(uri)
    for (valueSet <- values) {
      stream.writeInt(valueSet.size)
      for (value <- valueSet) {
        stream.writeUTF(value)
      }
    }
  }
}

object Instance {
  def fromXML(node: Node, spec: InstanceSpecification) = {
    new Instance(
      uri = (node \ "@uri").text.trim,
      values = {
        for (valNode <- node \ "Val") yield {
          { for (e <- valNode \ "e") yield e.text }.toSet
        }
      }.toIndexedSeq,
      spec = spec
    )
  }

  def deserialize(stream: DataInputStream, spec: InstanceSpecification) = {
    //Read URI
    val uri = stream.readUTF()

    //Read Values
    def readValue = Traversable.fill(stream.readInt)(stream.readUTF).toSet
    val values = IndexedSeq.fill(spec.paths.size)(readValue)

    new Instance(uri, values, spec)
  }
}
