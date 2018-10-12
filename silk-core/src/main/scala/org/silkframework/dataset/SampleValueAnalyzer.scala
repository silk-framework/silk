package org.silkframework.dataset

import org.silkframework.entity.{ForwardOperator, Path}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * Executes the analyzer as soon as enough values have been gathered. Does not add more values for already analyzed paths.
  *
  * @param sampleLimit     The value sample limit. The maximum number of values when the analyzer should be run. Only
  *                        this many values are stored at most.
  * @param analyzerFactory The analyzer factory to do the analysis.
  */
case class SampleValueAnalyzer[T](sampleLimit: Int,
                                  analyzerFactory: ValueAnalyzerFactory[T]) {
  // Stores the path values
  private val pathValues = mutable.HashMap[List[String], mutable.ArrayBuffer[String]]()
  // Stores analyzer results
  private val analyzerResults = mutable.HashMap[List[String], Option[T]]()

  /** Add a value for a path. */
  def addValue(path: List[String], value: String): Unit = {
    if(!analyzerResults.contains(path)) {
      val values = pathValues.getOrElseUpdate(path, mutable.ArrayBuffer[String]())
      values += value
      if(values.size >= sampleLimit) {
        // Calculate analyzer result if enough sample data collected
        analyzePath(path, values)
      }
    }
  }

  private def analyzePath(path: List[String], values: ArrayBuffer[String]) = {
    val analyzer = analyzerFactory.analyzer()
    analyzer.update(values)
    analyzerResults.put(path, analyzer.result)
    pathValues.remove(path)
  }

  /** Get the analyzer results for all paths having at least one sample value. */
  def result: Map[List[String], Option[T]] = {
    if(pathValues.nonEmpty) { // If analyzer results are still missing, calculate them now
      for(path <- pathValues.keys) {
        val values = pathValues(path)
        analyzePath(path, values)
      }
    }
    analyzerResults.toMap
  }

  def clear(): Unit = {
    pathValues.clear()
    analyzerResults.clear()
  }
}

trait SampleValueAnalyzerExtractionSource extends SchemaExtractionSource {
  this: DataSource =>

  /**
    * Collects all paths from the JSON.
    *
    * @param limit         The number of paths after which no more paths should be collected.
    * @param collectValues A function to collect values of a path.
    * @return all collected paths
    */
  def collectPaths(limit: Int, collectValues: (List[String], String) => Unit = (_, _) => {}): Seq[List[String]]

  override def extractSchema[T](analyzerFactory: ValueAnalyzerFactory[T],
                                pathLimit: Int,
                                sampleLimit: Option[Int],
                                progress: (Double) => Unit = (_) => {}): ExtractedSchema[T] = {
    val sampleValueAnalyzer = SampleValueAnalyzer(sampleLimit.getOrElse(DEFAULT_VALUE_SAMPLE_LIMIT), analyzerFactory)
    val collectValues: (List[String], String) => Unit = (path, value) => { sampleValueAnalyzer.addValue(path, value) }
    val allPaths = collectPaths(pathLimit, collectValues)
    progress(0.1)
    val pathAnalyzerResults = sampleValueAnalyzer.result.map { case (k, v) => (k.reverse, v)} // Analyzed paths are still reversed
    progress(0.7)
    sampleValueAnalyzer.clear()
    val pathAnalyzerDerivedTypes = pathAnalyzerResults.keys.map(_.dropRight(1)).toSet
    val types = allPaths.filter(p => !pathAnalyzerResults.contains(p) || pathAnalyzerDerivedTypes.contains(p))
    // Map from types (path) to its value paths
    val typeMap: Map[List[String], ArrayBuffer[List[String]]] = types.map(t => (t, ArrayBuffer[List[String]]())).toMap
    for(path <- allPaths) {
      if(pathAnalyzerResults.contains(path)) {
        val typePath = path.dropRight(1)
        val paths = typeMap(typePath)
        paths.append(path)
      }
    }
    progress(0.8)
    val schemaClasses = for(typ <- types) yield {
      val extractedSchemaPaths = for(path <- typeMap(typ)) yield {
        val analyzerResult = pathAnalyzerResults(path)
        ExtractedSchemaProperty(Path(path.last), analyzerResult)
      }
      ExtractedSchemaClass(Path(typ.map(ForwardOperator(_))).normalizedSerialization, extractedSchemaPaths)
    }
    progress(1.0)
    ExtractedSchema(schemaClasses)
  }
}