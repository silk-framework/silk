package models.linking

/**
  * User feedback about a link candidate.
  */
object LinkCandidateDecision {

  /**
    * Confirmation of a link candidate.
    */
  val positive = "positive"

  /**
   * Rejection of a link candidate.
   */
  val negative = "negative"

  /**
    * No decision.
    */
  val pass = "pass"

}
