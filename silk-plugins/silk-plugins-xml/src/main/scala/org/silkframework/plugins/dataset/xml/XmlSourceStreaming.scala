package org.silkframework.plugins.dataset.xml

import org.silkframework.config.{PlainTask, Prefixes, Task}
import org.silkframework.dataset._
import org.silkframework.entity._
import org.silkframework.entity.paths._
import org.silkframework.execution.EntityHolder
import org.silkframework.execution.local.GenericEntityTable
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.iterator.BufferingIterator
import org.silkframework.runtime.resource.{Resource, ResourceTooLargeException}
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.{Identifier, Uri}

import java.io.{ByteArrayInputStream, InputStream}
import java.util.concurrent.atomic.AtomicInteger
import javax.xml.stream.{XMLInputFactory, XMLStreamConstants, XMLStreamException, XMLStreamReader}
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.Try

/**
  * XML streaming source.
  */
class XmlSourceStreaming(file: Resource, basePath: String, uriPattern: String) extends DataSource
  with PeakDataSource with PathCoverageDataSource with ValueCoverageDataSource with XmlSourceTrait with HierarchicalSampleValueAnalyzerExtractionSource {
  // We can only get the character offset not the byte offset, so this is an approximiation
  private val maxEntitySizeInBytes = Resource.maxInMemorySize
  private val xmlFactory = XMLInputFactory.newInstance()

  private case class XMLReadException(msg: String, cause: Throwable) extends RuntimeException(msg, cause)

  // Create and init stream reader, positions the stream reader on the first tag
  private def initStreamReader(inputStream: InputStream) = {
    try {
      // Don't resolve external references such as DTDs
      xmlFactory.setXMLResolver((publicID: String, systemID: String, baseURI: String, namespace: String) => {
        new ByteArrayInputStream(Array[Byte]())
      })
      val reader = xmlFactory.createXMLStreamReader(inputStream)
      var foundStartElement = false
      while (reader.hasNext && !foundStartElement) {
        reader.next()
        foundStartElement = reader.isStartElement
      }
      reader
    } catch {
      case ex: XMLStreamException =>
        throw XMLReadException(s"Error occurred reading XML file '${file.name}': " + ex.getMessage, ex)
    }
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

  override def retrieveXmlPaths(typeUri: Uri, depth: Int, limit: Option[Int], onlyLeafNodes: Boolean, onlyInnerNodes: Boolean): IndexedSeq[TypedPath] = {
    val inputStream = file.inputStream
    try {
      val reader: XMLStreamReader = initStreamReader(inputStream)
      goToFirstEntity(reader, UntypedPath.parse(typeUri.uri))
      val paths = collectPaths(reader, UntypedPath.empty, onlyLeafNodes = onlyLeafNodes, onlyInnerNodes = onlyInnerNodes, depth).toIndexedSeq
      limit match {
        case Some(value) => paths.take(value)
        case None => paths
      }
    } catch {
      case _: PathNotFoundException =>
        IndexedSeq.empty
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
                       (implicit userContext: UserContext, prefixes: Prefixes): EntityHolder = {
    if(entitySchema.typedPaths.exists(_.operators.exists(_.isInstanceOf[BackwardOperator]))) {
      throw new ValidationException("Backward paths are not supported when streaming XML. Disable streaming to use backward paths.")
    }

    val entities = new Entities(entitySchema, limit)
    GenericEntityTable(entities, entitySchema, underlyingTask)
  }

  private class Entities(entitySchema: EntitySchema, limit: Option[Int] = None) extends BufferingIterator[Entity] {

    // The next entity
    private var nextEntity: Option[Entity] = None

    // True, if there are unread entities
    private var hasMoreEntities: Boolean = true

    private var count = 0

    private val inputStream = file.inputStream

    private val reader: XMLStreamReader = initStreamReader(inputStream)

    private val entityPath = {
      val typeUri = entitySchema.typeUri.uri
      val typeUriPart = if (typeUri.isEmpty) {
        ""
      } else if (typeUri.startsWith("\\") || typeUri.startsWith("/") || typeUri.startsWith("[")) {
        typeUri
      } else {
        "/" + typeUri
      }
      val pathStr = basePath + typeUriPart

      UntypedPath.parse(pathStr) ++ entitySchema.subPath
    }

    private val entityPathSegments = PathSegments(entityPath)

    /**
      * Reads to the next entity.
      * Sets `nextEntity` and `hasMoreEntities`.
      */
    override def retrieveNext(): Option[Entity] = {
      try {
        if (count == 0) {
          // Read until first entity
          hasMoreEntities = goToFirstEntity(reader, entityPath)
        }

        nextEntity = None
        while (nextEntity.isEmpty && hasMoreEntities && limit.forall(count < _)) {
          val node = if (entityPathSegments.endsWithAttribute) {
            buildAttributeNode(reader, entityPathSegments.pathSegment(entityPathSegments.nrPathSegments - 1).forwardOp.get.property.uri)
          } else {
            NodeBuilder(reader).buildNode()
          }
          val traverser = XmlTraverser(node)

          val uri = traverser.generateUri(uriPattern)
          val values = for (typedPath <- entitySchema.typedPaths) yield traverser.evaluatePathAsString(typedPath, uriPattern)

          nextEntity = Some(Entity(uri, values, entitySchema))

          hasMoreEntities = goToNextEntity(reader, entityPathSegments, entityPathSegments.nrPathSegments - 1)
          count += 1
        }
        nextEntity
      } catch {
        case _: PathNotFoundException =>
          // do nothing, no entity will be output
          hasMoreEntities = false
          None
      }
    }

    override def close(): Unit = {
      try {
        reader.close()
      } finally {
        inputStream.close()
      }
    }

  }

    def buildNode(): InMemoryXmlElem = {
    file.read { inputStream => {
      val reader = initStreamReader(inputStream)
      NodeBuilder(reader).buildNode()
    }}
  }

  private case class NodeBuilder(reader: XMLStreamReader) {

    private val startCharOffset = reader.getLocation.getCharacterOffset

    private val maxSize = Resource.maxInMemorySize()

    /**
      * Builds a XML node for a given start element that includes all its children.
      * The parser must be positioned on the start element when calling this method.
      * On return, the parser will be positioned on the element that directly follows the element.
      */
    def buildNode(): InMemoryXmlElem = {
      assert(reader.isStartElement)
      val currentSize = reader.getLocation.getCharacterOffset - startCharOffset
      if(currentSize > maxSize) {
        throw new ResourceTooLargeException("Tried to load an entity into memory that is larger than the configured maximum " +
          s"(size: $currentSize, maximum size: $maxSize}). " +
          s"Configure '${Resource.maxInMemorySizeParameterName}' in order to increase this limit.")
      }
      val position = getPosition(reader)

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
          children.append(buildNode())
        } else if(reader.isCharacters) {
          children.append(InMemoryXmlText(reader.getText, getPosition(reader)))
          reader.next()
        } else {
          reader.next()
        }
      }

      // Move to the element after the end element.
      reader.next()

      InMemoryXmlElem(label, attributes.toMap, children.toArray, position)
    }
  }

  /**
    * Moves the parser to the first match of the given path.
    * When returning true, the parser will be positioned on the first start element with the given path.
    * When returning false, no entity with that path has been found.
    * If the path is empty, the base path will be used.
    *
    */
  private def goToFirstEntity(reader: XMLStreamReader, rootPath: UntypedPath): Boolean = {
    val path = if(rootPath.isEmpty) UntypedPath.parse(basePath) else rootPath
    checkObjectPath(path)

    val pathSegments = PathSegments(path)
    goToNextEntity(reader, pathSegments, initialPathSegmentIdx = 0)
  }

  /**
    * Moves the parser to the next element with the provided name on the same hierarchy level.
    *
    * @param entityPathSegments    The entity path leading to the entity.
    * @param initialPathSegmentIdx The path segment the XML stream reader is currently positioned at.
    * @return True, if another element was found. The parser will be positioned on the start element.
    *         False, if the end of the file has been reached.
    */
  private def goToNextEntity(reader: XMLStreamReader,
                             entityPathSegments: PathSegments,
                             initialPathSegmentIdx: Int): Boolean = {
    var pathSegmentIdx = initialPathSegmentIdx
    def currentPathSegment(): entityPathSegments.PathSegment = entityPathSegments.pathSegment(pathSegmentIdx)
    while(reader.hasNext) {
      if(reader.isStartElement) {
        if(currentPathSegment().matches(reader)) {
          if(pathSegmentIdx == entityPathSegments.nrPathSegments - 1) {
            // All path segments were matching, found element.
            return true
          } else {
            if(pathSegmentIdx != entityPathSegments.nrPathSegments - 2 || !entityPathSegments.pathSegment(entityPathSegments.nrPathSegments - 1).isAttribute) {
              // If the next path segment is the last and an attribute, do not forward the reader.
              reader.next()
            }
            // Last path segment was matching, check next one
            pathSegmentIdx += 1
          }
        } else {
          // Path element was not matching, check next sibling
          skipElement(reader)
          // skipElement already calls reader.next() at the end, so this is not needed here.
        }
      } else if(reader.isEndElement) {
        pathSegmentIdx -= 1
        reader.next()
      } else {
        reader.next()
      }
    }
    // Document end, no further entity can be found.
    false
  }

  /** Representation of a path that groups together forward op with related property filters. */
  case class PathSegments(entityPath: UntypedPath) {
    checkObjectPath(entityPath)

    /** The number of path segments. */
    // The root element counts a +1.
    val nrPathSegments: Int = entityPath.operators.count(_.isInstanceOf[ForwardOperator]) + 1

    /** Each path segment consists of a forward path followed by arbitrarily many property filters,
      * except for the first (root) segment, which can only have property filters. */
    private val pathSegments: Array[PathSegment] = {
      val arr = new Array[PathSegment](nrPathSegments)
      var counter = 0
      // Init root segment
      arr(0) = PathSegment(None)
      for(op <- entityPath.operators) {
        op match {
          case fp: ForwardOperator =>
            counter += 1
            arr(counter) = PathSegment(Some(fp))
          case pf: PropertyFilter =>
            arr(counter) = arr(counter).copy(pathFilters = arr(counter).pathFilters :+ pf)
          case _ =>
            throw new IllegalArgumentException("Unsupported operator: " + op)
        }
      }
      arr
    }

    def pathSegment(idx: Int): PathSegment = {
      pathSegments(idx)
    }

    def endsWithAttribute: Boolean = pathSegments.last.isAttribute

    /**
      * A path segment groups a forward operator with its related property filters.
      *
      * @param forwardOp   An optional forward operator. This is only None for the root segment.
      * @param pathFilters Path filters that are applied after the corresponding forward operator.
      */
    case class PathSegment(forwardOp: Option[ForwardOperator], pathFilters: Seq[PropertyFilter] = Seq.empty) {
      /** Checks if the path segment matches the current element of the XML reader. */
      def matches(reader: XMLStreamReader): Boolean = {
        assert(reader.isStartElement)
        val opMatch = forwardOp map { op =>
          if(isAttribute) {
            attributeValue(reader, op.property.uri.stripPrefix("@")).isDefined
          } else {
            op.property.uri == reader.getLocalName
          }
        }
        val filterMatch = pathFilters.forall(pf => propertyFilterApplies(reader, pf))
        opMatch.getOrElse(true) && filterMatch
      }

      /** Returns true if the forward operator contains an attribute property. */
      def isAttribute: Boolean = {
        forwardOp.exists(_.property.uri.startsWith("@"))
      }
    }
  }

  /** Returns the attribute value from the current element of the stream reader. It assumes that it's placed on a start element. */
  private def attributeValue(reader: XMLStreamReader, attributeName: String): Option[String] = {
    val attributeValues = for(i <- 0 until reader.getAttributeCount if reader.getAttributeName(i).getLocalPart == attributeName) yield {
      reader.getAttributeValue(i)
    }
    attributeValues.headOption
  }

  // Evaluate property filter on the current element
  private def propertyFilterApplies(reader: XMLStreamReader, propertyFilter: PropertyFilter): Boolean = {
    val filterAttributeName = propertyFilter.property.uri.stripPrefix("@")
    reader.isStartElement &&
      attributeValue(reader, filterAttributeName).exists(value => propertyFilter.evaluate(s""""$value""""))
  }

  private def checkObjectPath(path: UntypedPath): Unit = {
    val validationRules = Seq[(String, Boolean)](
      s"Path '${path.normalizedSerialization}' contains an invalid operator. Only forward operators and property filters on attributes are supported in streaming mode." ->
        path.operators.forall(op => op.isInstanceOf[ForwardOperator] ||
          (op.isInstanceOf[PropertyFilter] && op.asInstanceOf[PropertyFilter].property.uri.startsWith("@")))
        ,
        "No #text path allowed inside object path with streaming mode enabled." ->
          path.operators.filter(_.isInstanceOf[PropertyFilter]).forall(_.asInstanceOf[PropertyFilter].property.uri != "#text")
    )
    for((assertErrorMessage, assertionValue) <- validationRules) {
      assert(assertionValue, assertErrorMessage)
    }
  }

  /**
    * Collects all paths inside the current element.
    * The parser must be positioned on the start element when calling this method.
    * On return, the parser will be positioned on the element that directly follows the element.
    */
  private def collectPaths(reader: XMLStreamReader, path: UntypedPath, onlyLeafNodes: Boolean, onlyInnerNodes: Boolean, depth: Int): Seq[TypedPath] = {
    assert(reader.isStartElement)
    assert(!(onlyInnerNodes && onlyLeafNodes), "onlyInnerNodes and onlyLeafNodes cannot be set to true at the same time")

    if(!file.nonEmpty) {
      return Seq.empty
    }

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

  private def fetchAttributePaths(reader: XMLStreamReader, path: UntypedPath): IndexedSeq[TypedPath] = {
    for (attributeIndex <- 0 until reader.getAttributeCount) yield {
      TypedPath(path ++ UntypedPath("@" + reader.getAttributeLocalName(attributeIndex)), ValueType.STRING, isAttribute = true)
    }
  }

  private def buildAttributeNode(reader: XMLStreamReader, attributeName: String): InMemoryXmlNode = {
    val value = attributeValue(reader, attributeName.stripPrefix("@"))
    assert(value.isDefined, s"Cannot build attribute node for missing attribute '$attributeName'.")
    reader.next()
    InMemoryXmlAttribute(attributeName, value.get, getPosition(reader))
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
    // Counts the number of elements started
    var elementStartedCount = 1
    while(elementStartedCount > 0) {
      if(reader.isStartElement) {
        elementStartedCount += 1
      } else if(reader.isEndElement) {
        elementStartedCount -= 1
      }
      reader.next()
    }
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
      def collectAttributes(): Unit = {
        for (attributeIndex <- 0 until reader.getAttributeCount) yield {
          val attributePath = "@" + reader.getAttributeLocalName(attributeIndex) :: currentPath
          addIfNotExists(paths, idx, attributePath)
          collectValues(attributePath.dropRight(basePathLength), reader.getAttributeValue(attributeIndex))
        }
      }
      if (basePathMatches(currentPath)) { collectAttributes() }
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
              collectAttributes()
            }
            val text = Try(reader.getElementText)
            text foreach { elemText =>
              collectValues(currentPath.dropRight(basePathLength), elemText)
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

  private def getPosition(reader: XMLStreamReader): XmlPosition = {
    XmlPosition(
      line = reader.getLocation.getLineNumber,
      column = reader.getLocation.getColumnNumber
    )
  }

  /**
    * The dataset task underlying the Datset this source belongs to
    *
    * @return
    */
  override lazy val underlyingTask: Task[DatasetSpec[Dataset]] = PlainTask(Identifier.fromAllowed(file.name), DatasetSpec(EmptyDataset))   //FIXME CMEM-1352 replace with actual task
}
