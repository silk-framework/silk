package org.silkframework.plugins.dataset.csv

import java.nio.charset.MalformedInputException

import org.silkframework.dataset._
import org.silkframework.entity._
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.BulkResource
import org.silkframework.util.Uri

import scala.collection.mutable
import scala.io.Codec


/**
  * Wrapper for that provides access to BulkResources with an underlying data source object and
  * the bulk resource object.
  */
class BulkCsvDataSource(bulk: BulkResource, dataset: CsvDataset) extends CsvSource(bulk) with PathCoverageDataSource with PeakDataSource with TypedPathRetrieveDataSource {

  private final val fallbackCodecs: List[Codec] = List(Codec("CP1252"))

  private val underlyingDataset: Option[CsvDataset] = Some(dataset)

  private def individualSources: Seq[CsvSource] = for (stream <- bulk.inputStreams) yield {
    var firstFile = true
    val subResource = BulkResource.createBulkResourceWithStream(bulk, stream)
    val subSource = new CsvSource(
      subResource,
      underlyingDataset.get.csvSettings,
      underlyingDataset.get.properties,
      underlyingDataset.get.uri,
      underlyingDataset.get.regexFilter,
      Codec(underlyingDataset.get.charset),
      if (!firstFile) 1 else underlyingDataset.get.linesToSkip,
      underlyingDataset.get.ignoreBadLines,
      detectSeparator = false, detectSkipLinesBeginning = false,
      fallbackCodecs = fallbackCodecs, maxLinesToDetectCodec = None,
      ignoreMalformedInputExceptionInPropertyList = false
    )
    firstFile = false
    subSource
  }

  /**
    * Overwrite because of collecting all sub resource properties
    */
  override val propertyList: IndexedSeq[String] = {
    val propertySet: mutable.HashSet[String] = new mutable.HashSet[String]()
    for (subSource: CsvSource <- individualSources) {
      val props = if (!underlyingDataset.get.properties.trim.isEmpty) {
        CsvSourceHelper.parse(underlyingDataset.get.properties).toIndexedSeq
      }
      else {
        CsvSourceHelper.convertHeaderFields(firstLine(subSource))
      }
      props.foreach(p => propertySet.add(p))
    }
    propertySet.toIndexedSeq
  }

  /**
    * Rewrite because of collecting the first line of a specific sub resource.
    */
  private def firstLine(subSource: CsvSource): Array[String] = {
    var parser: Option[CsvParser] = None
    try {
      parser = Some(subSource.csvParser())
      parser.get.parseNext().getOrElse(Array())
    } finally {
      parser foreach (_.stopParsing())
    }
  }

  /**
    * Override because of collecting all sub resource types
    */
  override def retrieveTypes(limit: Option[Int])(implicit userContext: UserContext): Traversable[(String, Double)] = {
    val types: mutable.HashSet[(String, Double)] = new mutable.HashSet[(String, Double)]
    for (subSource <- individualSources) {
      val subResourceTypes: Traversable[(String, Double)] = subSource.retrieveTypes(limit)
      subResourceTypes.foreach(t => types.add(t))
    }
    types
  }

  /**
    * Override because of collecting all sub resource paths. Will that use the impl. above or in the trait
    * if that is not overwritten here. Who knows? FIXME
    */
  override def retrievePaths(t: Uri, depth: Int, limit: Option[Int])
                            (implicit userContext: UserContext): IndexedSeq[Path] = {
    try {
      for (property <- propertyList) yield {
        Path(ForwardOperator(Uri.parse(property)) :: Nil)
      }
    }
    catch {
      case e: MalformedInputException =>
        throw new RuntimeException(s"Exception: ${e.getMessage} in CsvBulkSource: ${bulk.name}")
    }
  }

