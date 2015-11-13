package de.fuberlin.wiwiss.silk.plugins.dataset.rdf

/**
 * Sparql parameters.
 *
 * @param uri The URI of the endpoint
 * @param user The login required by the endpoint for authentication
 * @param pageSize The number of solutions to be retrieved per SPARQL query (default: 1000)
 * @param pauseTime The minimum number of milliseconds between two queries
 * @param retryCount The number of retries if a query fails
 * @param retryPause The pause in milliseconds before a query is retried. For each subsequent retry the pause is doubled.
 * @param queryParameters Additional parameters to be appended to every request e.g. &soft-limit=1
 * @param parallel True, if multiple queries should be executed in parallel for faster retrieval.
*/
case class SparqlParams(uri: String = "", user: String = null, password: String = null,
                        graph: String = null, pageSize: Int = 1000, entityList: String = null,
                        pauseTime: Int = 0, retryCount: Int = 3, retryPause: Int = 1000,
                        queryParameters: String = "", parallel: Boolean = true, useOrderBy: Boolean = true) {

  /**
   * The login as option pair of user and password.
   */
  val login = {
    if (user != null) {
      require(password != null, "No password provided for login '" + user + "'. Please set the 'password' parameter.")
      Some((user, password))
    } else {
      None
    }
  }
}