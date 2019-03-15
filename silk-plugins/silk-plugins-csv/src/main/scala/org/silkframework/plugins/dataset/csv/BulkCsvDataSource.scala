package org.silkframework.plugins.dataset.csv

import java.io.{BufferedReader, InputStreamReader}
import java.net.URLEncoder
import java.nio.charset.MalformedInputException
import java.util.logging.{Level, Logger}
import java.util.regex.Pattern

import org.silkframework.config.{PlainTask, Task}
import org.silkframework.dataset._
import org.silkframework.entity.Path.IDX_PATH_IDX
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
object BulkCsvDataSource extends DataSource with PathCoverageDataSource with PeakDataSource with TypedPathRetrieveDataSource {

  private final val logger: Logger = Logger.getLogger(this.getClass.getSimpleName)

  /* all the stuff needed to emulate a valid source*/
  private var underlyingResource: Option[BulkResource] = None
  private var underlyingDataset: Option[CsvDataset] = None
  private var csvSettings: Option[CsvSettings] = None
  private var fallbackCodecs: List[Codec] = List.empty

  /**
    * Create data source that uses the bulk resource.
    *
    * @param bulkResource Zip Resource
    * @param dataset      Dataset
    * @return
    */
  def apply(bulkResource: BulkResource, dataset: CsvDataset, settings: CsvSettings,
            codecList: List[Codec] = List.empty[Codec]): DataSource = {

    underlyingDataset = Some(dataset)
    underlyingResource = Some(bulkResource)
    csvSettings = Some(settings)
    fallbackCodecs = codecList
    this
  }

  /**
    * The dataset task underlying the Datset this source belongs to
    *
    * @return
    */
  override def underlyingTask: Task[DatasetSpec[Dataset]] = underlyingDataset.map(ds => {
    PlainTask(ds.pluginSpec.id, new DatasetSpec(ds))
  }).getOrElse(throw new RuntimeException("The underlying data source for the BulkResourceDataSource is missing."))

  /**
    * Retrieves known types in this source.
    * Implementations are only required to work on a best effort basis i.e. it does not necessarily return any or all types.
    * The default implementation returns an empty traversable.
    *
    * @param limit Restricts the number of types to be retrieved. If not given, all found types are returned.
    *
    */
  override def retrieveTypes(limit: Option[Int])(implicit userContext: UserContext): Traversable[(String, Double)] = {
    var firstFile = true
    val individualSources = for (stream <- underlyingResource.get.inputStreams) yield {
      val subResource = BulkResource.createBulkResourceWithStream(underlyingResource.get, stream)
      val subSource = new CsvSource(
        subResource,
        CsvSettings(),
        underlyingDataset.get.properties,
        "",
        underlyingDataset.get.regexFilter,
        Codec(underlyingDataset.get.charset),
        if (!firstFile) 1 else underlyingDataset.get.linesToSkip,
        underlyingDataset.get.ignoreBadLines,
        detectSeparator = false, detectSkipLinesBeginning = false,
        fallbackCodecs = List(Codec("CP1252")), maxLinesToDetectCodec = None,
        ignoreMalformedInputExceptionInPropertyList = false
      )
      firstFile = false
      subSource
    }
    val types: mutable.HashSet[(String, Double)] = new mutable.HashSet[(String, Double)]
    for (source <- individualSources) {
      val subResourceTypes: Traversable[(String, Double)] = source.retrieveTypes(limit)
      subResourceTypes.foreach(t => types.add(t))
    }
    types
  }


  /**
    * Retrieves the most frequent paths in this source.
    * Implementations are only required to work on a best effort basis i.e. it does not necessarily return all paths in the source.
    * The default implementation returns an empty traversable.
    *
    * @param t     The entity type for which paths shall be retrieved
    * @param depth Only retrieve paths up to a certain length. If not given, only paths of length 1 are returned. Since
    *              this value can be set to Int.MaxValue, the source has to make sure that it returns a result that
    *              can still be handled, e.g. it is Ok for XML and JSON to return all paths, for GRAPH data models this
    *              would be infeasible.
    * @param limit Restricts the number of paths to be retrieved. If not given, all found paths are returned.
    * @return A Sequence of the found paths sorted by their frequency (most frequent first).
    */
  override def retrievePaths(t: Uri, depth: Int, limit: Option[Int])
                            (implicit userContext: UserContext): IndexedSeq[Path] = {
    val ps = try {
      for (property <- propertyList) yield {
        Path(ForwardOperator(Uri.parse(property)) :: Nil)
      }
    } catch {
      case e: MalformedInputException =>
        throw new RuntimeException("Exception in CsvSource " + underlyingResource.get.name, e)
    }
    println("PATHS: " + ps.mkString("|"))
    ps
  }

