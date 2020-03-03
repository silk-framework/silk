package org.silkframework.plugins.dataset.xml

import java.io.InputStream
import java.util.concurrent.atomic.AtomicInteger

import javax.xml.stream.{XMLInputFactory, XMLStreamConstants, XMLStreamReader}
import org.silkframework.config.{PlainTask, Task}
import org.silkframework.dataset._
import org.silkframework.entity._
import org.silkframework.entity.paths.{BackwardOperator, ForwardOperator, TypedPath, UntypedPath}
import org.silkframework.execution.EntityHolder
import org.silkframework.execution.local.GenericEntityTable
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.Resource
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.{Identifier, Uri}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.Try
import scala.xml._

/**
  * XML streaming source.
  */
class XmlSourceStreaming(file: Resource, basePath: String, uriPattern: String) extends DataSource
  with PeakDataSource with PathCoverageDataSource with ValueCoverageDataSource with XmlSourceTrait with HierarchicalSampleValueAnalyzerExtractionSource {

  private val xmlFactory = XMLInputFactory.newInstance()
  final val schemaElementLimit = 100 * 1000

  /**
    * Retrieves known types in this source.
    * Each path from the root corresponds to one type.
    *
    * @param limit Restricts the number of types to be retrieved. If not given, all found types are returned.
    */
  override def retrieveTypes(limit: Option[Int])
                            (implicit userContext: UserContext): Traversable[(String, Double)] = {
    if(file.nonEmpty) {
      val schema = extractSchema(PathCategorizerValueAnalyzerFactory(), pathLimit = schemaElementLimit, sampleLimit = Some(1))
      for(schemaClass <- schema.classes) yield {
        val operators = UntypedPath.parse(schemaClass.sourceType)
        (schemaClass.sourceType, pathRank(operators.size))
      }
    } else {
      Traversable.empty
    }
  }

  // Create and init stream reader, positions the stream reader on the first tag
  private def initStreamReader(inputStream: InputStream) = {
    val reader = xmlFactory.createXMLStreamReader(inputStream)
    var foundStartElement = false
    while(reader.hasNext && !foundStartElement) {
      reader.next()
      foundStartElement = reader.isStartElement
    }
    reader
  }

  private val basePathParts: List[String] = {
    val pureBasePath = basePath.stripPrefix("/").trim
    if (pureBasePath == "") {
      List.empty[String]
    } else {
      pureBasePath.split('/').toList
    }
  }

  private val basePathPartsReversed = basePathParts.reverse

  private val basePathLength = basePathParts.length

  /**
    * Retrieves the most frequent paths in this source.
    *
    * @param typeUri The entity type, which provides the base path from which paths shall be collected.
    * @param depth Only retrieve paths up to a certain length.
    * @param limit Restricts the number of paths to be retrieved. If not given, all found paths are returned.
    */
  override def retrievePaths(typeUri: Uri, depth: Int = Int.MaxValue, limit: Option[Int] = None)
                            (implicit userContext: UserContext): IndexedSeq[TypedPath] = {
    val schema = extractSchema(PathCategorizerValueAnalyzerFactory(), pathLimit = schemaElementLimit, sampleLimit = Some(1))
    val pathBuffer = mutable.ArrayBuffer[TypedPath]()
    val normalizedTypeUri = typeUri.toString.dropWhile(_ == '/')
    for(schemaClass <- schema.classes if schemaClass.sourceType.startsWith(normalizedTypeUri)) {
      val relativeClass = schemaClass.sourceType.drop(normalizedTypeUri.length).dropWhile(_ == '/')
      val classPath = UntypedPath.parse(relativeClass)
      if(classPath.size > 0 && classPath.size <= depth) {
        pathBuffer.append(TypedPath(classPath, ValueType.URI, isAttribute = false))
      }
      for(schemaPath <- schemaClass.properties) {
        val typedPath = TypedPath(UntypedPath.parse(relativeClass + "/" + schemaPath.path.normalizedSerialization), ValueType.STRING,
          isAttribute = schemaPath.path.normalizedSerialization.startsWith("@"))
        if(typedPath.size <= depth) {
          pathBuffer.append(typedPath)
        }
      }
    }
    pathBuffer.toIndexedSeq
  }

  private def pathRank(pathLength: Int): Double = 1.0 / (pathLength + 1)

  override def retrieveXmlPaths(typeUri: Uri, depth: Int, limit: Option[Int], onlyLeafNodes: Boolean, onlyInnerNodes: Boolean): IndexedSeq[TypedPath] = {
    val inputStream = file.inputStream
    try {
      val reader: XMLStreamReader = initStreamReader(inputStream)
      goToPath(reader, UntypedPath.parse(typeUri.uri))
      val paths = collectPaths(reader, UntypedPath.empty, onlyLeafNodes = onlyLeafNodes, onlyInnerNodes = onlyInnerNodes, depth).toIndexedSeq
      limit match {
        case Some(value) => paths.take(value)
        case None => paths
      }
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
  override def retrieve(entitySchema: EntitySchema, limit: Option[Int])
                       (implicit userContext: UserContext): EntityHolder = {
    if(entitySchema.typedPaths.exists(_.operators.exists(_.isInstanceOf[BackwardOperator]))) {
      throw new ValidationException("Backward paths are not supported when streaming XML. Disable streaming to use backward paths.")
    }

    val entities =
      new Traversable[Entity] {
        override def foreach[U](f: Entity => U): Unit = {
          val inputStream = file.inputStream
          try {
            val reader: XMLStreamReader = initStreamReader(inputStream)
            goToPath(reader, UntypedPath.parse(entitySchema.typeUri.uri) ++ entitySchema.subPath)
            var count = 0
            do {
              val node = buildNode(reader)
              val traverser = XmlTraverser(node)

              val uri = traverser.generateUri(uriPattern)
              val values = for (typedPath <- entitySchema.typedPaths) yield traverser.evaluatePathAsString(typedPath, uriPattern)

              f(Entity(uri, values, entitySchema))

              goToNextEntity(reader, node.label)
              count += 1

            } while (reader.isStartElement && limit.forall(count < _))
          } finally {
            inputStream.close()
          }
        }
      }

    GenericEntityTable(entities, entitySchema, underlyingTask)
  }

  /**
    * Moves the parser to a given path.
    * On return, the parser will be positioned on the first start element with the given path.
    * If the path is empty, the base path will be used.
    */
  private def goToPath(reader: XMLStreamReader, rootPath: UntypedPath): Unit = {
    val path = if(rootPath.isEmpty) UntypedPath.parse(basePath) else rootPath
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
      if(reader.isStartElement && backwardPath.isEmpty && reader.getLocalName == name) {
        return true
      } else if(reader.isStartElement && backwardPath.nonEmpty && backwardPath.head == reader.getLocalName) {
        backwardPath = backwardPath.drop(1)
        reader.next()
      } else if(reader.isStartElement) {
        skipElement(reader)
      } else if(reader.isEndElement) {
        backwardPath = reader.getLocalName +: backwardPath
        reader.next()
      } else {
        reader.next()
      }
    }

    false
  }

  /**
    * Collects all paths inside the current element.
    * The parser must be positioned on the start element when calling this method.
    * On return, the parser will be positioned on the element that directly follows the element.
    */
  private def collectPaths(reader: XMLStreamReader, path: UntypedPath, onlyLeafNodes: Boolean, onlyInnerNodes: Boolean, depth: Int): Seq[TypedPath] = {
    assert(reader.isStartElement)
    assert(!(onlyInnerNodes && onlyLeafNodes), "onlyInnerNodes and onlyLeafNodes cannot be set to true at the same time")

    // Collect attribute paths
    val attributePaths: IndexedSeq[TypedPath] = fetchAttributePaths(reader, path)

    // Move to first child
    nextStartOrEndTag(reader)

    // Iterate all child elements
    var paths = Seq[TypedPath]()
    var startElements = Set[String]()
    while(!reader.isEndElement) {
      if (reader.isStartElement && !startElements.contains(reader.getLocalName)) {
        // Get paths from children
        val localName = reader.getLocalName
        val tagPath = path ++ UntypedPath(localName)
        val childPaths = collectPaths(reader, tagPath, onlyLeafNodes, onlyInnerNodes, depth - 1)

        // The depth check has to be done after collecting paths of the child, because all tags must be consumed by the reader
        val depthAdjustedChildPaths = if (depth == 0) Seq() else childPaths
        // Collect all wanted paths
        val newPaths = choosePaths(onlyLeafNodes, onlyInnerNodes, childPaths, depthAdjustedChildPaths)

        // Append new paths
        paths ++= newPaths
        startElements += localName
      } else if (reader.isStartElement) {
        // We already collected paths for this tag
        skipElement(reader)
      } else {
        reader.next()
      }
    }

    reader.next()

    val depthAdjustedAttributePaths: IndexedSeq[TypedPath] = if(depth == 0) IndexedSeq() else attributePaths
    val pathValueType = if(attributePaths.nonEmpty || startElements.nonEmpty) ValueType.URI else ValueType.STRING
    val typedPath = TypedPath(path, pathValueType, isAttribute = false)

    if(onlyInnerNodes && startElements.isEmpty && attributePaths.isEmpty) {
      Seq() // An inner node has at least an attribute or child elements
    } else if(onlyInnerNodes) {
      TypedPath(path, ValueType.URI, isAttribute = false) +: paths // The paths are already depth adjusted and only contain inner nodes
    } else if(onlyLeafNodes && startElements.isEmpty) {
      Seq(typedPath) ++ depthAdjustedAttributePaths // A leaf node is a node without children, but may have attributes
    } else if(onlyLeafNodes) {
      depthAdjustedAttributePaths ++ paths
    } else {
      typedPath +: (depthAdjustedAttributePaths ++ paths)
    }
  }

  private def choosePaths(onlyLeafNodes: Boolean, onlyInnerNodes: Boolean, childPaths: Seq[TypedPath], depthAdjustedChildPaths: Seq[TypedPath]) = {
    if (onlyInnerNodes) {
      if (childPaths.isEmpty) {
        Seq()
      } else {
        depthAdjustedChildPaths
      }
    } else if (onlyLeafNodes) {
      if (childPaths.nonEmpty) {
        depthAdjustedChildPaths
      } else {
        Seq()
      }
    } else {
      depthAdjustedChildPaths
    }
  }

  private def fetchAttributePaths(reader: XMLStreamReader, path: UntypedPath) = {
    for (attributeIndex <- 0 until reader.getAttributeCount) yield {
      TypedPath(path ++ UntypedPath("@" + reader.getAttributeLocalName(attributeIndex)), ValueType.STRING, isAttribute = true)
    }
  }

  /**
    * Builds a XML node for a given start element that includes all its children.
    * The parser must be positioned on the start element when calling this method.
    * On return, the parser will be positioned on the element that directly follows the element.
    */
  private def buildNode(reader: XMLStreamReader): InMemoryXmlElem = {
    assert(reader.isStartElement)
    val lineNumber = reader.getLocation.getLineNumber
    val columnNumber = reader.getLocation.getColumnNumber

    // Remember label
    val label = reader.getLocalName

    // Collect attributes on this element
    val attributes = for(i <- 0 until reader.getAttributeCount) yield {
      reader.getAttributeLocalName(i) -> reader.getAttributeValue(i)
    }

    // Collect child nodes
    val children = new ArrayBuffer[InMemoryXmlNode]()
    reader.next()
    while(!reader.isEndElement) {
      if(reader.isStartElement) {
        children.append(buildNode(reader))
      } else if(reader.isCharacters) {
        children.append(InMemoryXmlText(reader.getText))
        reader.next()
      } else {
        reader.next()
      }
    }

    // Move to the element after the end element.
    reader.next()

    InMemoryXmlElem(s"$lineNumber-$columnNumber", label, attributes.toMap, children.toArray)
  }

  /**
    * Skips an element.
    * The parser must be positioned on the start element when calling this method.
    * On return, the parser will be positioned on the element that directly follows the element.
    */
  private def skipElement(reader: XMLStreamReader): Unit = {
    assert(reader.isStartElement)

    // Move to first child element
    reader.next()

    // If this is an empty tag, we return immediately
    if(reader.isEndElement) {
      reader.next()
      return
    }

    // Skip contents
    do {
      if(reader.isStartElement) {
        skipElement(reader)
      } else {
        reader.next()
      }
    } while(!reader.isEndElement)

    // Move to element that follows the skipped element
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

  override def combinedPath(typeUri: String, inputPath: UntypedPath): UntypedPath = {
    val typePath = UntypedPath.parse(typeUri)
    UntypedPath(typePath.operators ++ inputPath.operators)
  }

  override def convertToIdPath(path: UntypedPath): Option[UntypedPath] = {
    Some(UntypedPath(path.operators ::: List(ForwardOperator("#id"))))
  }

  private def basePathMatches(currentPath: List[String]) = {
    basePathLength == 0 || basePathPartsReversed == currentPath.takeRight(basePathLength)
  }

  def collectPaths(limit: Int, collectValues: (List[String], String) => Unit = (_, _) => {}): Seq[List[String]] = {
    val paths = mutable.HashMap[List[String], Int]()
    paths.put(Nil, 0)
    val idx = new AtomicInteger(1)
    var currentPath = List[String]()

    val inputStream = file.inputStream
    try {
      val reader: XMLStreamReader = initStreamReader(inputStream)
      var readNext = true
      var eventId = reader.getEventType
      while(reader.hasNext && paths.size < limit) {
        if(readNext) {
          reader.next()
          eventId = reader.getEventType
        } else {
          eventId = reader.getEventType
          readNext = true
        }
        eventId match {
          case XMLStreamConstants.START_ELEMENT =>
            currentPath ::= reader.getLocalName
            if (basePathMatches(currentPath)) {
              addIfNotExists(paths, idx, currentPath)
              for (attributeIndex <- 0 until reader.getAttributeCount) yield {
                val attributePath = "@" + reader.getAttributeLocalName(attributeIndex) :: currentPath
                addIfNotExists(paths, idx, attributePath)
                collectValues(attributePath, reader.getAttributeValue(attributeIndex))
              }
            }
            val text = Try(reader.getElementText)
            text foreach { elemText =>
              collectValues(currentPath, elemText)
            }
            readNext = false // Do not read next event, already placed on the next one
          case XMLStreamConstants.END_ELEMENT =>
            if (currentPath.nonEmpty) { // needs to be done since we don't read the root element, but it appears as end element
              currentPath = currentPath.tail
            }
          case _ =>
          // Nothing to be done for other events
        }
      }
    } finally {
      inputStream.close()
    }
    // Sort paths by first occurrence
    paths.toSeq.sortBy(_._2).map(p => p._1.reverse)
  }

  private def addIfNotExists(paths: mutable.HashMap[List[String], Int],
                             idx: AtomicInteger,
                             path: List[String]) = {
    if (!paths.contains(path.dropRight(basePathLength))) {
      paths.put(if (basePathLength == 0) path else path.dropRight(basePathLength), idx.getAndIncrement())
    }
  }

  /**
    * The dataset task underlying the Datset this source belongs to
    *
    * @return
    */
  override def underlyingTask: Task[DatasetSpec[Dataset]] = PlainTask(Identifier.fromAllowed(file.name), DatasetSpec(EmptyDataset))   //FIXME CMEM-1352 replace with actual task
}
