package org.silkframework.plugins.dataset.csv

import java.io.{BufferedReader, InputStream, InputStreamReader}
import java.net.URLEncoder
import java.nio.charset.MalformedInputException
import java.util.logging.Logger
import java.util.regex.Pattern
import java.util.zip.{ZipEntry, ZipFile, ZipInputStream}

import org.silkframework.config.{PlainTask, Task}
import org.silkframework.dataset._
import org.silkframework.entity.Path.IDX_PATH_IDX
import org.silkframework.entity._
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.{BulkResource, BulkResourceSupport}
import org.silkframework.util.{Identifier, Uri}

import scala.collection.mutable
import scala.io.Codec


/**
  * Wrapper for that provides access to BulkResources with an underlying data source object and
  * the bulk resource object.
  */
class BulkCsvDataSource(bulk: BulkResource, dataset: CsvDataset, settings: CsvSettings)
  extends DataSource
    with PathCoverageDataSource
    with PeakDataSource
    with TypedPathRetrieveDataSource {

  private val fallbackCodecs: List[Codec] = List(Codec("CP1252"))
  private val underlyingDataset: Option[CsvDataset] = Some(dataset)

  private def individualSources: Seq[BulkCsvDataSource] = for (stream <- bulk.inputStreams) yield {
//    var firstFile = true
    val subResource = BulkResource.createBulkResourceWithStream(bulk, stream)
    val subSource = new BulkCsvDataSource(subResource, dataset, settings)
    subSource
  }


  val propertyList: IndexedSeq[String] = {

    BulkResourceSupport.getNewlineInputStream
    val propertySet: mutable.HashSet[String] = new mutable.HashSet[String]()
    for (subResource <- bulk.inputStreams) {
      val props = if (!underlyingDataset.get.properties.trim.isEmpty) {
        CsvSourceHelper.parse(underlyingDataset.get.properties).toIndexedSeq
      }
      else {
        val first = BulkResourceSupport.getLinesFromInputStream(subResource)
        val fields = first.split(dataset.separator)
        CsvSourceHelper.convertHeaderFields(fields)
      }
      props.foreach(p => propertySet.add(p))
    }
    propertySet.toIndexedSeq
  }




  // FIXME returns only one type, because it's one resource, we might have a real concept, though
  // val zip = new ZipFile(bulk.path)
  // for (entry <- zip.entries()) { getName } would work


  override def retrieveTypes(limit: Option[Int])(implicit userContext: UserContext): Traversable[(String, Double)] = {
    Traversable((bulk.name, 1.0))
  }

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

    /**
      * Retrieves entities from this source which satisfy a specific entity schema.
      *
      * @param entitySchema The entity schema
      * @param limitOpt        Limits the maximum number of retrieved entities
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
              Path.IDX_PATH_IDX
            } else {
              throw new Exception("Property " + property + " not found in CSV " + bulk.name + ". Available properties: " + propertyList.mkString(", "))
            }
          } else {
            propertyIndex
          }
        }

      entityTraversable(entityDesc, entities, indices)
    }

    private def entityTraversable(entityDesc: EntitySchema,
                                  entities: Seq[String],
                                  indices: IndexedSeq[Int]): Traversable[Entity] = {
      new Traversable[Entity] {
        def foreach[U](f: Entity => U) {
          val parser: CsvParser = csvParser(underlyingDataset.get.properties.trim.isEmpty)
          val regex: Pattern = if (!underlyingDataset.get.regexFilter.isEmpty) underlyingDataset.get.regexFilter.r.pattern else null

          try {
            var entryOpt = parser.parseNext()
            var index = 1
            while (entryOpt.isDefined) {
              val entry = entryOpt.get
              if ((underlyingDataset.get.properties.trim.nonEmpty || index >= 0) &&
                (underlyingDataset.get.regexFilter.isEmpty || regex.matcher(entry.mkString(underlyingDataset.get.separator.toString)).matches())
              ) {
                if (propertyList.size <= entry.length) {
                  val values = collectValues(indices, entry, index)
                  val entityURI = generateEntityUri(index, entry)
                  if (entities.isEmpty || entities.contains(entityURI)) {
                    val entityValues: IndexedSeq[Seq[String]] = splitArrayValue(values)
                    f(Entity(entityURI, entityValues,entityDesc))
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

  protected def collectValues(indices: IndexedSeq[Int], entry: Array[String], entityIdx: Int): IndexedSeq[String] = {
    indices map {
      case IDX_PATH_IDX =>
        entityIdx.toString
      case idx: Int if idx >= 0 =>
        entry(idx)
    }
  }

  def csvParser(skipFirst: Boolean = false): CsvParser = {
    lazy val reader = getAndInitBufferedReaderForCsvFile()
    val parser = new CsvParser(Seq.empty, dataset.csvSettings)
    try {
      parser.beginParsing(reader)
      if(skipFirst) parser.parseNext()
      parser
    }
    catch {
      case e: Throwable =>
        parser.stopParsing()
        throw new RuntimeException("Problem during initialization of CSV parser: " + e.getMessage, e)
    }
  }

    private def handleBadLine[U](index: Int, entry: Array[String]): Unit = {
      if (!underlyingDataset.get.ignoreBadLines) {
        assert(propertyList.size <= entry.length, s"Invalid line ${index + 1}: '${entry.toSeq}' in resource '${bulk.name}' with " +
          s"${entry.length} elements. Expected number of elements ${propertyList.size}.")
      }
    }

    private def splitArrayValue[U](values: IndexedSeq[String]): IndexedSeq[Seq[String]] = {
      val entityValues = settings.separator match {
        case c: Char =>
          values.map(v => if (v != null) v.split(c).toSeq else Seq.empty[String])
        case _ =>
          values.map(v => if (v != null) Seq(v) else Seq.empty[String])
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
    val inputStream = bulk.inputStream
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
          Logger.getLogger(this.getClass.getSimpleName) fine s"Codec $c failed for input file ${bulk.name}"
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
    * The dataset task underlying the Datset this source belongs to
    *
    * @return
    */
  override def underlyingTask: Task[DatasetSpec[Dataset]] = PlainTask(Identifier.fromAllowed(bulk.name), DatasetSpec(EmptyDataset))
  //FIXME CMEM-1352 replace with actual task

  /**
    * returns the combined path. Depending on the data source the input path may or may not be modified based on the type URI.
    */
  override def combinedPath(typeUri: String, inputPath: Path): Path = ???
}
