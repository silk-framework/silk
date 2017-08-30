package org.silkframework.plugins.dataset.xml

import java.io.File
import javax.xml.stream.{XMLInputFactory, XMLStreamReader}

import org.silkframework.config.Prefixes
import org.silkframework.dataset.DataSource
import org.silkframework.entity.{EntitySchema, Path}
import org.silkframework.runtime.resource.{FileResource, Resource}
import org.silkframework.util.Uri

/**
  * XML streaming source.
  *
  * Possible improvements:
  *   - Respect provided limits.
  *   - For retrieving paths and types, stop parsing after a configured number of tags has been reached.
  *   - Don't call collectPaths recursively for ignored tags.
  */
object XmlSourceStreaming extends App {

  val resource = FileResource(new File("C:\\Users\\risele\\repositories\\data-integration\\conf\\logback.xml"))

  val source = new XmlSourceStreaming(resource)

  println(source.retrieveTypes())

}

class XmlSourceStreaming(file: Resource) extends DataSource {

  private val xmlFactory = XMLInputFactory.newInstance()

  override def retrieveTypes(limit: Option[Int]): Traversable[(String, Double)] = {
    val inputStream = file.load
    try {
      val reader = xmlFactory.createXMLStreamReader(inputStream)
      reader.nextTag()
      val paths = Path.empty +: collectPaths(reader, Path.empty)
      for (path <- paths) yield {
        (path.serialize(Prefixes.empty), 1.0 / (path.operators.size + 1))
      }
    } finally {
      inputStream.close()
    }
  }

  override def retrievePaths(t: Uri, depth: Int, limit: Option[Int]): IndexedSeq[Path] = {
    val inputStream = file.load
    try {
      val reader = xmlFactory.createXMLStreamReader(inputStream)
      reader.nextTag()
      collectPaths(reader, Path.empty).toIndexedSeq

    } finally {
      inputStream.close()
    }
  }

  private def goToPath(reader: XMLStreamReader, path: Path): Unit = {

  }

  private def collectPaths(reader: XMLStreamReader, path: Path): Seq[Path] = {
    assert(reader.isStartElement)
    nextTag(reader)

    var paths = Seq[Path]()
    var startElements = Set[String]()

    while(reader.isStartElement) {
      val tagPath = path ++ Path(reader.getLocalName)
      val attributePaths =
        for(attributeIndex <- 0 until reader.getAttributeCount) yield {
          tagPath ++ Path("@" + reader.getAttributeLocalName(attributeIndex))
        }
      val newPaths = (tagPath +: attributePaths) ++ collectPaths(reader, tagPath)

      if(!startElements.contains(reader.getLocalName)) {
        paths ++= newPaths
        startElements += reader.getLocalName
      }

      nextTag(reader)
    }

    paths
  }

  private def nextTag(reader: XMLStreamReader): Unit = {
    reader.next()
    while(!reader.isStartElement && !reader.isEndElement) {
      reader.next()
    }
  }


  /**
    * Retrieves entities from this source which satisfy a specific entity schema.
    *
    * @param entitySchema The entity schema
    * @param limit        Limits the maximum number of retrieved entities
    * @return A Traversable over the entities. The evaluation of the Traversable may be non-strict.
    */
  override def retrieve(entitySchema: EntitySchema, limit: Option[Int]) = ???

  /**
    * Retrieves a list of entities from this source.
    *
    * @param entitySchema The entity schema
    * @param entities     The URIs of the entities to be retrieved.
    * @return A Traversable over the entities. The evaluation of the Traversable may be non-strict.
    */
  override def retrieveByUri(entitySchema: EntitySchema, entities: Seq[Uri]) = ???
}
