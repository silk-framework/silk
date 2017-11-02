package org.silkframework.plugins.dataset.xml

import java.net.URLEncoder
import javax.xml.stream.{XMLInputFactory, XMLStreamReader}

import org.silkframework.config.Prefixes
import org.silkframework.dataset.{DataSource, PathCoverageDataSource, PeakDataSource, ValueCoverageDataSource}
import org.silkframework.entity._
import org.silkframework.runtime.resource.Resource
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.Uri

import scala.xml._

/**
  * XML streaming source.
  */
class XmlSourceStreaming(file: Resource, basePath: String, uriPattern: String) extends DataSource
  with PeakDataSource with PathCoverageDataSource with ValueCoverageDataSource {

  private val xmlFactory = XMLInputFactory.newInstance()

  /**
    * Retrieves known types in this source.
    * Each path from the root corresponse to one type.
    *
    * @param limit Restricts the number of types to be retrieved. If not given, all found types are returned.
    */
  override def retrieveTypes(limit: Option[Int]): Traversable[(String, Double)] = {
    val inputStream = file.inputStream
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

  /**
    * Retrieves the most frequent paths in this source.
    *
    * @param typeUri The entity type, which provides the base path from which paths shall be collected.
    * @param depth Only retrieve paths up to a certain length. Currently ignored on this dataset.
    * @param limit Restricts the number of paths to be retrieved. If not given, all found paths are returned.
    */
  override def retrievePaths(typeUri: Uri, depth: Int, limit: Option[Int]): IndexedSeq[Path] = {
    val inputStream = file.inputStream
    try {
      val reader = xmlFactory.createXMLStreamReader(inputStream)
      goToPath(reader, Path.parse(typeUri.uri))
      collectPaths(reader, Path.empty, onlyLeafNodes = false).toIndexedSeq

    } finally {
      inputStream.close()
    }
  }

  /**
    * Retrieves entities from this source which satisfy a specific entity schema.
    *
    * @param entitySchema The entity schema
    * @param limit        Limits the maximum number of retrieved entities
    * @return A Traversable over the entities. The evaluation of the Traversable is non-strict.
    */
  override def retrieve(entitySchema: EntitySchema, limit: Option[Int]): Traversable[Entity] = {
    if(entitySchema.typedPaths.exists(_.path.operators.exists(_.isInstanceOf[BackwardOperator]))) {
      throw new ValidationException("Backward paths are not supported when streaming XML. Disable streaming to use backward paths.")
    }

    new Traversable[Entity] {
      override def foreach[U](f: (Entity) => U): Unit = {
        val inputStream = file.inputStream
        try {
          val reader = xmlFactory.createXMLStreamReader(inputStream)
          goToPath(reader, Path.parse(entitySchema.typeUri.uri) ++ entitySchema.subPath)
          var count = 0
          do {
            val node = buildNode(reader)
            val traverser = XmlTraverser(node)

            val uri = traverser.generateUri(uriPattern)
            val values = for (typedPath <- entitySchema.typedPaths) yield traverser.evaluatePathAsString(typedPath.path, uriPattern)

            f(new Entity(uri, values, entitySchema))

            goToNextEntity(reader, node.label)
            count += 1

          } while (reader.isStartElement && limit.forall(count < _))
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
    * If the path is empty, the base path will be used.
    */
  private def goToPath(reader: XMLStreamReader, rootPath: Path): Unit = {
    val path = if(rootPath.isEmpty) Path.parse(basePath) else rootPath
    assert(path.operators.forall(_.isInstanceOf[ForwardOperator]), "Only forward operators are supported.")

    var remainingOperators = path.operators
    reader.nextTag()
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

    // Collect attribute paths
    val attributePaths =
      for (attributeIndex <- 0 until reader.getAttributeCount) yield {
        path ++ Path("@" + reader.getAttributeLocalName(attributeIndex))
      }

    // Move to first child
    nextStartOrEndTag(reader)

    // Iterate all child elements
    var paths = Seq[Path]()
    var startElements = Set[String]()
    while(reader.isStartElement) {
      if(!startElements.contains(reader.getLocalName)) {
        // Get paths from tags and children
        val tagPath = path ++ Path(reader.getLocalName)
        val childPaths = collectPaths(reader, tagPath, onlyLeafNodes)

        // Collect all wanted paths
        var newPaths = childPaths
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

      nextStartOrEndTag(reader)
    }

    attributePaths ++ paths
  }

  /**
    * Builds a XML node for a given start element that includes all its children.
    * The parser must be positioned on the start element when calling this method.
    */
  private def buildNode(reader: XMLStreamReader): Elem = {
    assert(reader.isStartElement)

    // Remember label
    val label = reader.getLocalName

    // Collect attributes on this element
    var attributes: MetaData = Null
    for(i <- 0 until reader.getAttributeCount) {
      attributes = new UnprefixedAttribute(reader.getAttributeLocalName(i), reader.getAttributeValue(i), attributes)
    }

    // Collect child nodes
    var children = List[Node]()
    do {
      reader.next()
      if(reader.isStartElement) {
        children ::= buildNode(reader)
      } else if(reader.isCharacters) {
        children ::= Text(reader.getText)
      }
    } while(!reader.isEndElement)

    // Move to the element after the end element.
    reader.next()

    Elem(null, label, attributes, NamespaceBinding(null, null, null), minimizeEmpty = false, children.reverse: _*)
  }

  /**
    * Skips an element.
    * The parser must be positioned on the start element when calling this method.
    */
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

  /**
    * Positions the parser to next start or end element.
    */
  private def nextStartOrEndTag(reader: XMLStreamReader): Unit = {
    do {
      reader.next()
    } while(!reader.isStartElement && !reader.isEndElement)
  }

  override def combinedPath(typeUri: String, inputPath: Path): Path = {
    val typePath = Path.parse(typeUri)
    Path(typePath.operators ++ inputPath.operators)
  }

  override def convertToIdPath(path: Path): Option[Path] = {
    Some(Path(path.operators ::: List(ForwardOperator("#id"))))
  }

}
