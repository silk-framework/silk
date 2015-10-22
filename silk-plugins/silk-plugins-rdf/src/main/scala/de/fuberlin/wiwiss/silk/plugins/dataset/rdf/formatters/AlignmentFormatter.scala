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

package de.fuberlin.wiwiss.silk.plugins.dataset.rdf.formatters

import de.fuberlin.wiwiss.silk.entity.Link
import de.fuberlin.wiwiss.silk.runtime.plugin.Plugin

/**
 * Writes the alignment format specified at http://alignapi.gforge.inria.fr/format.html.
 */
class AlignmentFormatter() extends XMLFormatter {
  override def header = {
    "<?xml version='1.0' encoding='utf-8' standalone='no'?>\n" +
      "<rdf:RDF xmlns='http://knowledgeweb.semanticweb.org/heterogeneity/alignment#'\n" +
      "    xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#'\n" +
      "    xmlns:xsd='http://www.w3.org/2001/XMLSchema#'\n" +
      "    xmlns:align='http://knowledgeweb.semanticweb.org/heterogeneity/alignment#'>\n" +
      "<Alignment>\n"
  }

  override def footer = {
    "</Alignment>\n" +
      "</rdf:RDF>\n"
  }

  override def formatXML(link: Link, predicateUri: String) = {
    <map>
      <Cell>
          <entity1 rdf:resource={link.source}/>
          <entity2 rdf:resource={link.target}/>
        <relation>{
          if (predicateUri == "http://www.w3.org/2002/07/owl#sameAs") "=" else predicateUri
        }</relation>
        <measure rdf:datatype="http://www.w3.org/2001/XMLSchema#float">{
          link.confidence.getOrElse(0.0).toString
        }</measure>
      </Cell>
    </map>
  }
}