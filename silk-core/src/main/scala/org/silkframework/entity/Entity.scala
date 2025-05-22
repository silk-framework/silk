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

import org.silkframework.config.Prefixes
import org.silkframework.entity.metadata.{EntityMetadata, GenericExecutionFailure}
import org.silkframework.entity.paths.{TypedPath, UntypedPath}
import org.silkframework.failures.FailureClass
import org.silkframework.util.StreamUtils._
import org.silkframework.util.Uri

import java.io.{ByteArrayOutputStream, DataInput, DataInputStream, DataOutput, DataOutputStream}
import java.nio.ByteBuffer
import scala.language.existentials
import scala.xml.Node

/**
  * An Entity can represent an instance of any given concept
 *
  * @param uri         - an URI as identifier
  * @param values        - A list of values of the properties defined in the provided EntitySchema
  * @param schema      - The EntitySchema defining the nature of this entity
  * @param metadata - metadata object containing all available metadata information about this object
  *                 an Entity is marked as 'failed' if [[org.silkframework.entity.metadata.EntityMetadata.failure]] is set. It becomes sealed.
  */
case class Entity(
    uri: Uri,
    values: IndexedSeq[Seq[String]],
    schema: EntitySchema,
    metadata: EntityMetadata = EntityMetadata()
  ) extends Serializable {

  def copy(
            uri: Uri = this.uri,
            values: IndexedSeq[Seq[String]] = this.values,
            schema: EntitySchema = this.schema,
            metadata: EntityMetadata = this.metadata,
            failureOpt: Option[FailureClass] = None,
            projectValuesIfNewSchema: Boolean = false
  ): Entity = this.failure match{
    case Some(_) => this                                // if origin entity has already failed, we forward it so the failure is not overwritten
    case None =>
      val actualVals = if(schema != this.schema && projectValuesIfNewSchema) shiftProperties(schema) else values  //here we remap value indices for possible shifts of typed paths
      val actualMetadata = failureOpt match{
        case Some(f) if metadata.failure.isEmpty => metadata.withFailure(f)
        case _ => metadata
      }
      new Entity(uri, actualVals, schema, actualMetadata)
  }

  /**
    * Will remap the index positions of values in case the typed paths of the EntitySchema were changed
    * @param es - the new schema
    * @return - the new value array
    */
  private def shiftProperties(es: EntitySchema): IndexedSeq[Seq[String]] ={
    es.typedPaths.map(tp => this.schema.typedPaths.find(p => p.equalsUntyped(tp)) match{
      case Some(fp) => this.evaluate(fp)
      case None => Seq()
    })
  }

  /**
    * Convenience function for applying a new schema without validating (e.g. when renaaming properties)
    * @param es - the schema
    * @return
    */
  def applyNewSchema(es: EntitySchema): Entity = copy(schema = es, projectValuesIfNewSchema = false)

  val failure: Option[GenericExecutionFailure] = {
    if(metadata.failure.isEmpty) {                                                    // if no failure has occurred yet
      if(uri.uri.trim.isEmpty){
        Some(GenericExecutionFailure(new IllegalArgumentException("Entity with an empty URI is not allowed.")))
      }
      else if (! this.validate) { // if entity is not valid
        Some(GenericExecutionFailure(new IllegalArgumentException("Provided schema does not fit entity values or sub-entities.")))
      }
      else{
        None
      }}
    else {
      metadata.failure.map(_.rootCause)   //propagate former failure
    }
  }

  /**
    * Signals if the given [[Entity]] is marked as having failed to evaluate
    */
  def hasFailed: Boolean = failure.isDefined

  /**
    * Will retrieve the values of a given path of any type (if available)
    * @param path - the property or path
    */
  def evaluate(path: UntypedPath): Seq[String] = {
    valueOfPath(path)
  }

  /**
    * Will retrieve the values of a given path (if available)
    * @param path - the property or path
    */
  def evaluate(path: TypedPath): Seq[String] = valueOfTypedPath(path)

  /**
    * returns the all values for the column index of the row representing this entity
    * @param pathIndex - the index in the value array or -1 to address the entity URI itself.
    */
  def evaluate(pathIndex: Int): Seq[String] = {
    if(pathIndex == -1) {
      Seq(uri.uri)
    } else {
      this.values(pathIndex)
    }
  }

  /**
    * returns all values of a given property in the entity
    * @param path - the property or path
    */
  def valueOfTypedPath(path: TypedPath): Seq[String] ={
    if(path.operators.isEmpty) {
      Seq(uri)
    } else {
      schema.getSchemaOfProperty(path) match {
        case Some(es) =>
          evaluate(es.indexOfTypedPath(TypedPath.removePathPrefix(path, es.subPath)))
        case None => Seq()
      }
    }
  }

  /**
    * returns all values of a given property in the entity
    * NOTE: there might be a chance that a given path exists twice with different value types, use [[valueOfTypedPath()]] instead
    * @param path - the property or path
    */
  def valueOfPath(path: UntypedPath): Seq[String] ={
    if(path.operators.isEmpty) {
      Seq(uri)
    } else {
      schema.getSchemaOfPropertyIgnoreType(path) match {
        case Some(es) =>
          evaluate(es.indexOfPath(UntypedPath.removePathPrefix(path, es.subPath)))
        case None => Seq()
      }
    }
  }

  /**
    * returns the first value (of possibly many) for the property of the given name in this entity
    * NOTE: there might be a chance that a given path exists twice with different value types, use TypedPath based version instead
    * @param property - the property name to query
    * @return
    */
  def singleValue(property: String)(implicit prefixes: Prefixes = Prefixes.default): Option[String] = valueOfPath(UntypedPath.saveApply(property)).headOption

  /**
    * returns the first value (of possibly many) for the property of the given name in this entity
    * @param path - the path to query
    * @return
    */
  def singleValue(path: TypedPath): Option[String] = valueOfTypedPath(path).headOption

  /**
    * Validates the complete value row against the given types of the schema
    * @return - the result of the validation matrix (where all values are valid)
    */
  private def validate: Boolean = {
    val entitySchema = schema match {
      case mes: MultiEntitySchema => mes.pivotSchema
      case _ => schema
    }
    val valsSize = values.size >= entitySchema.typedPaths.size
    val valsConform = entitySchema.typedPaths.zipWithIndex.forall(path => {
      if(path._2 < values.size) {
        values(path._2).forall(v => path._1.valueType.validate(v))
      } else {
        throw new ArrayIndexOutOfBoundsException(s"Entity with URI $uri is invalid. Values have size ${values.size} but schema has size ${entitySchema.typedPaths.size}.")
      }
    })
    valsSize && valsConform
  }

  def toXML: Node = {
    <Entity uri={uri.toString}>
      <Values>      {
        for (valueSet <- values) yield {
          <Val> {
            for (value <- valueSet) yield {
              <e>{value}</e>
            }
            }
          </Val>
        }
        }
      </Values>
    </Entity>
  }

  override def toString: String = failure match{
    case Some(f) => s"$uri failed with: ${f.getMessage}"
    case None => s"$uri {\n${values.mkString(", ")}\n}"
  }


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

  def empty(uri: Uri): Entity = new Entity(uri, IndexedSeq.empty, EntitySchema.empty)

  def apply(uri: String, values: IndexedSeq[Seq[String]], schema: EntitySchema): Entity = {
    new Entity(uri, values, schema)
  }

  def apply(uri: Uri, values: IndexedSeq[Seq[String]], schema: EntitySchema): Entity = {
    new Entity(uri, values, schema)
  }

  def apply(uri: String, values: IndexedSeq[Seq[String]], schema: EntitySchema, failureOpt: Option[FailureClass]): Entity = {
    new Entity(uri, values, schema, EntityMetadata(failureOpt))
  }

  /**
    * Instantiates a new Entity and fails it with the given Throwable
    * NOTE: values are all set to empty.
    * @param uri - uri of the entity
    * @param schema - the EntitySchema pertaining to the Entity
    * @param failure - the Throwable which failed this Enity as [[FailureClass]]
    * @return - the failed Entity
    */
  def apply(uri: Uri, schema: EntitySchema, failure: FailureClass): Entity = {
    val emptyValues = schema.typedPaths.map(_ => Seq.empty)
    Entity(uri, emptyValues, schema, Some(failure))
  }

  /**
    * Instantiates a new Entity and fails it with the given Throwable
    * @param uri - uri of the entity
    * @param values - the values applied for the failed Entity
    * @param schema - the EntitySchema pertaining to the Entity
    * @param failure - the Throwable which failed this Enity as [[FailureClass]]
    * @return - the failed Entity
    */
  def apply(uri: Uri, values: IndexedSeq[Seq[String]], schema: EntitySchema, failure: FailureClass): Entity = Entity(uri, values, schema, Some(failure))


  def fromXML(node: Node, desc: EntitySchema): Entity = {
    if(node == null)
      return null
    new Entity(
      uri = (node \ "@uri").text.trim,
      values = {
        for (valNode <- node \ "Values" \ "Val") yield {
          for (e <- valNode \ "e") yield e.text
        }
      }.toIndexedSeq,
      schema = desc
    )
  }

  /**
    * De-/Serializes entities from/to bytes.
    * Does not serialize the entity schema, which needs to be serialized separately.
    */
  object EntitySerializer {

    def serialize(entity: Entity, stream: DataOutput): Unit = {
      writeString(stream, entity.uri)
      for (valueSet <- entity.values) {
        stream.writeInt(valueSet.size)
        for (value <- valueSet) {
          writeString(stream, value)
        }
      }
    }

    def serializeToArray(entity: Entity): Array[Byte] = {
      val byteStream = new ByteArrayOutputStream
      val objectStream = new DataOutputStream(byteStream)
      try {
        serialize(entity, objectStream)
      } finally {
        objectStream.close()
      }
      byteStream.toByteArray
    }

    def deserialize(buffer: ByteBuffer, schema: EntitySchema): Entity = {
      val inputStream = new DataInputStream(new ByteBufferBackedInputStream(buffer))
      try {
        deserialize(inputStream, schema)
      } finally {
        inputStream.close()
      }
    }

    def deserialize(stream: DataInput, schema: EntitySchema): Entity = {
      //Read URI
      val uri = readString(stream)

      //Read Values
      def readValue = Seq.fill(stream.readInt)(readString(stream))

      schema match {
        case mes: MultiEntitySchema =>
          val values = IndexedSeq.fill(mes.pivotSchema.typedPaths.size)(readValue)
          Entity(uri, values, mes)
        case es: EntitySchema =>
          val values = IndexedSeq.fill(schema.typedPaths.size)(readValue)
          Entity(uri, values, es)
      }
    }
  }

}
