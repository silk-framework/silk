package views.editor

object GenerateId {

  def apply(elementId: String, displayed: Boolean): String = {
    if(displayed)
      "operator_" + elementId
    else
      "toolboxOperator_" + elementId
  }

}
