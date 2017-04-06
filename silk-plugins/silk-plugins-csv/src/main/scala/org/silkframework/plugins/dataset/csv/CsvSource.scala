package org.silkframework.plugins.dataset.csv

import java.io.{BufferedReader, InputStreamReader, StringReader}
import java.net.URLEncoder
import java.nio.charset.MalformedInputException
import java.util.logging.{Level, Logger}
import java.util.regex.Pattern

import org.silkframework.dataset.{DataSource, PathCoverageDataSource}
import org.silkframework.entity._
import org.silkframework.runtime.resource.Resource
import org.silkframework.util.Uri

import scala.collection.mutable
import scala.collection.mutable.{HashMap => MMap}
import scala.io.Codec

class CsvSource(file: Resource,
                settings: CsvSettings = CsvSettings(),
                properties: String = "",
                prefix: String = "",
                uri: String = "",
                regexFilter: String = "",
                codec: Codec = Codec.UTF8,
                skipLinesBeginning: Int = 0,
                ignoreBadLines: Boolean = false,
                detectSeparator: Boolean = false,
                detectSkipLinesBeginning: Boolean = false,
                // If the text file fails to be read because of a MalformedInputException, try other codecs
                fallbackCodecs: List[Codec] = List(),
                maxLinesToDetectCodec: Option[Int] = None) extends DataSource with PathCoverageDataSource {

  private val logger = Logger.getLogger(getClass.getName)

  // How many lines should be used for detecting the encoding or separator etc.
  final val linesForDetection = 100

  lazy val propertyList: IndexedSeq[String] = {
    val parser = new CsvParser(Seq.empty, csvSettings)
    if (!properties.trim.isEmpty) {
      CsvSourceHelper.parse(properties).toIndexedSeq
    } else {
      val parser = csvParser()
      val firstLine = parser.parseNext()
      parser.stopParsing()
      if (firstLine.isDefined && firstLine.nonEmpty) {
        firstLine.get
            .takeWhile(Option(_).isDefined) // Break if a header field is null
            .map { s =>
          if (Uri(s).isValidUri && (Option(prefix).isEmpty || prefix == "")) {
            s
          } else {
            URLEncoder.encode(s, "UTF-8")
          }
        }.toIndexedSeq
      } else {
        mutable.IndexedSeq()
      }
    }
  }

  // Number of lines in input file (including header and potential skipped lines)
  lazy val nrLines = {
    val reader = getBufferedReaderForCsvFile()
    var count = 0l
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
      SeparatorDetector.detectSeparatorChar(getBufferedReaderForCsvFile(), settings, linesForDetection)
    } finally {
      None
    }
  }

  override def toString = file.toString

  override def retrievePaths(t: Uri, depth: Int, limit: Option[Int]): IndexedSeq[Path] = {
    try {
      for (property <- propertyList) yield {
        Path(ForwardOperator(prefix + property) :: Nil)
      }
    } catch {
      case e: MalformedInputException =>
        throw new RuntimeException("Exception in CsvSource " + file.name, e)
    }
  }

  override def retrieve(entitySchema: EntitySchema, limitOpt: Option[Int] = None): Traversable[Entity] = {
    if (entitySchema.filter.operator.isDefined) {
      ??? // TODO: Implement Restriction handling!
    }
    val entities = retrieveEntities(entitySchema)
    limitOpt match {
      case Some(limit) =>
        entities.take(limit)
      case None =>
        entities
    }
  }

  override def retrieveByUri(entitySchema: EntitySchema, entities: Seq[Uri]): Seq[Entity] = {
    val entities = retrieveEntities(entitySchema)
    entities.toSeq
  }


  def retrieveEntities(entityDesc: EntitySchema, entities: Seq[String] = Seq.empty): Traversable[Entity] = {

    logger.log(Level.FINE, "Retrieving data from CSV.")

    // Retrieve the indices of the request paths
    val indices =
      for (path <- entityDesc.typedPaths) yield {
        val property = path.path.operators.head.asInstanceOf[ForwardOperator].property.uri.stripPrefix(prefix)
        val propertyIndex = propertyList.indexOf(property)
        if (propertyIndex == -1) {
          throw new Exception("Property " + property + " not found in CSV " + file.name + ". Available properties: " + propertyList.mkString(", "))
        }
        propertyIndex
      }

    // Return new Traversable that generates an entity for each line
    entityTraversable(entityDesc, entities, indices)
  }

  private def entityTraversable(entityDesc: EntitySchema,
                                entities: Seq[String],
                                indices: IndexedSeq[Int]): Traversable[Entity] = {
    new Traversable[Entity] {
      def foreach[U](f: Entity => U) {
        val parser: CsvParser = csvParser()

        // Compile the line regex.
        val regex: Pattern = if (!regexFilter.isEmpty) regexFilter.r.pattern else null

        try {
          // Iterate through all lines of the source file. If a *regexFilter* has been set, then use it to filter
          // the rows.
          var entryOpt = parser.parseNext()
          var index = 0
          while (entryOpt.isDefined) {
            val entry = entryOpt.get
            if (!(properties.trim.isEmpty && 0 == index) && (regexFilter.isEmpty || regex.matcher(entry.mkString(csvSettings.separator.toString)).matches())) {
              if (propertyList.size <= entry.length) {
                //Extract requested values
                val values = indices.map(entry(_))
                val entityURI = generateEntityUri(index, entry)

                //Build entity
                if (entities.isEmpty || entities.contains(entityURI)) {
                  val entityValues: IndexedSeq[Seq[String]] = splitArrayValue(values)
                  f(new Entity(
                    uri = entityURI,
                    values = entityValues,
                    desc = entityDesc
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

  private def handleBadLine[U](index: Int, entry: Array[String]) = {
    // Bad line
    if (!ignoreBadLines) {
      assert(propertyList.size <= entry.length, s"Invalid line ${index + 1}: '${entry.toSeq}' in resource '${file.name}' with ${entry.size} elements. Expected number of elements ${propertyList.size}.")
    }
  }

  private def splitArrayValue[U](values: IndexedSeq[String]): IndexedSeq[Seq[String]] = {
    val entityValues = csvSettings.arraySeparator match {
      case None =>
        values.map(v => if (v != null) Seq(v) else Seq.empty[String])
      case Some(c) =>
        values.map(v => if (v != null) v.split(c.toString, -1).toSeq else Seq.empty[String])
    }
    entityValues
  }

  /** Returns a generated entity URI.
    * The default URI pattern is to use the prefix and the line number.
    * However the user can specify a different URI pattern (in the *uri* property), which is then used to
    * build the entity URI. An example of such pattern is 'urn:zyx:{id}' where *id* is a name of a property
    * as defined in the *properties* field. */
  private def generateEntityUri(index: Int, entry: Array[String]) = {
    if (uri.isEmpty && prefix.isEmpty) {
      file.name + "/" + (index + 1)
    } else if (uri.isEmpty) {
      prefix + (index + 1)
    } else {
      "\\{([^\\}]+)\\}".r.replaceAllIn(uri, m => {
        val propName = m.group(1)

        assert(propertyList.contains(propName))
        val value = entry(propertyList.indexOf(propName))
        URLEncoder.encode(value, "UTF-8")
      })
    }
  }

  private def csvParser(): CsvParser = {
    lazy val reader = getAndInitBufferedReaderForCsvFile
    val parser = new CsvParser(Seq.empty, csvSettings) // Here we could only load the required indices as a performance improvement
    parser.beginParsing(reader)
    parser
  }

  // Skip lines that are not part of the CSV file, headers may be included
  private def initBufferedReader(reader: BufferedReader): Unit = {
    val nrLinesToSkip = skipLinesAutomatic getOrElse skipLinesBeginning
    for (i <- 1 to nrLinesToSkip)
      reader.readLine() // Skip line
  }

  private def getAndInitBufferedReaderForCsvFile(): BufferedReader = {
    val reader = getBufferedReaderForCsvFile()
    initBufferedReader(reader)
    reader
  }

  private def getBufferedReaderForCsvFile(): BufferedReader = {
    getBufferedReaderForCsvFile(codecToUse)
  }

  private def getBufferedReaderForCsvFile(codec: Codec): BufferedReader = {
    val inputStream = file.load
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

  override def retrieveTypes(limit: Option[Int] = None): Traversable[(String, Double)] = {
    Seq((classUri, 1.0))
  }

  private def classUri = prefix + file.name

  /**
    * returns the combined path. Depending on the data source the input path may or may not be modified based on the type URI.
    */
  override def combinedPath(typeUri: String, inputPath: Path): Path = inputPath
}

object SeparatorDetector {
  private val separatorList = Seq(',', '\t', ';', '|', '^')
  final val maxColumnsToParseForDetection = 32000
  final val maxCharsPerColumnForDetection = 64000

  def detectSeparatorChar(reader: => java.io.Reader,
                          settings: CsvSettings,
                          maxEntriesToTest: Int): Option[DetectedSeparator] = {
    val separatorCharDist = for (separator <- separatorList) yield {
      // Test which separator has the lowest entropy
      val csvParser = separatorDetectionCsvParser(settings, separator)
      csvParser.beginParsing(reader)
      val fieldCountDist = new MMap[Int, Int]
      var count = 1
      var fields = csvParser.parseNext()
      while (count < maxEntriesToTest && fields.isDefined) {
        val fieldCount = fields.get.length
        fieldCountDist.put(fieldCount, fieldCountDist.getOrElse(fieldCount, 0) + 1)
        fields = csvParser.parseNext()
        count += 1
      }
      csvParser.stopParsing()
      (separator, fieldCountDist.toMap)
    }
    // Filter out
    pickBestSeparator(separatorCharDist.toMap, reader, settings)
  }

  private def separatorDetectionCsvParser(settings: CsvSettings, separator: Char) = {
    new CsvParser(Seq.empty, csvSettingsForDetection(csvSettingsForDetection(settings, separator), separator))
  }

  private def csvSettingsForDetection(settings: CsvSettings, separator: Char) = {
    settings.copy(
      separator = separator,
      maxColumns = Some(math.max(settings.maxColumns.getOrElse(0), maxColumnsToParseForDetection)),
      maxCharsPerColumn = Some(math.max(settings.maxCharsPerColumn.getOrElse(0), maxCharsPerColumnForDetection))
    )
  }

  // For entropy equation, see https://en.wikipedia.org/wiki/Entropy_%28information_theory%29
  def entropy(distribution: Map[Int, Int]): Double = {
    if (distribution.isEmpty) {
      return 0.0
    }
    val overallCount = distribution.values.sum
    if (overallCount == 0) {
      return 0.0
    }
    var sum = 0.0

    for ((_, count) <- distribution if count > 0) {
      val probability = count.toDouble / overallCount
      sum += probability * math.log(probability)
    }
    -sum
  }

  // Filter out separators that don't split most of the input lines, then pick the one with the lowest entropy
  private def pickBestSeparator(separatorDistribution: Map[Char, Map[Int, Int]],
                                reader: => java.io.Reader,
                                csvSettings: CsvSettings): Option[DetectedSeparator] = {
    assert(separatorDistribution.isEmpty || separatorDistribution.forall(d => d._2.nonEmpty && d._2.values.sum > 0))
    // Ignore characters that did not split anything
    val candidates = separatorDistribution filter { case (c, dist) =>
      val oneFieldCount = dist.getOrElse(1, 0)
      val sum = dist.values.sum
      // Separators with too many 1-field lines are filtered out
      oneFieldCount.toDouble / sum < 0.5
    }
    val charEntropy = candidates map { case (c, dist) =>
      (c, entropy(dist))
    }
    pickSeparatorBasedOnEntropy(separatorDistribution, charEntropy, reader, csvSettings)
  }

  // Pick the separator with the lowest entropy of its field count distribution
  private def pickSeparatorBasedOnEntropy(separatorDistribution: Map[Char, Map[Int, Int]],
                                          charEntropy: Map[Char, Double],
                                          reader: => java.io.Reader,
                                          csvSettings: CsvSettings): Option[DetectedSeparator] = {
    val lowestEntropySeparator = charEntropy.toSeq.sortWith(_._2 < _._2).headOption
    // Entropy must be < 0.1, which means that at most 6 out of [[linesForDetection]] lines may have a different number of fields than the majority
    val separator = lowestEntropySeparator filter (_._2 < 0.1) map (_._1)
    separator map { c =>
      val dist = separatorDistribution(c)
      val numberOfFields = dist.toSeq.sortWith(_._2 > _._2).head._1
      val skipLinesAtBeginning = detectSkipLinesBasedOnDetectedSeparator(reader, numberOfFields, c, csvSettings)
      DetectedSeparator(c, numberOfFields, skipLinesAtBeginning)
    }
  }

  private def detectSkipLinesBasedOnDetectedSeparator(reader: => java.io.Reader,
                                                      numberOfFields: Int,
                                                      separator: Char,
                                                      csvSettings: CsvSettings): Int = {
    val parser = new CsvParser(Seq.empty, csvSettingsForDetection(csvSettings, separator))
    parser.beginParsing(reader)
    var counter = 0
    while(! validLineOrEnd(parser.parseNext(), numberOfFields)) {
      counter += 1
    }
    parser.stopParsing()
    counter
  }

  private def validLineOrEnd(fields: Option[Array[String]],
                             numberOfFields: Int): Boolean = {
    fields match {
      case Some(f) =>
        f.length == numberOfFields
      case None =>
        true // Nothing to parse, reached end
    }
  }
}

/**
  * The return value of the separator detection
  *
  * @param separator      the character used for separating fields in CSV
  * @param numberOfFields the detected number of fields when splitting with this separator
  */
case class DetectedSeparator(separator: Char, numberOfFields: Int, skipLinesBeginning: Int)

object CsvSourceHelper {
  lazy val standardCsvParser = new CsvParser(
    Seq.empty,
    CsvSettings(separator = ',', quote = Some('"'))
  )

  def serialize(fields: Traversable[String]): String = {
    fields.map { field =>
      if (field.contains("\"") || field.contains(",")) {
        escapeString(field)
      } else {
        field
      }
    }.mkString(",")
  }

  def parse(str: String): Seq[String] = {
    standardCsvParser.synchronized {
      standardCsvParser.parseLine(str)
    }
  }

  def escapeString(str: String): String = {
    val quoteReplaced = str.replaceAll("\"", "\"\"")
    s"""\"$quoteReplaced\""""
  }
}