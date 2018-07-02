package org.silkframework.entity.metadata

import org.silkframework.runtime.serialization._

import scala.xml.Node

trait EntityMetadata[Serialization] extends Map[String, LazyMetadata[_, Serialization]] with Serializable{

  implicit val serTag: Class[Serialization]

  /**
    * get metadata object
    * @param key - with this key
    * @tparam Typ - of this Type
    * @return - the metadata object or None
    */
  def getLazyMetadata[Typ](key: String)(implicit tt: Class[Typ]): LazyMetadata[Typ, Serialization] = metadata.get(key) match{
    case Some(lm: LazyMetadata[_, _]) => lm.metadata match{
      case Some(x: Any) if tt.isAssignableFrom(x.getClass) => lm.asInstanceOf[LazyMetadata[Typ, Serialization]]
      case _ => LazyMetadata.empty[Typ, Serialization]
    }
    case _ => LazyMetadata.empty[Typ, Serialization]
  }

  /**
    * @return - returning a Throwable for failed Entities
    */
  def failure: LazyMetadata[Throwable, Serialization] = getLazyMetadata[Throwable](EntityMetadata.FAILURE_KEY)(classOf[Throwable])

  val serializer: SerializationFormat[EntityMetadata[Serialization], Serialization]

  val metadata: Map[String, LazyMetadata[_, Serialization]]

  override def +[B1 >: LazyMetadata[_, Serialization]](kv: (String, B1)): Map[String, B1] = metadata.+[B1](kv)

  override def get(key: String): Option[LazyMetadata[_, Serialization]] = metadata.get(key)

  override def iterator: Iterator[(String, LazyMetadata[_, Serialization])] = metadata.iterator

  override def -(key: String): Map[String, LazyMetadata[_, Serialization]] = metadata.-(key)
}

object EntityMetadata{
  val FAILURE_KEY: String = "failure_metadata"
  val METADATA_KEY : String = "entity_metadata"

  type CT >: Any <: Any

  def apply[Typ](map: Map[String, Typ])(implicit typTag: Class[Typ]): EntityMetadata[Node] = {
    val resMap = map.map(ent => {
      val serializer = XmlMetadataSerializer.getSerializationFormat[Typ](ent._1).getOrElse(throw new IllegalArgumentException("Unknown metadata category: " + ent._1))
      ent._1 -> LazyMetadataXml(ent._2, serializer)(typTag)
    })
    new EntityMetadataXml(resMap)
  }

  def apply(t: Throwable): EntityMetadata[Node] = apply(Map(FAILURE_KEY -> t))(classOf[Throwable])

  def apply(value: String): EntityMetadata[Node] = XmlSerializer.fromString(value, XmlFormat.MIME_TYPE_TEXT)(ReadContext())

  val empty: EntityMetadata[String] = new EntityMetadata[String] {
    override val serializer: SerializationFormat[EntityMetadata[String], String] = LazyMetadata.nullSerializer
    override val metadata: Map[String, LazyMetadata[_, String]] = Map()
    override implicit val serTag: Class[String] = classOf[String]
  }

  object XmlSerializer extends XmlMetadataSerializer[EntityMetadata[Node]]{
    override def read(node: Node)(implicit readContext: ReadContext): EntityMetadata[Node] = {
      val metaMap = for(meta <- node \ "Metadata") yield{
        val key = (meta \ "MetaId").text.trim
        val serializer = XmlMetadataSerializer.getSerializationFormat[CT](key).getOrElse(throw new IllegalArgumentException("Unknown metadata category: " + key))
        key -> LazyMetadataXml((meta \ "MetaValue").head, serializer)(serializer.valueType.asInstanceOf[Class[CT]])
      }
      new EntityMetadataXml(metaMap.toMap)
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
    override val metadataId: String = METADATA_KEY
  }
}

class EntityMetadataXml(override val metadata: Map[String, LazyMetadata[_, Node]] = Map[String, LazyMetadata[_, Node]]()) extends EntityMetadata[Node] {

  def this(rawMetadata: String) = {
    this(EntityMetadata.XmlSerializer.fromString(rawMetadata, XmlFormat.MIME_TYPE_TEXT)(ReadContext()))
  }

  override val serializer: SerializationFormat[EntityMetadata[Node], Node] = EntityMetadata.XmlSerializer

  override implicit val serTag: Class[Node] = classOf[Node]
}