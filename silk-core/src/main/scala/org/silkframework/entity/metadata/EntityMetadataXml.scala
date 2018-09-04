package org.silkframework.entity.metadata

import org.silkframework.entity.metadata.EntityMetadata.{FAILURE_KEY, METADATA_KEY}
import org.silkframework.entity.metadata.EntityMetadataXml.XmlSerializer
import org.silkframework.failures.FailureClass
import org.silkframework.runtime.serialization.{ReadContext, SerializationFormat, WriteContext, XmlFormat}

import scala.xml.Node


case class EntityMetadataXml(override val metadata: Map[String, LazyMetadata[_, Node]] = Map[String, LazyMetadata[_, Node]]()) extends EntityMetadata[Node] {

  def this(rawMetadata: String) = {
    this(XmlSerializer.fromString(rawMetadata, XmlFormat.MIME_TYPE_TEXT)(ReadContext()))
  }

  override def addFailure(failure: FailureClass): EntityMetadata[Node] = {
    val lm = LazyMetadataXml(failure, EntityMetadata.FAILURE_KEY)(classOf[FailureClass])
    addReplaceMetadata(EntityMetadata.FAILURE_KEY, lm)
  }

  override implicit val serTag: Class[Node] = classOf[Node]

  /**
    * providing an empty instance
    */
  override def emptyEntityMetadata: EntityMetadata[Node] = EntityMetadataXml()

  /**
    * The serializer used to serialize this EntityMetadata object
    */
  override val serializer: SerializationFormat[EntityMetadata[Node], Node] = XmlSerializer

  /**
    * Will insert a new [[LazyMetadata]] object into the metadata map
    *
    * @param key - string identifiers to specify the type of metadata (see for example: [[XmlMetadataSerializer.metadataId]])
    * @param lm  - the [[LazyMetadata]] object
    * @return - the updated map
    */
  override def addReplaceMetadata(key: String, lm: LazyMetadata[_, Node]): EntityMetadata[Node] =
    EntityMetadataXml((metadata.toSeq.filterNot(_._1 == key) ++ Seq(key -> lm)).toMap)
}

object EntityMetadataXml{

  EntityMetadata.registerNewEntityMetadataFormat(EntityMetadataXml())

  type CT >: Any <: Any

  def apply[Typ](map: Map[String, Typ])(implicit typTag: Class[Typ]): EntityMetadata[Node] = {
    val resMap = map.map(ent => {
      val serializer = XmlMetadataSerializer.getSerializationFormat[Typ](ent._1).getOrElse(throw new IllegalArgumentException("Unknown metadata category: " + ent._1))
      ent._1 -> LazyMetadataXml(ent._2, serializer)(typTag)
    })
    EntityMetadataXml(resMap)
  }

  def apply(t: FailureClass): EntityMetadata[Node] = apply(Map(FAILURE_KEY -> t))(classOf[FailureClass])

  def apply(value: String): EntityMetadata[Node] = XmlSerializer.fromString(value, XmlFormat.MIME_TYPE_TEXT)(ReadContext())

  /**
    * The XML serializer used to serialize EntityMetadataXml
    */
  object XmlSerializer extends XmlMetadataSerializer[EntityMetadata[Node]]{
    override def read(node: Node)(implicit readContext: ReadContext): EntityMetadata[Node] = {
      val metaMap = for(meta <- node \ "Metadata") yield{
        val key = (meta \ "MetaId").text.trim
        val serializer = XmlMetadataSerializer.getSerializationFormat[CT](key).getOrElse(throw new IllegalArgumentException("Unknown metadata category: " + key))
        key -> LazyMetadataXml((meta \ "MetaValue").head, serializer)(serializer.valueType.asInstanceOf[Class[CT]])
      }
      EntityMetadataXml(metaMap.toMap)
    }

    override def write(em: EntityMetadata[Node])(implicit writeContext: WriteContext[Node]): Node =
      <EntityMetadata>{
        for (ent <- em.toSeq) yield {
          <Metadata>
            <MetaId>{ent._1}</MetaId>
            <MetaValue>{ent._2.serialized}</MetaValue>
          </Metadata>
        }
        }
      </EntityMetadata>

    /**
      * The identifier used to define metadata objects in the map of [[EntityMetadata]]
      */
    override def metadataId: String = METADATA_KEY

    override def replaceableMetadata: Boolean = true    //has no importance for EntityMetadata
  }
}
