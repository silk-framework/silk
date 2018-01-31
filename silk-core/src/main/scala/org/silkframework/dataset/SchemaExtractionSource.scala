package org.silkframework.dataset

import org.silkframework.entity.Path

/**
  * A data source extension for quick schema extraction.
  */
trait SchemaExtractionSource {
  this: DataSource =>

  final val DEFAULT_VALUE_SAMPLE_LIMIT = 20

  /** Extract the schema of the data source quickly.
    * This should be a much quicker method than using retrieveTypes and subsequently retrievePaths on every class,
    * which would need #types passes over the underlying resource for some datasets.
    *
    * @param analyzerFactory The analyzers that should be executed against the sample values of the value paths.
    * @param pathLimit       The overall max. number of schema elements (classes and paths) that should be extracted
    * @param sampleLimit     If defined then only this many values will be analyzed with the given analyzer before
    *                        calculating the result.
    * @param progressFN      A function that takes a double between 0.0 and 1.0 where the progress can be indicated to
    *                        the caller, >= 1.0 meaning the extraction has finished.
    * */
  def extractSchema[T](analyzerFactory: ValueAnalyzerFactory[T],
                       pathLimit: Int,
                       sampleLimit: Option[Int],
                       progressFN: (Double) => Unit = (_) => {}): ExtractedSchema[T]
}

case class ExtractedSchema[T](classes: Seq[ExtractedSchemaClass[T]])

/**
  * The extracted schema class, e.g. a RDF class or a JSON/XML base path
  *
  * @param sourceType The class URI or path, something that the corresponding dataset understands as type.
  * @param properties The direct properties connected to this schema class.
  */
case class ExtractedSchemaClass[T](sourceType: String, properties: Seq[ExtractedSchemaProperty[T]])

/**
  * The extracted schema property, e.g. a RDF property, an XML/JSON path.
  * @param path           A Silk path expression, usually a forward or even backward path
  * @param valueAnalysis  A caller defined analysis of the values found if this is a value path, e.g. profiling information.
  * @tparam T             The type of the called defined analysis.
  */
case class ExtractedSchemaProperty[T](path: Path, valueAnalysis: Option[T])

trait ValueAnalyzer[T] {
  /** The result of the analyzed values. None if no meaningful result can be returned. */
  def result: Option[T]

  /** Update the analyzer */
  def update(value: String): Unit

  def update(values: Traversable[String]): Unit = values foreach update
}

trait ValueAnalyzerFactory[T] {
  /** Returns a fresh analyzer that should be used on one set of values. */
  def analyzer(): ValueAnalyzer[T]
}