package de.fuberlin.wiwiss.silk.plugins.dataset.csv

import java.io.{BufferedReader, InputStreamReader}
import java.net.URLEncoder
import java.util.logging.{Level, Logger}
import java.util.regex.Pattern

import de.fuberlin.wiwiss.silk.dataset.DataSource
import de.fuberlin.wiwiss.silk.entity._
import de.fuberlin.wiwiss.silk.entity.rdf.{SparqlEntitySchema, SparqlRestriction}
import de.fuberlin.wiwiss.silk.runtime.resource.Resource
import de.fuberlin.wiwiss.silk.util.Uri

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
                detectSeparator: Boolean = false) extends DataSource {

  private val logger = Logger.getLogger(getClass.getName)

  private lazy val propertyList: Seq[String] = {
    val parser = new CsvParser(Seq.empty, csvSettings)
    if (!properties.trim.isEmpty)
      parser.parseLine(properties)
    else {
      val source = getAndInitBufferedReaderForCsvFile()
      val firstLine = source.readLine()
      source.close()
      parser.parseLine(firstLine).map(s => URLEncoder.encode(s, "UTF8"))
    }
  }
  
  lazy val csvSettings: CsvSettings = {
    var csvSettings = settings
    if(detectSeparator) {
      csvSettings = csvSettings.copy(separator = detectSeparatorChar() getOrElse settings.separator)
    }
    csvSettings
  }

  // automatically detect the separator, returns None if confidence is too low
  private def detectSeparatorChar(): Option[Char] = {
    val source = getAndInitBufferedReaderForCsvFile()
    try {
      val inputLines = (for (i <- 1 to 100)
        yield source.readLine()) filter (_ != null)
      SeparatorDetector.detectSeparatorCharInLines(inputLines, settings)
    } finally {
      source.close()
      None
    }
  }

  override def toString = file.toString

  override def retrieveSparqlPaths(restriction: SparqlRestriction, depth: Int, limit: Option[Int]): Traversable[(Path, Double)] = {
    for (property <- propertyList) yield {
      (Path(restriction.variable, ForwardOperator(prefix + property) :: Nil), 1.0)
    }
  }

  override def retrieveSparqlEntities(entityDesc: SparqlEntitySchema, entities: Seq[String] = Seq.empty): Traversable[Entity] = {

    logger.log(Level.FINE, "Retrieving data from CSV.")

    // Retrieve the indices of the request paths
    val indices =
      for (path <- entityDesc.paths) yield {
        val property = path.operators.head.asInstanceOf[ForwardOperator].property.uri.stripPrefix(prefix)
        val propertyIndex = propertyList.indexOf(property)
        if (propertyIndex == -1)
          throw new Exception("Property " + path.toString + " not found in CSV")
        propertyIndex
      }

    // Return new Traversable that generates an entity for each line
    new Traversable[Entity] {
      def foreach[U](f: Entity => U) {

        lazy val reader = getAndInitBufferedReaderForCsvFile
        val parser = new CsvParser(Seq.empty, csvSettings) // Here we could only load the required indices as a performance improvement

        // Compile the line regex.
        val regex: Pattern = if (!regexFilter.isEmpty) regexFilter.r.pattern else null

        try {
          // Iterate through all lines of the source file. If a *regexFilter* has been set, then use it to filter
          // the rows.
          var line = reader.readLine()
          var index = 0
          while(line != null) {
            if(!(properties.trim.isEmpty && 0 == index) && (regexFilter.isEmpty || regex.matcher(line).matches())) {
              logger.log(Level.FINER, s"Retrieving data from CSV [ line number :: ${index + 1} ].")

              //Split the line into values
              val allValues = parser.parseLine(line)
              if(allValues != null) {
                if(propertyList.size <= allValues.size) {

                  //Extract requested values
                  val values = indices.map(allValues(_))

                  // The default URI pattern is to use the prefix and the line number.
                  // However the user can specify a different URI pattern (in the *uri* property), which is then used to
                  // build the entity URI. An example of such pattern is 'urn:zyx:{id}' where *id* is a name of a property
                  // as defined in the *properties* field.
                  val entityURI = if (uri.isEmpty)
                    prefix + (index + 1)
                  else
                    "\\{([^\\}]+)\\}".r.replaceAllIn(uri, m => {
                      val propName = m.group(1)

                      assert(propertyList.contains(propName))
                      val value = allValues(propertyList.indexOf(propName))
                      URLEncoder.encode(value, "UTF-8")
                    })

                  //Build entity
                  if (entities.isEmpty || entities.contains(entityURI)) {
                    val entityValues = csvSettings.arraySeparator match {
                      case None =>
                        values.map(v => if (v != null) Set(v) else Set.empty[String])
                      case Some(c) =>
                        values.map(v => if (v != null) v.split(c.toString, -1).toSet else Set.empty[String])
                    }

                    f(new Entity(
                      uri = entityURI,
                      values = entityValues,
                      desc = entityDesc
                    ))
                  }
                } else {
                  // Bad line
                  if(! ignoreBadLines) {
                    assert(propertyList.size <= allValues.size, s"Invalid line ${index + 1}: '$line' in resource '${file.name}' with ${allValues.size} elements. Expected number of elements ${propertyList.size}.")
                  }
                }
              }
            }
            index += 1
            line = reader.readLine()
          }
        } finally {
          reader.close()
        }
      }
    }
  }

  // Skip lines that are not part of the CSV file, headers may be included
  private def initBufferedReader(reader: BufferedReader): Unit = {
    for (i <- 1 to skipLinesBeginning)
      reader.readLine() // Skip line
  }

  private def getAndInitBufferedReaderForCsvFile(): BufferedReader = {
    val inputStream = file.load
    val reader = new BufferedReader(new InputStreamReader(inputStream, codec.decoder))
    initBufferedReader(reader)
    reader
  }

  override def retrieveTypes(limit: Option[Int] = None): Traversable[(String, Double)] = {
    Seq((classUri, 1.0))
  }

  override def retrievePaths(t: Uri, depth: Int = 1, limit: Option[Int] = None): IndexedSeq[Path] = {
    if(classUri == t.uri) {
      val props = for (property <- propertyList) yield {
        Path(prefix + property)
      }
      props.toIndexedSeq
    } else {
      IndexedSeq.empty[Path]
    }
  }

  override def retrieve(entitySchema: EntitySchema, limitOpt: Option[Int] = None): Traversable[Entity] = {
    if(entitySchema.filter.operator.isDefined) {
      ??? // TODO: Implement Restriction handling!
    }
    val entities = retrieveSparqlEntities(SparqlEntitySchema(paths = entitySchema.paths))
    limitOpt match {
      case Some(limit) =>
        entities.take(limit)
      case None =>
        entities
    }
  }

  private def classUri = prefix + "CsvTable"
}