  //  /**
  //    * Retrieves entities from this source which satisfy a specific entity schema.
  //    *
  //    * @param entitySchema The entity schema
  //    * @param limitOpt        Limits the maximum number of retrieved entities
  //    * @return A Traversable over the entities. The evaluation of the Traversable may be non-strict.
  //    */
  //  override def retrieve(entitySchema: EntitySchema, limitOpt: Option[Int] = None)
  //                       (implicit userContext: UserContext): Traversable[Entity] = {
  //    if (entitySchema.filter.operator.isDefined) {
  //      throw new NotImplementedError("Filter restrictions are not supported on CSV datasets!") // TODO: Implement Restriction handling!
  //    }
  //    val entities = retrieveEntities(entitySchema)
  //    limitOpt match {
  //      case Some(limit) =>
  //        entities.take(limit)
  //      case None =>
  //        entities
  //    }
  //  }

  //  def retrieveEntities(entityDesc: EntitySchema, entities: Seq[String] = Seq.empty): Traversable[Entity] = {
  //    val indices =
  //      for (path <- entityDesc.typedPaths) yield {
  //        val property = path.operators.head.asInstanceOf[ForwardOperator].property.uri
  //        val propertyIndex = propertyList.indexOf(property.toString)
  //        if (propertyIndex == -1) {
  //          if (property == "#idx") {
  //            IDX_PATH_IDX
  //          } else {
  //            throw new Exception("Property " + property + " not found in CSV " + underlyingResource.get.name + ". Available properties: " + propertyList.mkString(", "))
  //          }
  //        } else {
  //          propertyIndex
  //        }
  //      }
  //
  //    // Return new Traversable that generates an entity for each line
  //    entityTraversable(entityDesc, entities, indices)
  //  }

  //  private def entityTraversable(entityDesc: EntitySchema,
  //                                entities: Seq[String],
  //                                indices: IndexedSeq[Int]): Traversable[Entity] = {
  //    new Traversable[Entity] {
  //      def foreach[U](f: Entity => U) {
  //        val parser: CsvParser = csvParser(underlyingDataset.get.properties.trim.isEmpty)
  //
  //        val regex: Pattern = if (!underlyingDataset.get.regexFilter.isEmpty) underlyingDataset.get.regexFilter.r.pattern else null
  //
  //        try {
  //          var entryOpt = parser.parseNext()
  //          var index = 1
  //          while (entryOpt.isDefined) {
  //            val entry = entryOpt.get
  //            if ((underlyingDataset.get.properties.trim.nonEmpty || index >= 0) &&
  //              (underlyingDataset.get.regexFilter.isEmpty || regex.matcher(entry.mkString(csvSettings.separator.toString)).matches())
  //            ) {
  //              if (propertyList.size <= entry.length) {
  //                val values = collectValues(indices, entry, index)
  //                val entityURI = generateEntityUri(index, entry)
  //                if (entities.isEmpty || entities.contains(entityURI)) {
  //                  val entityValues: IndexedSeq[Seq[String]] = splitArrayValue(values)
  //                  f(Entity(
  //                    uri = entityURI,
  //                    values = entityValues,
  //                    schema = entityDesc
  //                  ))
  //                }
  //              } else {
  //                handleBadLine(index, entry)
  //              }
  //            }
  //            index += 1
  //            entryOpt = parser.parseNext()
  //          }
  //        } finally {
  //          parser.stopParsing()
  //        }
  //      }
  //    }
  //  }

  //  override def handleBadLine[U](index: Int, entry: Array[String]): Unit = {
  //    if (!underlyingDataset.get.ignoreBadLines) {
  //      assert(propertyList.size <= entry.length, s"Invalid line ${index + 1}: '${entry.toSeq}' in resource '${bulk.get.name}' with " +
  //        s"${entry.length} elements. Expected number of elements ${propertyList.size}.")
  //    }
  //  }

