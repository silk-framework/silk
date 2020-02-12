package org.silkframework.plugins.dataset.csv

import java.io.{BufferedReader, InputStreamReader}
import java.net.URLEncoder
import java.nio.charset.MalformedInputException
import java.util.logging.{Level, Logger}
import java.util.regex.Pattern

import org.silkframework.config.{PlainTask, Task}
import org.silkframework.dataset._
import org.silkframework.entity.paths.UntypedPath.{IDX_PATH_MISSING, IDX_PATH_IDX}
import org.silkframework.entity._
import org.silkframework.entity.paths.{ForwardOperator, TypedPath, UntypedPath}
import org.silkframework.execution.EntityHolder
import org.silkframework.execution.local.GenericEntityTable
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.Resource
import org.silkframework.util.{Identifier, Uri}

import scala.io.Codec
import scala.util.control.NonFatal

class CsvSource(file: Resource,
                settings: CsvSettings = CsvSettings(),
                properties: String = "",
                uriPattern: String = "",
                regexFilter: String = "",
                codec: Codec = Codec.UTF8,
                ignoreBadLines: Boolean = false,
                detectSeparator: Boolean = false,
                detectSkipLinesBeginning: Boolean = false,
                specificTypeName: Option[String] = None,    // if the csv file represents a specific type which is not or can not be written as the file name
                // If the text file fails to be read because of a MalformedInputException, try other codecs
                fallbackCodecs: List[Codec] = List(),
                maxLinesToDetectCodec: Option[Int] = None,
                ignoreMalformedInputExceptionInPropertyList: Boolean = false)
    extends DataSource
        with PathCoverageDataSource
        with PeakDataSource {

  private val logger = Logger.getLogger(getClass.getName)

  // How many lines should be used for detecting the encoding or separator etc.
  final val linesForDetection = 100

  val propertyList: IndexedSeq[String] = {
    if (!properties.trim.isEmpty) {
      CsvSourceHelper.parse(properties).toIndexedSeq
    } else {
      try {
        CsvSourceHelper.convertHeaderFields(firstLine)
      } catch {
        case ex: RuntimeException =>
          if(ignoreMalformedInputExceptionInPropertyList) {
            IndexedSeq.empty
          } else {
            throw ex
          }
      }
    }
  }

  // Number of lines in input file (including header and potential skipped lines)
  lazy val nrLines: Long = {
    val reader = bufferedReaderForCsvFile()
    var count = 0L
    while (reader.readLine() != null) {
      count += 1
    }
    reader.close()
    count
  }

  lazy val (csvSettings, skipLinesAutomatic): (CsvSettings, Option[Int]) = {
    var csvSettings = settings
    lazy val detectedSeparator = detectSeparatorChar()
    lazy val separatorChar = detectedSeparator map (_.separator)
    lazy val skipLinesBeginningAutoDetected = detectedSeparator map (_.skipLinesBeginning)
    if (detectSeparator) {
      csvSettings = csvSettings.copy(separator = separatorChar getOrElse settings.separator)
    }
    val skipNrLines = if (detectSkipLinesBeginning) {
      skipLinesBeginningAutoDetected
    } else {
      None
    }
    (csvSettings, skipNrLines)
  }

  // automatically detect the separator, returns None if confidence is too low
  private def detectSeparatorChar(): Option[DetectedSeparator] = {
    try {
      CsvSeparatorDetector.detectSeparatorChar(bufferedReaderForCsvFile(), settings, linesForDetection)
    } finally {
      None
    }
  }

  override def toString: String = file.toString

  override def retrievePaths(t: Uri, depth: Int, limit: Option[Int])
                            (implicit userContext: UserContext): IndexedSeq[TypedPath] = {
    if(t.toString.nonEmpty && t != typeUri)
      return IndexedSeq.empty

    try {
      for (property <- propertyList) yield {
        UntypedPath(ForwardOperator(Uri.parse(property)) :: Nil).asStringTypedPath
      }
    } catch {
      case e: MalformedInputException =>
        throw new RuntimeException("Exception in CsvSource " + file.name, e)
    }
  }

  override def retrieve(entitySchema: EntitySchema, limitOpt: Option[Int] = None)
                       (implicit userContext: UserContext): EntityHolder = {
    if (entitySchema.filter.operator.isDefined) {
      throw new NotImplementedError("Filter restrictions are not supported on CSV datasets!") // TODO: Implement Restriction handling!
    }
    retrieveEntities(entitySchema)
  }

  override def retrieveByUri(entitySchema: EntitySchema, entities: Seq[Uri])
                            (implicit userContext: UserContext): EntityHolder = {
    if(entities.isEmpty) {
      GenericEntityTable(Seq.empty, entitySchema, underlyingTask)
    } else {
      val entitySet = entities.map(_.uri.toString).toSet
      val entityTraversal = retrieveEntities(entitySchema)
      val filteredEntities = entityTraversal filter (e => entitySet.contains(e.uri.toString))
      filteredEntities
    }
  }

  def retrieveEntities(entityDesc: EntitySchema, entities: Seq[String] = Seq.empty, limitOpt: Option[Int] = None): EntityHolder = {
    if(!file.exists) {
      return GenericEntityTable(Seq.empty[Entity], entityDesc, underlyingTask, Seq.empty)
    }

    if(entityDesc.typeUri.toString.nonEmpty && entityDesc.typeUri != Uri(typeUri))
      return GenericEntityTable(Traversable.empty, entityDesc, underlyingTask)

    logger.log(Level.FINE, "Retrieving data from CSV.")

    // Retrieve the indices of the request paths
    var missingColumns = Seq[String]()
    val indices =
      for (path <- entityDesc.typedPaths) yield {
        val property = path.operators.head.asInstanceOf[ForwardOperator].property.uri
        val propertyIndex = propertyList.indexOf(property.toString)
        if (propertyIndex == -1) {
          if(property == "#idx") {
            IDX_PATH_IDX
          } else {
            missingColumns :+= property
            IDX_PATH_MISSING
          }
        } else {
          propertyIndex
        }
      }

    // Return new Traversable that generates an entity for each line
    val retrievedEntities = entityTraversable(entityDesc, entities, indices)

    val limitedEntities = limitOpt match {
      case Some(limit) =>
        retrievedEntities.take(limit)
      case None =>
        retrievedEntities
    }

    val missingColumnMessages =
      if(missingColumns.isEmpty) {
        Seq.empty
      } else {
        Seq(s"Column(s) ${missingColumns.mkString(", ")} not found in CSV ${file.name}. Values for missing columns will be empty. Available columns: ${propertyList.mkString(", ")}.")
      }

    GenericEntityTable(limitedEntities, entityDesc, underlyingTask, missingColumnMessages)
  }

  private def entityTraversable(entityDesc: EntitySchema,
                                entities: Seq[String],
                                indices: IndexedSeq[Int]): Traversable[Entity] = {
    new Traversable[Entity] {
      def foreach[U](f: Entity => U) {
        val parser: CsvParser = csvParser(properties.trim.isEmpty)

        // Compile the line regex.
        val regex: Pattern = if (!regexFilter.isEmpty) regexFilter.r.pattern else null

        try {
          // Iterate through all lines of the source file. If a *regexFilter* has been set, then use it to filter the rows.

          var entryOpt = parser.parseNext()
          var index = 1
          while (entryOpt.isDefined) {
            val entry = entryOpt.get
            if ((properties.trim.nonEmpty || index >= 0) && (regexFilter.isEmpty || regex.matcher(entry.mkString(csvSettings.separator.toString)).matches())) {
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

  private def collectValues(indices: IndexedSeq[Int], entry: Array[String], entityIdx: Int): IndexedSeq[String] = {
    indices map {
      case IDX_PATH_MISSING =>
        null // splitArrayValue will generate an empty sequence from this.
      case IDX_PATH_IDX =>
        entityIdx.toString
      case idx: Int if idx >= 0 =>
        entry(idx)
    }
  }

  /** Returns a generated entity URI.
    * The default URI pattern is to use the prefix and the line number.
    * However the user can specify a different URI pattern (in the *uri* property), which is then used to
    * build the entity URI. An example of such pattern is 'urn:zyx:{id}' where *id* is a name of a property
    * as defined in the *properties* field. */
  private def generateEntityUri(index: Int, entry: Array[String]) = {
    if (uriPattern.isEmpty) {
      genericEntityIRI(index.toString)
    } else {
      "\\{([^\\}]+)\\}".r.replaceAllIn(uriPattern, m => {
        val propName = m.group(1)

        assert(propertyList.contains(propName))
        val value = entry(propertyList.indexOf(propName))
        URLEncoder.encode(value, "UTF-8")
      })
    }
  }

  private def handleBadLine[U](index: Int, entry: Array[String]): Unit = {
    // Bad line
    if (!ignoreBadLines) {
      assert(propertyList.size <= entry.length, s"Invalid line ${index + 1}: '${entry.toSeq}' in resource '${file.name}' with " +
          s"${entry.length} elements. Expected number of elements ${propertyList.size}.")
    }
  }

  private def splitArrayValue[U](values: IndexedSeq[String]): IndexedSeq[Seq[String]] = {
    val entityValues = csvSettings.arraySeparator match {
      case None =>
        values.map(v => if (v != null) Seq(v) else Seq.empty[String])
      case Some(c) =>
        values.map(v => if (v != null) v.split(c).toSeq else Seq.empty[String])
    }
    entityValues
  }

  private def csvParser(skipFirst: Boolean = false): CsvParser = {
    lazy val reader = getAndInitBufferedReaderForCsvFile()
    val parser = new CsvParser(Seq.empty, csvSettings) // Here we could only load the required indices as a performance improvement
    try {
      parser.beginParsing(reader)
      if(skipFirst) parser.parseNext()
      parser
    } catch {
      case NonFatal(ex) =>
        parser.stopParsing()
        throw new RuntimeException("Problem during initialization of CSV parser: " + ex.getMessage, ex)
    }
  }

  private def firstLine: Array[String] = {
    var parser: Option[CsvParser] = None
    if(!file.exists) {
      Array.empty[String]
    } else {
      try {
        parser = Some(csvParser())
        parser.get.parseNext().getOrElse(Array())
      } finally {
        parser foreach (_.stopParsing())
      }
    }
  }

  // Skip lines that are not part of the CSV file, headers may be included
  private def initBufferedReader(reader: BufferedReader): Unit = {
    val nrLinesToSkip = skipLinesAutomatic getOrElse csvSettings.linesToSkip
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
    val inputStream = file.inputStream
    new BufferedReader(new InputStreamReader(inputStream, codec.decoder))
  }

  lazy val codecToUse: Codec = {
    if (fallbackCodecs.isEmpty) {
      codec
    } else {
      pickWorkingCodec
    }
  }

  private def pickWorkingCodec: Codec = {
    val tryCodecs = codec :: fallbackCodecs
    for (c <- tryCodecs) {
      val reader = getBufferedReaderForCsvFile(c)
      // Test read
      try {
        var line = reader.readLine()
        var lineCount = 0
        while (line != null && maxLinesToDetectCodec.forall(max => lineCount < max)) {
          line = reader.readLine()
          lineCount += 1
        }
        return c
      } catch {
        case e: MalformedInputException =>
          logger.fine(s"Codec $c failed for input file ${file.name}")
      } finally {
        reader.close()
      }
    }
    codec
  }

  override def retrieveTypes(limit: Option[Int] = None)
                            (implicit userContext: UserContext): Traversable[(String, Double)] = {
    Seq((typeUri, 1.0))
  }

  private lazy val typeUri = {
    val uri = Uri(specificTypeName.getOrElse(file.name))
    if(uri.isValidUri)
      uri
    else{
      val segments = uri.uri.split("/")
        .map(_.trim)
        .map(seg => URLEncoder.encode(seg, "UTF-8"))
      Uri(segments.mkString("/"))
    }
  }

  /**
    * returns the combined path. Depending on the data source the input path may or may not be modified based on the type URI.
    */
  override def combinedPath(typeUri: String, inputPath: UntypedPath): UntypedPath = inputPath

  def autoConfigure(): CsvAutoconfiguredParameters = {
    val csvSource = new CsvSource(file, csvSettings, properties, uriPattern, regexFilter, codec,
      detectSeparator = true, detectSkipLinesBeginning = true, fallbackCodecs = List(Codec.ISO8859), maxLinesToDetectCodec = Some(1000))
    val detectedSettings = csvSource.csvSettings
    val detectedSeparator = detectedSettings.separator.toString
    // Skip one more line if header was detected and property list set

    CsvAutoconfiguredParameters(detectedSeparator, csvSource.codecToUse.name, csvSource.skipLinesAutomatic)
  }

  /**
    * The dataset task underlying the Datset this source belongs to
    *
    * @return
    */
  override def underlyingTask: Task[DatasetSpec[Dataset]] = PlainTask(Identifier.fromAllowed(file.name), DatasetSpec(EmptyDataset))   //FIXME CMEM-1352 replace with actual task
}

case class CsvAutoconfiguredParameters(detectedSeparator: String, codecName: String, linesToSkip: Option[Int])

/**
  * The return value of the separator detection
  *
  * @param separator      the character used for separating fields in CSV
  * @param numberOfFields the detected number of fields when splitting with this separator
  */
case class DetectedSeparator(separator: Char, numberOfFields: Int, skipLinesBeginning: Int)