/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.fuberlin.wiwiss.silk.config

import de.fuberlin.wiwiss.silk.entity.rdf.SparqlRestriction
import de.fuberlin.wiwiss.silk.util.{DPair, Identifier}

import scala.xml.Node

/**
 * Defines a dataset.
 *
 * @param datasetId The id of the dataset
 * @param restriction Restricts this dataset to specific resources.
 */
case class DatasetSelection(datasetId: Identifier, restriction: SparqlRestriction) {

  def variable = restriction.variable

  /**
   * Serializes this dataset specification as XML.
   *
   * @param asSource If true, this dataset will be serialized as a source dataset. If false it will be serialize as target dataset.
   */
  def toXML(asSource: Boolean) = {
    if (asSource) {
      <SourceDataset dataSource={datasetId} var={restriction.variable}>
        <RestrictTo>
          {restriction.toSparql}
        </RestrictTo>
      </SourceDataset>
    }
    else {
      <TargetDataset dataSource={datasetId} var={restriction.variable}>
        <RestrictTo>
          {restriction.toSparql}
        </RestrictTo>
      </TargetDataset>
    }
  }
}

object DatasetSelection {
  /**
   * Creates a DatasetSpecification from XML.
   */
  def fromXML(node: Node)(implicit prefixes: Prefixes = Prefixes.empty): DatasetSelection = {
    val variable = (node \ "@var").text

    DatasetSelection(
      datasetId = (node \ "@dataSource").text,
      restriction = SparqlRestriction.fromSparql(variable, (node \ "RestrictTo").text.trim)
    )
  }

  def empty = DatasetSelection("EmptyDatasetSelection", SparqlRestriction.empty)

  def emptyPair =
    DPair(
      DatasetSelection("SourceDatasetSelection", SparqlRestriction.empty),
      DatasetSelection("TargetDatasetSelection", SparqlRestriction.empty)
    )
}
