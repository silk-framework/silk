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

package org.silkframework.dataset.rdf

import org.silkframework.runtime.activity.UserContext

/**
 * Represents a SPARQL endpoint and provides an interface to execute queries on it.
 */
trait SparqlEndpoint {
  /**
    * @return the SPARQL related configuration of this SPARQL endpoint.
    */
  def sparqlParams: SparqlParams

  /**
    *
    * @param sparqlParams the new configuration of the SPARQL endpoint.
    * @return A SPARQL endpoint configured with the new parameters.
    */
  def withSparqlParams(sparqlParams: SparqlParams): SparqlEndpoint

  /**
    * Executes an ask query.
    * ASK queries determine whether or not a query solution exists for the given set of patterns and restrictions.
    * Syntax: ASK WHERE{ ... }
    */
  def ask(query: String)(implicit userContext: UserContext): SparqlAskResult = {
    val selectQuery = query.replaceFirst("(ASK|ask)\\s+", "SELECT * ")
    val askResult = this.select(selectQuery, 1).bindings.use(_.nonEmpty)
    new SparqlAskResult(askResult)
  }

  /**
    * Executes a select query.
    * If the query does not contain a offset or limit, automatic paging is done by issuing multiple queries with a sliding offset.
    *
    */
  def select(query: String, limit: Int = Integer.MAX_VALUE)(implicit userContext: UserContext): SparqlResults

  /**
    * Executes a construct query.
    */
  def construct(query: String)(implicit userContext: UserContext): QuadIterator = {
    throw new UnsupportedOperationException(s"Endpoint type $getClass does not support issuing SPARQL Construct queries")
  }

  /**
    * Executes an update query.
    */
  def update(query: String)(implicit userContext: UserContext): Unit = {
    throw new UnsupportedOperationException(s"Endpoint type $getClass does not support issuing SPARQL/Update queries")
  }
}