  //  private def splitArrayValue[U](values: IndexedSeq[String]): IndexedSeq[Seq[String]] = {
  //    val entityValues = csvSettings.map(_.separator) match {
  //      case None =>
  //        values.map(v => if (v != null) Seq(v) else Seq.empty[String])
  //      case Some(c) =>
  //        values.map(v => if (v != null) v.split(c).toSeq else Seq.empty[String])
  //    }
  //    entityValues
  //  }

//  private def generateEntityUri(index: Int, entry: Array[String]) = {
//    if (underlyingDataset.get.uri.isEmpty) {
//      genericEntityIRI(index.toString)
//    } else {
//      "\\{([^\\}]+)\\}".r.replaceAllIn(underlyingDataset.get.uri, m => {
//        val propName = m.group(1)
//
//        assert(propertyList.contains(propName))
//        val value = entry(propertyList.indexOf(propName))
//        URLEncoder.encode(value, "UTF-8")
//      })
//    }
//  }


//  private def collectValues(indices: IndexedSeq[Int], entry: Array[String], entityIdx: Int): IndexedSeq[String] = {
//    indices map {
//      case IDX_PATH_IDX =>
//        entityIdx.toString
//      case idx: Int if idx >= 0 =>
//        entry(idx)
//    }
//  }


//  private def initBufferedReader(reader: BufferedReader): Unit = {
//    val nrLinesToSkip = underlyingDataset.get.linesToSkip
//    for (_ <- 1 to nrLinesToSkip) {
//      reader.readLine() // Skip line
//    }
//  }
//
//  private def getAndInitBufferedReaderForCsvFile(): BufferedReader = {
//    val reader = bufferedReaderForCsvFile()
//    initBufferedReader(reader)
//    reader
//  }

//  private def bufferedReaderForCsvFile(): BufferedReader = {
//    getBufferedReaderForCsvFile(codecToUse)
//  }

//  private def getBufferedReaderForCsvFile(codec: Codec): BufferedReader = {
//    val inputStream = underlyingResource.get.inputStream
//    new BufferedReader(new InputStreamReader(inputStream, codec.decoder))
//  }

//  lazy val codecToUse: Codec = {
//    if (fallbackCodecs.isEmpty) {
//      Codec.UTF8
//    } else {
//      pickWorkingCodec
//    }
//  }
//
//  private def pickWorkingCodec: Codec = {
//    val tryCodecs = Codec(underlyingDataset.get.charset) :: fallbackCodecs
//    for (c <- tryCodecs) {
//      val reader = getBufferedReaderForCsvFile(c)
//      try {
//        var line = reader.readLine()
//        var lineCount = 0
//        while (line != null && Some(1000).forall(max => lineCount < max)) {
//          line = reader.readLine()
//          lineCount += 1
//        }
//        return c
//      } catch {
//        case e: MalformedInputException =>
//          logger.fine(s"Codec $c failed for input file ${underlyingResource.get.name}")
//      } finally {
//        reader.close()
//      }
//    }
//    Codec.UTF8
//  }


  /**
    * Retrieves a list of entities from this source.
    *
    * @param entitySchema The entity schema
    * @param entities     The URIs of the entities to be retrieved.
    * @return A Traversable over the entities. The evaluation of the Traversable may be non-strict.
    */
  override def retrieveByUri(entitySchema: EntitySchema, entities: Seq[Uri])(implicit userContext: UserContext): Traversable[Entity] = {
    throw new NotImplementedError()
  }

  /**
    * Retrieves typed paths. The value type of the path denotes what type this path has in the corresponding data source.
    * The [[org.silkframework.entity.UriValueType]] has a special meaning for non-RDF data sources, in that it specifies
    * non-literal values, e.g. a XML element with nested elements, a JSON object or array of objects etc.
    *
    * @param typeUri The type URI. For non-RDF data types this is not a URI, e.g. XML or JSON this may express the path from the root.
    * @param depth   The maximum depths of the returned paths. This is only a limit, but not a guarantee that all paths
    *                of this length are actually returned.
    * @param limit   The maximum number of typed paths returned. None stands for unlimited.
    */
  override def retrieveTypedPath(typeUri: Uri,
                                 depth: Int,
                                 limit: Option[Int])
                                (implicit userContext: UserContext): IndexedSeq[TypedPath] = {
    retrievePaths(typeUri, depth, limit).map(_.asStringTypedPath)
  }

}
