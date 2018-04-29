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
class Entity private(val uri: Uri, private val vals: IndexedSeq[Seq[String]], private val desc: EntitySchema) extends Serializable {

  private var _failure: Option[Throwable] = None
  private var _schema: EntitySchema = _
  private var _values: IndexedSeq[Seq[String]] = _
  setValues(vals)
  applyNewSchema(desc)

  private def setValues(vals: IndexedSeq[Seq[String]]): Unit ={
    def handleNullsInValueSeq(valueSeq: Seq[Any]) = {
      if(valueSeq == null)
        Seq()
      else
        valueSeq.flatMap(x => Option(x).map(_.toString))

// FIXME switch to a metadata map to not only record exceptions CMEM-719       if (!nullArrayLogged) {
//          nullArrayLogged = true
//          EventLog warn "Spark Array value contains null values!"
//        }
    }

    _values = vals.map(handleNullsInValueSeq)
  }

  def values: IndexedSeq[Seq[String]] = _values

  /**
    * The EntitySchema defining the cells of the value sequence
    * @return
    */
  def schema: EntitySchema = _schema

  /**
    * will apply a modified schema
    * NOTE: The number of values per row must be as least as great as the number of typed paths
    * NOTE: adding additional TypedPaths not instantiated in the values of the row will fail validation (use withProperty(..) instead)
    * @param newSchema - the new schema to be applied
    * @return - this
    */
  def applyNewSchema(newSchema: EntitySchema, validate: Boolean = true): Entity ={
    _schema = newSchema

    if(validate && (this.values.size < newSchema.typedPaths.size || !this.validate))
      failEntity(new IllegalArgumentException("Provided schema does not fit entity values."))

    setValues(_schema.typedPaths.zipWithIndex.map(tp =>values(tp._2)))
    this
  }

  /**
    * Will retrieve the values of a given path (if available)
    * @param path
    * @return
    */
  @deprecated("Use evaluate(path: TypedPath) instead, since uniqueness of paths are only guaranteed with provided ValueType.", "18.03")
  def evaluate(path: Path): Seq[String] = {
    if(path.operators.isEmpty) {
      Seq(uri)
    } else {
      evaluate(_schema.pathIndex(path))
    }
  }

  /**
    * Will retrieve the values of a given path (if available)
    * @param path
    * @return
    */
  def evaluate(path: TypedPath): Seq[String] = {
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
    _schema.propertyNames.zipWithIndex.find(_._1 == colName) match{
      case Some((_, ind)) => values(ind)
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
    _schema.typedPaths.zipWithIndex.forall(tp =>{
      values(tp._2).forall(v => tp._1.valueType.validate(v))
    })
  }

  /**
    * @return - the exception responsible for this ENtity to fail
    */
  def failure: Option[Throwable] = _failure

  def hasFailed: Boolean = failure.isDefined

  /**
    * Will fail this entity with the provided exception (if not already failed)
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
    hashCode = hashCode * 31 + values.foldLeft(1)(31 * _ + _.hashCode())
    hashCode = hashCode * 31 + schema.hashCode()
    hashCode
  }
}

object Entity {

  def empty(uri: Uri): Entity = new Entity(uri, IndexedSeq(), EntitySchema.empty)

  def apply(uri: Uri, values: IndexedSeq[Seq[String]], schema: EntitySchema): Entity = {
    new Entity(uri, values, schema)
  }

  def apply(uri: String, values: IndexedSeq[Seq[String]], schema: EntitySchema): Entity = {
    new Entity(uri, values, schema)
  }

  /**
    * Instantiates a new Entity and fails it with the given Throwable
    * NOTE: values are not recorded
    * @param uri - uri of the entity
    * @param schema - the EntitySchema pertaining to the Entity
    * @param t - the Throwable which failed this Enity
    * @return - the failed Entity
    */
  //FIXME add property option CMEM-719
  def apply(uri: Uri, schema: EntitySchema, t: Throwable): Entity = {
    val e = empty(uri)
    e.failEntity(t)
    e._values = schema.typedPaths.map(x => Seq())
    e.applyNewSchema(schema, validate = false)
    e
  }

  /**
    * Instantiates a new Entity and fails it with the given Throwable
    * @param uri - uri of the entity
    * @param values - the values applied for the failed Entity
    * @param schema - the EntitySchema pertaining to the Entity
    * @param t - the Throwable which failed this Enity
    * @return - the failed Entity
    */
  //FIXME add property option CMEM-719
  def apply(uri: Uri, values: IndexedSeq[Seq[String]], schema: EntitySchema, t: Throwable): Entity = {
    val e = empty(uri)
    e.failEntity(t)
    e._values = values
    e.applyNewSchema(schema)
    e
  }

  def fromXML(node: Node, desc: EntitySchema): Entity = {
    new Entity(
      uri = (node \ "@uri").text.trim,
      vals = {
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
