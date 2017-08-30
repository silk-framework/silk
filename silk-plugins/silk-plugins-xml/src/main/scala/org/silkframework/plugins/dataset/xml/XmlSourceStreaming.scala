package org.silkframework.plugins.dataset.xml

import java.io.File
import java.net.URLEncoder
import java.util.UUID
import javax.xml.stream.{XMLInputFactory, XMLStreamReader}

import org.silkframework.config.Prefixes
import org.silkframework.dataset.DataSource
import org.silkframework.entity.{Entity, EntitySchema, ForwardOperator, Path}
import org.silkframework.runtime.resource.{FileResource, Resource}
import org.silkframework.util.Uri

import scala.xml._

/**
  * XML streaming source.
  *
  * Possible improvements:
  *   - Respect provided limits.
  *   - For retrieving paths and types, stop parsing after a configured number of tags has been reached.
  *   - Don't call collectPaths recursively for ignored tags.
  */
class XmlSourceStreaming(file: Resource, uriPattern: String) extends DataSource {

  private val xmlFactory = XMLInputFactory.newInstance()

  private val uriRegex = "\\{([^\\}]+)\\}".r

  override def retrieveTypes(limit: Option[Int]): Traversable[(String, Double)] = {
    val inputStream = file.load
    try {
      val reader = xmlFactory.createXMLStreamReader(inputStream)
      reader.nextTag()
      val paths = Path.empty +: collectPaths(reader, Path.empty, onlyLeafNodes = false)
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
      goToPath(reader, Path.parse(t.uri))
      collectPaths(reader, Path.empty, onlyLeafNodes = true).toIndexedSeq

    } finally {
      inputStream.close()
    }
  }

  /**
    * Retrieves entities from this source which satisfy a specific entity schema.
    *
    * @param entitySchema The entity schema
    * @param limit        Limits the maximum number of retrieved entities
    * @return A Traversable over the entities. The evaluation of the Traversable may be non-strict.
    */
  override def retrieve(entitySchema: EntitySchema, limit: Option[Int]): Traversable[Entity] = {
    new Traversable[Entity] {
      override def foreach[U](f: (Entity) => U): Unit = {
        val inputStream = file.load
        try {
          val reader = xmlFactory.createXMLStreamReader(inputStream)
          reader.nextTag()
          goToPath(reader, Path.parse(entitySchema.typeUri.uri))
          do {
            val node = buildNode(reader)
            val traverser = XmlTraverser(node)

            val uri =
              if (uriPattern.isEmpty) {
                "urn:instance:" + entitySchema.typeUri + "/" + traverser.node.label + reader.getLocation.getCharacterOffset
              } else {
                uriRegex.replaceAllIn(uriPattern, m => {
                  val pattern = m.group(1)
                  val value = traverser.evaluatePathAsString(Path.parse(pattern)).mkString("")
                  URLEncoder.encode(value, "UTF8")
                })
              }

            val values = for (typedPath <- entitySchema.typedPaths) yield traverser.evaluatePathAsString(typedPath.path)

            f(new Entity(uri, values, entitySchema))

            goToNextEntity(reader, node.label)
          } while (reader.isStartElement)
        } finally {
          inputStream.close()
        }
      }
    }
  }

  /**
    * Retrieves a list of entities from this source.
    *
    * @param entitySchema The entity schema
    * @param entities     The URIs of the entities to be retrieved.
    * @return A Traversable over the entities. The evaluation of the Traversable may be non-strict.
    */
  override def retrieveByUri(entitySchema: EntitySchema, entities: Seq[Uri]): Seq[Entity] = {
    val uriSet = entities.map(_.uri).toSet
    retrieve(entitySchema).filter(entity => uriSet.contains(entity.uri)).toSeq
  }

