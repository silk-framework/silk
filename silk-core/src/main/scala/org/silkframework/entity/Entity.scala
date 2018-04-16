/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.silkframework.entity

import java.io.{DataInput, DataOutput}

import scala.xml.Node

/**
 * A single entity.
 */
class Entity(val uri: String, val values: IndexedSeq[Seq[String]], val desc: EntitySchema) extends Serializable {
  require(values.size == desc.typedPaths.size, "Must provide the same number of value sets as there are paths in the schema.")

  /**
    *
    * @param path
    * @return
    */
  def evaluate(path: Path): Seq[String] = {
    if(path.operators.isEmpty) {
      Seq(uri)
    } else {
      evaluate(desc.pathIndex(path))
    }
  }

  /**
    * returns all values of a given property in the entity
    * @param colName
    * @return
    */
  def valueOf(colName: String): Seq[String] ={
    desc.typedPaths.find(_.getLocalName.getOrElse("").trim == colName) match{
      case Some(col) => values(desc.pathIndex(col.path))
      case None => Seq()
    }
  }

  /**
    * returns the first value (of possibly many) for the property of the given name in this entity
    * @param columnName
    * @return
    */
  def singleValue(columnName: String): Option[String] = valueOf(columnName).headOption

  /**
    * returns the all values for the column index of the row representing this entity
    * @param pathIndex
    * @return
    */
  def evaluate(pathIndex: Int): Seq[String] = values(pathIndex)

  /**
    * Validates the complete value row against the given types of the schema
    * @return - the result of the validation matrix (where all values are valid)
    */
  def validate: Boolean = {
    desc.typedPaths.forall(tp =>{
      val ind = desc.pathIndex(tp.path) +1                      //TODO remove +1
      values(ind).forall(v => tp.valueType.validate(v))
    })
  }

  def toXML: Node = {
    <Entity uri={uri}> {
      for (valueSet <- values) yield {
        <Val> {
          for (value <- valueSet) yield {
            <e>{value}</e>
          }
        }
        </Val>
      }
    }
    </Entity>
  }

  def serialize(stream: DataOutput) {
    stream.writeUTF(uri)
    for (valueSet <- values) {
      stream.writeInt(valueSet.size)
      for (value <- valueSet) {
        stream.writeUTF(value)
      }
    }
  }

  override def toString: String = uri + "\n{\n  " + values.mkString("\n  ") + "\n}"

  override def equals(other: Any): Boolean = other match {
    case o: Entity => this.uri == o.uri && this.values == o.values && this.desc == o.desc
    case _ => false
  }

  override def hashCode(): Int = {
    var hashCode = uri.hashCode
    hashCode = hashCode * 31 + values.foldLeft(0)(31 * _ + _.hashCode())
    hashCode = hashCode * 31 + desc.hashCode()
    hashCode
  }
}

object Entity {

  def apply(uri: String, values: IndexedSeq[Seq[String]], schema: EntitySchema): Entity = {
    new Entity(uri, values, schema)
  }

  def fromXML(node: Node, desc: EntitySchema): Entity = {
    new Entity(
      uri = (node \ "@uri").text.trim,
      values = {
        for (valNode <- node \ "Val") yield {
          for (e <- valNode \ "e") yield e.text
        }
      }.toIndexedSeq,
      desc = desc
    )
  }

  def deserialize(stream: DataInput, desc: EntitySchema): Entity = {
    //Read URI
    val uri = stream.readUTF()

    //Read Values
    def readValue = Seq.fill(stream.readInt)(stream.readUTF)
    val values = IndexedSeq.fill(desc.typedPaths.size)(readValue)

    new Entity(uri, values, desc)
  }
}
