package org.silkframework.dataset

import org.silkframework.dataset.DataSourceCharacteristics.SupportedPathExpressions

/** Characteristics of a data source.
  *
  * @param supportedPathExpressions The characteristics of the supported path expressions.
  */
case class DataSourceCharacteristics(supportedPathExpressions: SupportedPathExpressions = SupportedPathExpressions())

object DataSourceCharacteristics {

  /** Sources that only support plain attributes (i.e., forward paths of length 1 without any filters) */
  def attributesOnly: DataSourceCharacteristics = DataSourceCharacteristics()

  /** The kind of path expressions supported by a data source.
    *
    * @param multiHopPaths  If enabled it is possible to define multi-hop paths (e.g. in RDF, JSON, XML). Else only single-hop
    *                       path are supported.
    * @param backwardPaths  If the data source supports backward paths, i.e. reversing the direction of a property (e.g. in
    *                       RDF, JSON (parent), XML (parent)).
    * @param languageFilter If the data source supports language filters, i.e. is able to filter by different language versions
    *                       of property values (only supported in RDF).
    * @param propertyFilter If the data source supports (single-hop forward) property filters.
    * @param specialPaths   The data source specific paths that are supported by a data source, e.g. row ID in CSV.
    */
  case class SupportedPathExpressions(multiHopPaths: Boolean = false,
                                      backwardPaths: Boolean = false,
                                      languageFilter: Boolean = false,
                                      propertyFilter: Boolean = false,
                                      specialPaths: Seq[SpecialPathInfo] = Seq.empty)

  /**
    * Information about data source specific special paths that have a special semantic.
    *
    * @param value       The path value.
    * @param description Description of the semantics of the special path.
    */
  case class SpecialPathInfo(value: String, description: Option[String])

}

