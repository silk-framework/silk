package views.editor

/**
  * Generates a unique id for a rule operator to be used in the editor html.
  */
object GenerateId {

  /**
    * Generates a unique id.
    * @param operatorId The id of the rule operator, which is unique inside the rule.
    * @param displayed Whether the rule is displayed, or hidden in the toolbox.
    */
  def apply(operatorId: String, displayed: Boolean): String = {
    if(displayed)
      "operator_" + operatorId
    else
      "toolboxOperator_" + operatorId
  }

}
