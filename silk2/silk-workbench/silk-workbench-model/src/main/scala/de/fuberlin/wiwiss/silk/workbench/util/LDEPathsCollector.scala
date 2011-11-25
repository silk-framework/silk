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

package de.fuberlin.wiwiss.silk.workbench.util

import de.fuberlin.wiwiss.silk.util.sparql.SparqlEndpoint
import de.fuberlin.wiwiss.silk.util.Uri
import java.util.logging.Logger
import de.fuberlin.wiwiss.silk.entity.{SparqlRestriction, Path, ForwardOperator}

/**
 * Retrieves property paths from the Wiki ontology within the LDE context
 */
object LDEPathsCollector
{
  private val logger = Logger.getLogger(LDEPathsCollector.getClass.getName)

  /**
   * Retrieves a list of properties 
   */
  def apply(endpoint : SparqlEndpoint, restrictions : SparqlRestriction,limit : Option[Int]) : Traversable[(Path, Double)] =
  {
    getAllPaths(endpoint, restrictions, limit.getOrElse(100))
  }

  private def getAllPaths(endpoint : SparqlEndpoint, restrictions : SparqlRestriction, limit : Int) : Traversable[(Path, Double)] =
  {
    val variable = restrictions.toSparql.dropWhile(_ != '?').drop(1).takeWhile(_ != ' ')
    val category = restrictions.toString.split(' ')(2)

    val sparql = QueryFactory.sPropertyPaths(category)

    val results = endpoint.query(sparql, limit).toList
    if(!results.isEmpty)
    {
      for(result <- results if result.contains("p")) yield
      {
        (Path(variable, ForwardOperator(Uri.fromURI(result("p").value)) :: Nil), 1.)
      }
    }
    else
    {
      Traversable.empty
    }
  }
    
}