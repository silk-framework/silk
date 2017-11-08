package org.silkframework.plugins.dataset.csv

import scala.collection.mutable.{HashMap => MMap}

/**
  * Separator detector for CSV files
  */
object CsvSeparatorDetector {
  private val separatorList = Seq(',', '\t', ';', '|', '^', '#')
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
    if(separatorDistribution.isEmpty || separatorDistribution.forall(d => d._2.nonEmpty && d._2.values.sum > 0)) {
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
    } else {
      None
    }
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