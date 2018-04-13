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

import org.silkframework.util.Uri

import scala.xml.Node

/**
 * A single entity.
 */
class Entity private(val uri: String, val values: IndexedSeq[Seq[String]], desc: EntitySchema) extends Serializable {

  private var _schema: EntitySchema = _
  applyNewSchema(desc)

  /**
    * The EntitySchema defining the cells of the value sequence
    * @return
    */
  def schema: EntitySchema = _schema

  def applyNewSchema(newSchema: EntitySchema): Entity ={
    if(values.size < newSchema.typedPaths.size)
      throw new IllegalArgumentException("Must provide at least the same number of value sets as there are paths in the schema.")
    _schema = newSchema
    this
  }

  /**
    *
    * @param path
    * @return
    */
  def evaluate(path: Path): Seq[String] = {
    if(path.operators.isEmpty) {
      Seq(uri)
    } else {
      evaluate(_schema.pathIndex(path))
    }
  }

  /**
    * returns all values of a given property in the entity
    * @param colName
    * @return
    */
  def valueOf(colName: String): Seq[String] ={
    _schema.typedPaths.find(_.getLocalName.getOrElse("").trim == colName) match{
      case Some(col) => values(_schema.pathIndex(col.path))
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
    _schema.typedPaths.forall(tp =>{
      val ind = _schema.pathIndex(tp.path) +1                      //FIXME remove +1 CMEM-1172
      values(ind).forall(v => tp.valueType.validate(v))
    })

  }

  private var _failure: Option[Throwable] = None
  /**
    *
    * @return
    */
  def failure: Option[Throwable] = _failure

  /**
    *
    * @return
    */
  def hasFailed: Boolean = failure.isDefined

  /**
    *
    * @param t
    */
  def failEntity(t: Throwable): Unit = if(!hasFailed) _failure = Option(t)

  def toXML: Node = {
    <Entity uri={uri.toString}> {
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
    case o: Entity => this.uri.toString == o.uri.toString && this.values == o.values && this.schema == o.schema
    case _ => false
  }

  override def hashCode(): Int = {
    var hashCode = uri.toString.hashCode
    hashCode = hashCode * 31 + values.foldLeft(0)(31 * _ + _.hashCode())
    hashCode = hashCode * 31 + schema.hashCode()
    hashCode
  }
}

object Entity {

  def apply(uri: Uri, values: IndexedSeq[Seq[String]], schema: EntitySchema): Entity = {
    new Entity(uri, values, schema)
  }

  def apply(uri: String, values: IndexedSeq[Seq[String]], schema: EntitySchema): Entity = {
    new Entity(uri, values, schema)
  }

  /**
    * Instantiates a new Entity and fails it with the given Throwable
    * @param uri - uri of the entity
    * @param schema - the EntitySchema pertaining to the Entity
    * @param t - the Throwable which failed this Enity
    * @return - the failed Entity
    */
  def apply(uri: Uri, schema: EntitySchema, t: Throwable): Entity = {
    val e = new Entity(uri, IndexedSeq(), schema)
    e.failEntity(t)
    e
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