object SeparatorDetector {
  private val separatorList = Seq(',', '\t', ';', '|', '^')

  def detectSeparatorCharInLines(inputLines: Seq[String], settings: CsvSettings): Option[Char] = {
    val separatorCharDist = for (separator <- separatorList) yield {
      // Test which separator has the lowest entropy
      val csvParser = new CsvParser(Seq.empty, settings.copy(separator = separator))
      val fieldCountDist = new MMap[Int, Int]
      for (line <- inputLines) {
        val fieldCount = csvParser.parseLine(line).size
        fieldCountDist.put(fieldCount, fieldCountDist.getOrElse(fieldCount, 0) + 1)
      }
      (separator, fieldCountDist.toMap)
    }
    pickBestSeparator(separatorCharDist.toMap)
  }

  // For entropy equation, see https://en.wikipedia.org/wiki/Entropy_%28information_theory%29
  def entropy(distribution: Map[Int, Int]): Double = {
    if(distribution.size == 0)
      return 0.0
    val overallCount = distribution.values.sum
    if(overallCount == 0)
      return 0.0
    var sum = 0.0

    for((_, count) <- distribution if count > 0) {
      val probability = count.toDouble / overallCount
      sum += probability * math.log(probability)
    }
    - sum
  }

  // Filter out separators that don't split most of the input lines, then pick the one with the lowest entropy
  private def pickBestSeparator(separatorDistribution: Map[Char, Map[Int, Int]]): Option[Char] = {
    assert(separatorDistribution.forall(d => d._2.size > 0 && d._2.values.sum > 0))
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
    val lowestEntropySeparator = charEntropy.toSeq.sortWith(_._2 < _._2).headOption
    // Entropy must be < 0.1, which means that at most 6 out of 100 lines may have a different number of fields than the majority
    lowestEntropySeparator filter (_._2 < 0.1) map (_._1)
  }
}