  /**
    * Retrieves entities from this source which satisfy a specific entity schema.
    *
    * @param entitySchema The entity schema
    * @param limit        Limits the maximum number of retrieved entities
    * @return A Traversable over the entities. The evaluation of the Traversable may be non-strict.
    */
  override def retrieve(entitySchema: EntitySchema, limitOpt: Option[Int] = None)
                       (implicit userContext: UserContext): Traversable[Entity] = {
    if (entitySchema.filter.operator.isDefined) {
      throw new NotImplementedError("Filter restrictions are not supported on CSV datasets!") // TODO: Implement Restriction handling!
    }
    val entities = retrieveEntities(entitySchema)
    limitOpt match {
      case Some(limit) =>
        entities.take(limit)
      case None =>
        entities
    }
  }

  def retrieveEntities(entityDesc: EntitySchema, entities: Seq[String] = Seq.empty): Traversable[Entity] = {
    val indices =
      for (path <- entityDesc.typedPaths) yield {
        val property = path.operators.head.asInstanceOf[ForwardOperator].property.uri
        val propertyIndex = propertyList.indexOf(property.toString)
        if (propertyIndex == -1) {
          if (property == "#idx") {
            IDX_PATH_IDX
          } else {
            throw new Exception("Property " + property + " not found in CSV " + underlyingResource.get.name + ". Available properties: " + propertyList.mkString(", "))
          }
        } else {
          propertyIndex
        }
      }

    // Return new Traversable that generates an entity for each line
    entityTraversable(entityDesc, entities, indices)
  }

  val propertyList: IndexedSeq[String] = {
    if (!underlyingDataset.get.properties.trim.isEmpty) {
      CsvSourceHelper.parse(underlyingDataset.get.properties).toIndexedSeq
    }
    else {
      CsvSourceHelper.convertHeaderFields(firstLine)
    }
  }

  private def firstLine: Array[String] = {
    var parser: Option[CsvParser] = None
    try {
      parser = Some(csvParser())
      parser.get.parseNext().getOrElse(Array())
    } finally {
      parser foreach (_.stopParsing())
    }
  }

  private def csvParser(skipFirst: Boolean = false): CsvParser = {
    lazy val reader = getAndInitBufferedReaderForCsvFile()
    val parser = new CsvParser(Seq.empty, csvSettings.get)
    try {
      parser.beginParsing(reader)
      if (skipFirst) parser.parseNext()
      parser
    } catch {
      case e: Throwable =>
        parser.stopParsing()
        throw new RuntimeException("Problem during initialization of CSV parser: " + e.getMessage, e)
    }
  }

  private def entityTraversable(entityDesc: EntitySchema,
                                entities: Seq[String],
                                indices: IndexedSeq[Int]): Traversable[Entity] = {
    new Traversable[Entity] {
      def foreach[U](f: Entity => U) {
        val parser: CsvParser = csvParser(underlyingDataset.get.properties.trim.isEmpty)

        // Compile the line regex.
        val regex: Pattern = if (!underlyingDataset.get.regexFilter.isEmpty) underlyingDataset.get.regexFilter.r.pattern else null

        try {
          // Iterate through all lines of the source file. If a *regexFilter* has been set, then use it to filter the rows.

          var entryOpt = parser.parseNext()
          var index = 1
          while (entryOpt.isDefined) {
            val entry = entryOpt.get
            if ((underlyingDataset.get.properties.trim.nonEmpty || index >= 0) && (underlyingDataset.get.regexFilter.isEmpty || regex.matcher(entry.mkString(csvSettings.get.separator.toString)).matches())) {
              if (propertyList.size <= entry.length) {
                //Extract requested values
                val values = collectValues(indices, entry, index)
                val entityURI = generateEntityUri(index, entry)
                //Build entity
                if (entities.isEmpty || entities.contains(entityURI)) {
                  val entityValues: IndexedSeq[Seq[String]] = splitArrayValue(values)
                  f(Entity(
                    uri = entityURI,
                    values = entityValues,
                    schema = entityDesc
                  ))
                }
              } else {
                handleBadLine(index, entry)
              }
            }
            index += 1
            entryOpt = parser.parseNext()
          }
        } finally {
          parser.stopParsing()
        }
      }
    }
  }

