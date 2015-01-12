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

import de.fuberlin.wiwiss.silk.entity.SparqlRestriction
import de.fuberlin.wiwiss.silk.util.Identifier

import scala.xml.Node

/**
 * Defines a dataset.
 *
 * @param datasetId The id of the dataset
 * @param variable Each data item will be bound to this variable.
 * @param restriction Restricts this dataset to specific resources.
 */
case class DatasetSelection(datasetId: Identifier, variable: String, restriction: SparqlRestriction) {
  require(!variable.isEmpty, "Variable must be non-empty")

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
  def fromXML(node: Node)(implicit prefixes: Prefixes): DatasetSelection = {
    val variable = (node \ "@var").text

    DatasetSelection(
      datasetId = (node \ "@dataSource").text,
      variable = variable,
      restriction = SparqlRestriction.fromSparql(variable, (node \ "RestrictTo").text.trim)
    )
  }

  def empty = DatasetSelection(Identifier.random, "x", SparqlRestriction.empty)
}
