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

package org.silkframework.config

import org.silkframework.entity.{Path, Restriction}
import org.silkframework.entity.rdf.SparqlRestrictionParser
import org.silkframework.runtime.serialization.ValidationException
import org.silkframework.util.{Uri, DPair, Identifier}

import scala.xml.Node

/**
 * Defines a dataset.
 *
 * @param datasetId The id of the dataset
 * @param restriction Restricts this dataset to specific resources.
 */
case class DatasetSelection(datasetId: Identifier, typeUri: Uri, restriction: Restriction = Restriction.empty) {

  /**
   * Serializes this dataset specification as XML.
   *
   * @param asSource If true, this dataset will be serialized as a source dataset. If false it will be serialize as target dataset.
   */
  def toXML(asSource: Boolean) = {
    if (asSource) {
      <SourceDataset dataSource={datasetId} var="a" typeUri={typeUri.uri}>
        <RestrictTo>
          {restriction.serialize}
        </RestrictTo>
      </SourceDataset>
    }
    else {
      <TargetDataset dataSource={datasetId} var="b" typeUri={typeUri.uri}>
        <RestrictTo>
          {restriction.serialize}
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

    var restrictionText = (node \ "RestrictTo").text.trim
    // Currently the entity is always expected to be referenced with the variable ?a. Older versions also allowed ?b, so we still support it.
    restrictionText = restrictionText.replace("?b", "?a")

    var typeUri = (node \ "@typeUri").text

    // If the type Uri is not defined, try to parse if from the SPARQL restriction
    if(typeUri.isEmpty) {
      try {
        val sourceRestriction = new SparqlRestrictionParser().apply(restrictionText)
        sourceRestriction.operator match {
          case Some(Restriction.Condition(path, uri)) if path.propertyUri.contains("http://www.w3.org/1999/02/22-rdf-syntax-ns#type") =>
            restrictionText = ""
            typeUri = uri
          case _ =>
        }
      } catch {
        case _: ValidationException =>
      }
    }

    DatasetSelection(
      datasetId = (node \ "@dataSource").text,
      typeUri = Uri(typeUri),
      restriction = Restriction.parse(restrictionText)
    )
  }

  def empty = DatasetSelection("EmptyDatasetSelection", Uri(""), Restriction.empty)

  def emptyPair =
    DPair(
      DatasetSelection("SourceDatasetSelection", Uri(""), Restriction.empty),
      DatasetSelection("TargetDatasetSelection", Uri(""), Restriction.empty)
    )
}
