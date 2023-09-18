package org.silkframework.util

/**
  * Utility function for scala reflection
  */
object ScalaReflectUtils {

  /**
    * tests if the implementation of a given symbol is of type def (and not val)
    *
    * @param symbolName - name of the symbol
    * @param clazz      - the class for which to check
    * @return
    */
  def implementedAsDef(symbolName: String, clazz: Class[_]): Boolean = {
    clazz.getMethods.exists(_.getName == symbolName)
  }
}