  /**
    * Moves the parser to a given path.
    * On return, the parser will be positioned on the first start element with the given path.
    */
  private def goToPath(reader: XMLStreamReader, path: Path): Unit = {
    assert(path.operators.forall(_.isInstanceOf[ForwardOperator]), "Only forward operators are supported.")

    var remainingOperators = path.operators
    while(reader.hasNext && remainingOperators.nonEmpty) {
      reader.next()
      val tagName = remainingOperators.head.asInstanceOf[ForwardOperator].property.uri
      if(reader.isStartElement && reader.getLocalName == tagName) {
        remainingOperators = remainingOperators.drop(1)
      }
    }

    if(remainingOperators.nonEmpty) {
      throw new Exception(s"No elements at path $path.")
    }
  }

  /**
    * Moves the parser to the next element with the provided name on the same hierarchy level.
    * @return True, if another element was found. The parser will be positioned on the start element.
    *         False, if the end of the file has been reached.
    */
  private def goToNextEntity(reader: XMLStreamReader, name: String): Boolean = {
    var backwardPath = Seq[String]()

    while(reader.hasNext) {
      reader.next()
      if(reader.isStartElement && backwardPath.isEmpty && reader.getLocalName == name) {
        return true
      } else if(reader.isStartElement && backwardPath.nonEmpty && backwardPath.head == reader.getLocalName) {
        backwardPath = backwardPath.drop(1)
      } else if(reader.isStartElement) {
        skipElement(reader)
      } else if(reader.isEndElement) {
        backwardPath = reader.getLocalName +: backwardPath
      }
    }

    false
  }

  /**
    * Collects all paths inside the current element.
    * The parser must be positioned on the start element when calling this method.
    */
  private def collectPaths(reader: XMLStreamReader, path: Path, onlyLeafNodes: Boolean): Seq[Path] = {
    assert(reader.isStartElement)
    nextTag(reader)

    var paths = Seq[Path]()
    var startElements = Set[String]()

    while(reader.isStartElement) {
      if(!startElements.contains(reader.getLocalName)) {
        // Get paths from tag, attributes and children
        val tagPath = path ++ Path(reader.getLocalName)
        val attributePaths =
          for (attributeIndex <- 0 until reader.getAttributeCount) yield {
            tagPath ++ Path("@" + reader.getAttributeLocalName(attributeIndex))
          }
        val childPaths = collectPaths(reader, tagPath, onlyLeafNodes)

        // Collect all wanted paths
        var newPaths = attributePaths ++ childPaths
        if (!(onlyLeafNodes && childPaths.nonEmpty)) {
          newPaths = tagPath +: newPaths
        }

        // Append new paths
        paths ++= newPaths
        startElements += reader.getLocalName
      } else {
        // We already collected paths for this tag
        skipElement(reader)
      }

      nextTag(reader)
    }

    paths
  }

  private def buildNode(reader: XMLStreamReader): Elem = {
    assert(reader.isStartElement)

    val label = reader.getLocalName

    var attributes: MetaData = Null
    for(i <- 0 until reader.getAttributeCount) {
      attributes = new UnprefixedAttribute(reader.getAttributeLocalName(i), reader.getAttributeValue(i), attributes)
    }

    var children = List[Node]()
    do {
      reader.next()

      if(reader.isStartElement) {
        children ::= buildNode(reader)
      } else if(reader.isCharacters) {
        children ::= Text(reader.getText)
      }

    } while(!reader.isEndElement)

    reader.next()

    Elem(null, label, attributes, NamespaceBinding(null, null, null), minimizeEmpty = false, children.reverse: _*)
  }

  private def skipElement(reader: XMLStreamReader): Unit = {
    assert(reader.isStartElement)

    do {
      reader.next()
      if(reader.isStartElement) {
        skipElement(reader)
      }
    } while(!reader.isEndElement)

    reader.next()
  }

  private def nextTag(reader: XMLStreamReader): Unit = {
    do {
      reader.next()
    } while(!reader.isStartElement && !reader.isEndElement)
  }

}
