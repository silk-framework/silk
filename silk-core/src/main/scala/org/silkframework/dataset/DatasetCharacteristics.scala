package org.silkframework.dataset

import org.silkframework.dataset.DatasetCharacteristics.SupportedPathExpressions
import org.silkframework.entity.paths.UntypedPath

/** Characteristics of a data source.
  *
  * @param supportedPathExpressions The characteristics of the supported path expressions.
  * @param supportsMultipleTables If true, the dataset supports reading and writing multiple tables, which includes hierarchical datasets (XML, JSON, etc.).
  *                               If false, the dataset only supports a single table (e.g., CSV).
  * @param readOnly               If false, this dataset can be written. If true, this is a read-only dataset (possibly because writing has not been implemented).
  * @param supportsMultipleWrites If true, this dataset can be written to multiple times without clearing it in-between (e.g., RDF)
 *                                If false, this dataset will be overwritten on each write (e.g., JSON).
  * @param typedEntities          If true, each entity needs a type property value (e.g. RDF).
  *                               If false, the type is already given and not needed on a per-entity basis (e.g. relational databases where each table only contains entities of one type).
  */
case class DatasetCharacteristics(supportedPathExpressions: SupportedPathExpressions = SupportedPathExpressions(),
                                  supportsMultipleTables: Boolean = true,
                                  readOnly: Boolean = false,
                                  supportsMultipleWrites: Boolean = false,
                                  typedEntities: Boolean = false)

object DatasetCharacteristics {

  /** Sources that only support plain attributes (i.e., forward paths of length 1 without any filters) */
  def attributesOnly(supportsMultipleTables: Boolean = false, readOnly: Boolean = false, supportsMultipleWrites: Boolean = false): DatasetCharacteristics = {
    DatasetCharacteristics(supportsMultipleTables = supportsMultipleTables, readOnly = readOnly, supportsMultipleWrites = supportsMultipleWrites)
  }

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
  case class SpecialPathInfo(value: String, description: Option[String], suggestedFor: SuggestedForEnum.Value = SuggestedForEnum.All) {
    lazy val path: UntypedPath = UntypedPath(value)
  }

  object SuggestedForEnum extends Enumeration {
    type SuggestedForEnum = Value

    val All, ValuePathOnly, ObjectPathOnly = Value
  }

  /**
   * Special paths that are typically supported by datasets.
   */
  object SpecialPaths {
    val IDX: SpecialPathInfo = SpecialPathInfo("#idx", Some("Returns the index of the entity."), SuggestedForEnum.All)
    val LINE: SpecialPathInfo = SpecialPathInfo("#line", Some("Line number of the selected value."), SuggestedForEnum.All)
    val COLUMN: SpecialPathInfo = SpecialPathInfo("#column", Some("Column position of the selected value."), SuggestedForEnum.All)
  }
}

