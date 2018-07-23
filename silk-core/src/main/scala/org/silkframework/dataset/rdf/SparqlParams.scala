package org.silkframework.dataset.rdf

/**
 * Sparql parameters.
 *
 * @param uri The URI of the endpoint
 * @param user The login required by the endpoint for authentication
 * @param graph The URI of a named graph
 * @param pageSize The number of solutions to be retrieved per SPARQL query (default: 1000)
 * @param entityList TODO @Robert Isele
 * @param pauseTime The minimum number of milliseconds between two queries
 * @param retryCount The number of retries if a query fails
 * @param retryPause The pause in milliseconds before a query is retried. For each subsequent retry the pause is doubled.
 * @param queryParameters Additional parameters to be appended to every request e.g. &soft-limit=1
 * @param strategy Strategy used for retrieving entities.
 * @param useOrderBy Enforce ordering by using ORDER BY in SPARQL select queries. May slow down reading.
*/
case class SparqlParams(
   uri: String = "",
  user: String = null,
  password: String = null,
  graph: Option[String] = None,
  pageSize: Int = 1000,
  entityList: String = null,
  pauseTime: Int = 0,
  retryCount: Int = 3,
  retryPause: Int = 1000,
  queryParameters: String = "",
  strategy: EntityRetrieverStrategy = EntityRetrieverStrategy.parallel,
  useOrderBy: Boolean = true
 ) {

  /**
   * The login as option pair of user and password.
   */
  val login: Option[(String, String)] = {
    if (user != null && user != "") {
      require(password != null, "No password provided for login '" + user + "'. Please set the 'password' parameter.")
      Some((user, password))
    } else {
      None
    }
  }

  /** Restrict entities to following URIs. If empty, then no restriction should apply. */
  def entityRestriction: Seq[String] = SparqlParams.splitEntityList(entityList)
}

object SparqlParams {
  /** Splits entities on whitespace */
  def splitEntityList(entityList: String): Seq[String] = {
    lazy val trimmedEntityList = entityList.trim
    if(entityList == null || trimmedEntityList == "") {
      Seq()
    } else {
      trimmedEntityList.split("\\s+")
    }
  }
}