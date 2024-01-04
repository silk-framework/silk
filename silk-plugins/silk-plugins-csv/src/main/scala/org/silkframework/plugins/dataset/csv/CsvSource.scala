package org.silkframework.plugins.dataset.csv

import org.mozilla.universalchardet.UniversalDetector
import org.silkframework.config.{PlainTask, Prefixes, Task}
import org.silkframework.dataset._
import org.silkframework.entity._
import org.silkframework.entity.paths.{ForwardOperator, TypedPath, UntypedPath}
import org.silkframework.execution.EntityHolder
import org.silkframework.execution.local.GenericEntityTable
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.iterator.{AutoClose, CloseableIterator}
import org.silkframework.runtime.resource.Resource
import org.silkframework.util.{Identifier, Uri}

import java.io.{BufferedReader, IOException, InputStreamReader}
import java.net.{URI, URLEncoder}
import java.nio.charset.MalformedInputException
import java.util.logging.{Level, Logger}
import java.util.regex.Pattern
import scala.collection.immutable.ArraySeq
import scala.io.Codec
import scala.util.control.NonFatal
import scala.util.matching.Regex
import scala.util.{Success, Try}

class CsvSource(file: Resource,
                settings: CsvSettings = CsvSettings(),
                properties: String = "",
                uriPattern: String = "",
                regexFilter: String = "",
                ignoreBadLines: Boolean = false,
                detectSeparator: Boolean = false,
                detectSkipLinesBeginning: Boolean = false,
                specificTypeName: Option[String] = None,    // if the csv file represents a specific type which is not or can not be written as the file name
                autoDetectCodec: Boolean =false,
                maxLinesToDetectCodec: Option[Int] = None,
                ignoreMalformedInputExceptionInPropertyList: Boolean = false)
    extends DataSource
        with PathCoverageDataSource
        with PeakDataSource {

  private val logger = Logger.getLogger(getClass.getName)

  // How many lines should be used for detecting the encoding or separator etc.
  final val linesForDetection = 100

  val noPathSeparatorCharacter: Regex = "[:/?]".r

  val propertyList: IndexedSeq[String] = {
    if (properties.trim.nonEmpty) {
      // Parse the properties parameter and split it into valid properties
      for(property <- CsvSourceHelper.parse(properties).toIndexedSeq) yield {
        // Encode properties that are not already absolute URIs or already a valid URL encoded string
        Try(new URI(property)) match {
          case Success(uri) if uri.isAbsolute || noPathSeparatorCharacter.findFirstIn(property).isEmpty =>
            property
          case _ =>
            URLEncoder.encode(property, "UTF-8")
        }
      }
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
    } catch {
      case NonFatal(_) =>
        None
    }
  }

  override def toString: String = file.toString

  override def retrievePaths(t: Uri, depth: Int, limit: Option[Int])
                            (implicit userContext: UserContext, prefixes: Prefixes): IndexedSeq[TypedPath] = {
    if(t.toString.nonEmpty && t != typeUri)
      return IndexedSeq.empty

    try {
      for (property <- propertyList) yield {
        UntypedPath(ForwardOperator(Uri(property)) :: Nil).asStringTypedPath
      }
    } catch {
      case e: MalformedInputException =>
        throw new RuntimeException("Exception in CsvSource " + file.name, e)
    }
  }

  override def retrieve(entitySchema: EntitySchema, limitOpt: Option[Int] = None)
                       (implicit userContext: UserContext, prefixes: Prefixes): EntityHolder = {
    if (entitySchema.filter.operator.isDefined) {
      throw new NotImplementedError("Filter restrictions are not supported on CSV datasets!") // TODO: Implement Restriction handling!
    }
    retrieveEntities(entitySchema, limitOpt = limitOpt)
  }

  override def retrieveByUri(entitySchema: EntitySchema, entities: Seq[Uri])
                            (implicit userContext: UserContext, prefixes: Prefixes): EntityHolder = {
    if(entities.isEmpty) {
      GenericEntityTable(CloseableIterator.empty, entitySchema, underlyingTask)
    } else {
      retrieveEntities(entitySchema, entities.map(_.uri))
    }
  }

  def retrieveEntities(entityDesc: EntitySchema, entities: Seq[String] = Seq.empty, limitOpt: Option[Int] = None): EntityHolder = {
    if(!file.exists) {
      return GenericEntityTable(CloseableIterator.empty, entityDesc, underlyingTask, Seq.empty)
    }

    if(entityDesc.typeUri.toString.nonEmpty && entityDesc.typeUri != Uri(typeUri))
      return GenericEntityTable(CloseableIterator.empty, entityDesc, underlyingTask)

    logger.log(Level.FINE, "Retrieving data from CSV.")

    // Collect missing columns
    var missingColumns = Seq[String]()
    for (path <- entityDesc.typedPaths) {
      val property = path.operators.head.asInstanceOf[ForwardOperator].property.uri
      val propertyIndex = propertyList.indexOf(property)
      if (propertyIndex == -1) {
        if(!property.startsWith("#")) {
          missingColumns :+= property
        }
      }
    }

    // Return new iterable that generates an entity for each line
    val generator = TableEntityGenerator(entityDesc, propertyList.map(Uri(_)), allowMissingPaths = true)
    val retrievedEntities = new EntityIterator(entityDesc, entities.toSet, generator) with AutoClose[Entity]

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

  private class EntityIterator(entityDesc: EntitySchema,
                               entities: Set[String],
                               generator: TableEntityGenerator) extends CloseableIterator[Entity] {

    private val parser: CsvParser = csvParser(properties.trim.isEmpty)

    // Compile the line regex.
    private val regex: Pattern = if (regexFilter.nonEmpty) regexFilter.r.pattern else null

    // Cached next entity
    private var nextEntity: Option[Entity] = None

    // Line index
    private var index: Int = 0

    // Retrieve first entity
    retrieveNext()

    override def hasNext: Boolean = {
      nextEntity.isDefined
    }

    override def next(): Entity = {
      val entity = nextEntity.getOrElse(throw new NoSuchElementException("No more entities left."))
      retrieveNext()
      entity
    }

    override def close(): Unit = {
      parser.stopParsing()
    }

    private def retrieveNext(): Unit = {
      while {
        parser.parseNext() match {
          case Some(line) =>
            index += 1
            nextEntity = readEntity(line)
          case None =>
            nextEntity = None
            // Reached the end of the file
            return
        }
        nextEntity.isEmpty
      } do()
    }

    private def readEntity(line: Array[String]): Option[Entity] = {
      // Check for regex filter
      if ((properties.trim.nonEmpty || index >= 0) && (regexFilter.isEmpty || regex.matcher(line.mkString(csvSettings.separator.toString)).matches())) {
        // Check if line provides enough values
        if (propertyList.size <= line.length) {
          //Extract requested values
          val entityURI = generateEntityUri(index, line)
          //Build entity
          if (entities.isEmpty || entities.contains(entityURI)) {
            Some(generator.generate(entityURI, splitArrayValue(line), index, index))
          } else {
            // Entity is not part of the entity URI set
            None
          }
        } else {
          // Bad line
          handleBadLine(index, line)
          None
        }
      } else {
        // Line does not match regex
        None
      }
    }

    private def handleBadLine[U](index: Int, entry: Array[String]): Unit = {
      // Bad line
      if (!ignoreBadLines) {
        assert(propertyList.size <= entry.length, s"Invalid line ${index + 1}: '${entry.toSeq}' in resource '${file.name}' with " +
          s"${entry.length} elements. Expected number of elements ${propertyList.size}.")
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

    private def splitArrayValue[U](values: IndexedSeq[String]): IndexedSeq[Seq[String]] = {
      val entityValues = csvSettings.arraySeparator match {
        case None =>
          values.map(v => if (v != null) Seq(v) else Seq.empty[String])
        case Some(c) =>
          values.map(v => if (v != null) ArraySeq.unsafeWrapArray(v.split(c)) else Seq.empty[String])
      }
      entityValues
    }
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
    if (autoDetectCodec) {
      detectWorkingCodec
    } else {
      settings.codec
    }
  }

  private def detectWorkingCodec: Codec = {
    try {
      val detectedCharset = UniversalDetector.detectCharset(file.inputStream)
      if(Option(detectedCharset).nonEmpty) {
        Codec.string2codec(detectedCharset)
      } else {
        settings.codec
      }
    } catch {
      case ex: IOException =>
        logger.log(Level.WARNING, s"Could not detect encoding of CSV file '${file.name}'.", ex)
        settings.codec
    }
  }

  override def retrieveTypes(limit: Option[Int] = None)
                            (implicit userContext: UserContext, prefixes: Prefixes): Iterable[(String, Double)] = {
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
    val csvSource = new CsvSource(file, csvSettings, properties, uriPattern, regexFilter,
      detectSeparator = true, detectSkipLinesBeginning = true, autoDetectCodec = true, maxLinesToDetectCodec = Some(1000),
      ignoreMalformedInputExceptionInPropertyList = true)
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
  override lazy val underlyingTask: Task[DatasetSpec[Dataset]] = PlainTask(Identifier.fromAllowed(file.name), DatasetSpec(EmptyDataset))   //FIXME CMEM-1352 replace with actual task
}

case class CsvAutoconfiguredParameters(detectedSeparator: String, codecName: String, linesToSkip: Option[Int])

/**
  * The return value of the separator detection
  *
  * @param separator      the character used for separating fields in CSV
  * @param numberOfFields the detected number of fields when splitting with this separator
  */
case class DetectedSeparator(separator: Char, numberOfFields: Int, skipLinesBeginning: Int)