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

import org.silkframework.entity.metadata.{EntityMetadata, EntityMetadataXml}
import org.silkframework.util.Uri

import scala.xml.Node
import scala.language.existentials

/**
  * An Entity can represent an instance of any given concept
  * @param uri         - an URI as identifier
  * @param vals        - A list of values of the properties defined in the provided EntitySchema
  * @param schema      - The EntitySchema defining the nature of this entity
  * @param subEntities - optional, each entity can be composed of multiple sub-entities if defined with a suitable MultiEntitiySchema
  * @param metadata    - metadata object containing all available metadata information about this object
  *                    an Entity is marked as 'failed' if [[org.silkframework.entity.metadata.EntityMetadata.failure]] is set. It becomes sealed.
  */
case class Entity private(
    uri: Uri,
    private val vals: IndexedSeq[Seq[String]],
    schema: EntitySchema,
    subEntities: IndexedSeq[Option[Entity]] = IndexedSeq.empty,
    metadata: EntityMetadata[_] = EntityMetadataXml()
  ) extends Serializable {

  def copy(
    uri: Uri = this.uri,
    values: IndexedSeq[Seq[String]] = this.values,
    schema: EntitySchema = this.schema,
    subEntities: IndexedSeq[Option[Entity]] = this.subEntities,
    metadata: EntityMetadata[_] = this.metadata,
    failureOpt: Option[Throwable] = None,
    projectValuesIfNewSchema: Boolean = true
  ): Entity = this.failure match{
    case Some(_) => this                                // if origin entity has already failed, we forward it so the failure is not overwritten
    case None =>
      val actualVals = if(schema != this.schema && projectValuesIfNewSchema) applyNewSchema(schema) else values  //here we remap value indices for possible shifts of typed paths
      val actualSubs = if(schema != this.schema && projectValuesIfNewSchema) subEntities.map(o => o.map(e => e.copy(schema = schema))) else subEntities
      val actualMetadata = failureOpt match{
        case Some(f) if metadata.failure.metadata.isEmpty => metadata.addFailure(f)
        case _ => metadata
      }
      new Entity(uri, actualVals, schema, actualSubs, actualMetadata)
  }

  /**
    * Will remap the index positions of values in case the typed paths of the EntitySchema were changed
    * @param es - the new schema
    * @return - the new value array
    */
  private def applyNewSchema(es: EntitySchema): IndexedSeq[Seq[String]] ={
    es.typedPaths.map(tp => this.schema.typedPaths.find(t => t == tp) match{
      case Some(fp) if fp != TypedPath.empty => this.evaluate(fp)
      case None => Seq()
    })
  }

  val values: IndexedSeq[Seq[String]] = vals.map(Entity.handleNullsInValueSeq)

  val failure: Option[Throwable] = if(metadata.failure.metadata.isEmpty) {                                                    // if no failure has occurred yet
    if(schema.isInstanceOf[MultiEntitySchema] && schema.asInstanceOf[MultiEntitySchema].subSchemata.size < subEntities.size)  // if sub entities size is not equal to sub schemata size
      Some(new IllegalArgumentException("Number of sub-entities is not equal to the number of sub-schemata for: " + uri))
    else if (! this.validate)                                                                                                 // if entity is not valid
      Some(new IllegalArgumentException("Provided schema does not fit entity values or sub-entities."))
    else
      None
  }
  else {
    metadata.failure.metadata   //propagate former failure
  }

  def hasFailed: Boolean = failure.isDefined

  /**
    * Will retrieve the values of a given path (if available)
    * @param path
    * @return
    */
  @deprecated("Use evaluate(path: TypedPath) instead, since uniqueness of paths are only guaranteed with provided ValueType.", "18.03")
  def evaluate(path: Path): Seq[String] = {
    valueOf(path.asAutoDetectTypedPath)
  }

  /**
    * Will retrieve the values of a given path (if available)
    * @param path
    * @return
    */
  def evaluate(path: TypedPath): Seq[String] = valueOf(path)

  /**
    * returns the all values for the column index of the row representing this entity
    * @param pathIndex - the index in the value array
    * @return
    */
  private[entity] def evaluate(pathIndex: Int): Seq[String] = values(pathIndex)


  def valueOf(property: String): Seq[String] = valueOf(Path(property.trim).asAutoDetectTypedPath)

  /**
    * returns all values of a given property in the entity
    * @param path - the path you want values of
    * @return
    */
  def valueOf(path: TypedPath): Seq[String] ={
    if(path.operators.isEmpty) {
      Seq(uri)
    } else {
      schema.getSchemaOfProperty(path) match {
        case Some(es) =>
          //if pertaining schema is this schema or its the pivot schema of a MultiEntitySchema
          val ent = if (es == schema || schema.isInstanceOf[MultiEntitySchema] && schema.asInstanceOf[MultiEntitySchema].pivotSchema == es) {
            this
          }
          else {
            subEntities.flatten.find(e => e.schema == es).getOrElse(return Seq())
          }
          //now find the pertaining index and get values
          ent.evaluate(es.pathIndex(path))
        case None => Seq()
      }
    }
  }

  /**
    * returns the first value (of possibly many) for the property of the given name in this entity
    * @param property - the property name to query
    * @return
    */
  def singleValue(property: String): Option[String] = valueOf(Path(property).asAutoDetectTypedPath).headOption

  /**
    * returns the first value (of possibly many) for the property of the given name in this entity
    * @param path - the path to query
    * @return
    */
  def singleValue(path: TypedPath): Option[String] = valueOf(path).headOption

  /**
    * Validates the complete value row against the given types of the schema
    * @return - the result of the validation matrix (where all values are valid)
    */
  def validate: Boolean = {
    val tps = schema match {
      case mes: MultiEntitySchema => mes.pivotSchema
      case _ => schema
    }
    val valsSize = values.size >= tps.typedPaths.size
    val valsConform = tps.typedPaths.zipWithIndex.forall(tp =>{
      if(tp._2 < values.size)
        values(tp._2).forall(v => tp._1.valueType.validate(v))
      else
        throw new ArrayIndexOutOfBoundsException(tp._2)
    })
    val subEntsValid = schema match{
      case mes: MultiEntitySchema => subEntities.zip(mes.subSchemata).forall(se => se._1.isEmpty || se._2 == se._1.get.schema && se._1.get.validate)
      case _: EntitySchema => true
    }
    valsSize && valsConform && subEntsValid
  }

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

  def empty(uri: Uri): Entity = new Entity(uri, IndexedSeq.empty, EntitySchema.empty)

  def apply(uri: Uri, values: IndexedSeq[Seq[String]], schema: EntitySchema, subEntities: IndexedSeq[Option[Entity]]): Entity = {
    new Entity(uri, values, schema, subEntities)
  }

  def apply(uri: String, values: IndexedSeq[Seq[String]], schema: EntitySchema, subEntities: IndexedSeq[Option[Entity]]): Entity = {
    new Entity(uri, values, schema, subEntities)
  }

  def apply(uri: Uri, values: IndexedSeq[Seq[String]], schema: EntitySchema): Entity = {
    new Entity(uri, values, schema)
  }

  def apply(uri: String, values: IndexedSeq[Seq[String]], schema: EntitySchema): Entity = {
    new Entity(uri, values, schema)
  }

  def apply(uri: String, values: IndexedSeq[Seq[String]], schema: EntitySchema, subEntities: IndexedSeq[Option[Entity]], failureOpt: Option[Throwable]): Entity = {
    new Entity(uri, values, schema, subEntities, failureOpt match{
      case Some(t) => EntityMetadataXml(t)
      case None => EntityMetadataXml()
    })
  }

  def handleNullsInValueSeq(valueSeq: Seq[String]): Seq[String] = if(valueSeq == null) Seq() else valueSeq.flatMap(x => Option(x))

  /**
    * Instantiates a new Entity and fails it with the given Throwable
    * NOTE: values are not recorded
    * @param uri - uri of the entity
    * @param schema - the EntitySchema pertaining to the Entity
    * @param t - the Throwable which failed this Enity
    * @return - the failed Entity
    */
  //FIXME add property option CMEM-719
  def apply(uri: Uri, schema: EntitySchema, t: Throwable): Entity = Entity(uri, IndexedSeq(), schema, IndexedSeq(), Some(t))

  /**
    * Instantiates a new Entity and fails it with the given Throwable
    * @param uri - uri of the entity
    * @param values - the values applied for the failed Entity
    * @param schema - the EntitySchema pertaining to the Entity
    * @param t - the Throwable which failed this Enity
    * @return - the failed Entity
    */
  //FIXME add property option CMEM-719 maybe only allow SparkInstanceException
  def apply(uri: Uri, values: IndexedSeq[Seq[String]], schema: EntitySchema, t: Throwable): Entity = Entity(uri, values, schema, IndexedSeq())


  def fromXML(node: Node, desc: EntitySchema): Entity = {
    new Entity(
      uri = (node \ "@uri").text.trim,
      vals = {
        for (valNode <- node \ "Val") yield {
          for (e <- valNode \ "e") yield e.text
        }
      }.toIndexedSeq,
      schema = desc
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