  private def handleBadLine[U](index: Int, entry: Array[String]): Unit = {
    // Bad line
    if (!underlyingDataset.get.ignoreBadLines) {
      assert(propertyList.size <= entry.length, s"Invalid line ${index + 1}: '${entry.toSeq}' in resource '${underlyingResource.get.name}' with " +
        s"${entry.length} elements. Expected number of elements ${propertyList.size}.")
    }
  }

  private def splitArrayValue[U](values: IndexedSeq[String]): IndexedSeq[Seq[String]] = {
    val entityValues = csvSettings.map(_.separator) match {
      case None =>
        values.map(v => if (v != null) Seq(v) else Seq.empty[String])
      case Some(c) =>
        values.map(v => if (v != null) v.split(c).toSeq else Seq.empty[String])
    }
    entityValues
  }

  private def generateEntityUri(index: Int, entry: Array[String]) = {
    if (underlyingDataset.get.uri.isEmpty) {
      genericEntityIRI(index.toString)
    } else {
      "\\{([^\\}]+)\\}".r.replaceAllIn(underlyingDataset.get.uri, m => {
        val propName = m.group(1)

        assert(propertyList.contains(propName))
        val value = entry(propertyList.indexOf(propName))
        URLEncoder.encode(value, "UTF-8")
      })
    }
  }


  private def collectValues(indices: IndexedSeq[Int], entry: Array[String], entityIdx: Int): IndexedSeq[String] = {
    indices map {
      case IDX_PATH_IDX =>
        entityIdx.toString
      case idx: Int if idx >= 0 =>
        entry(idx)
    }
  }


  // Skip lines that are not part of the CSV file, headers may be included
  private def initBufferedReader(reader: BufferedReader): Unit = {
    val nrLinesToSkip = underlyingDataset.get.linesToSkip
    for (_ <- 1 to nrLinesToSkip) {
      reader.readLine() // Skip line
    }
  }

  private def getAndInitBufferedReaderForCsvFile(): BufferedReader = {
    val reader = bufferedReaderForCsvFile()
    initBufferedReader(reader)
    reader
  }

  private def bufferedReaderForCsvFile(): BufferedReader = {
    getBufferedReaderForCsvFile(codecToUse)
  }

  private def getBufferedReaderForCsvFile(codec: Codec): BufferedReader = {
    val inputStream = underlyingResource.get.inputStream
    new BufferedReader(new InputStreamReader(inputStream, codec.decoder))
  }

  lazy val codecToUse: Codec = {
    if (fallbackCodecs.isEmpty) {
      Codec.UTF8
    } else {
      pickWorkingCodec
    }
  }

  private def pickWorkingCodec: Codec = {
    val tryCodecs = Codec(underlyingDataset.get.charset) :: fallbackCodecs
    for (c <- tryCodecs) {
      val reader = getBufferedReaderForCsvFile(c)
      // Test read
      try {
        var line = reader.readLine()
        var lineCount = 0
        while (line != null && Some(1000).forall(max => lineCount < max)) {
          line = reader.readLine()
          lineCount += 1
        }
        return c
      } catch {
        case e: MalformedInputException =>
          logger.fine(s"Codec $c failed for input file ${underlyingResource.get.name}")
      } finally {
        reader.close()
      }
    }
    Codec.UTF8
  }


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

  /**
    * returns the combined path. Depending on the data source the input path may or may not be modified based on the type URI.
    */
  override def combinedPath(typeUri: String, inputPath: Path): Path = ???
}
