package org.silkframework.dataset

/**
  * Categorizes paths as either object or value paths. If there is at least one literal value found, it will
  * become a value path.
  */
case class PathCategorizerValueAnalyzerFactory() extends ValueAnalyzerFactory[PathType] {
  override def analyzer(): PathCategorizerValueAnalyzer = PathCategorizerValueAnalyzer()
}

case class PathCategorizerValueAnalyzer() extends ValueAnalyzer[PathType] {
  private var pathType: PathType = ObjectPath

  override def result: Option[PathType] = Some(pathType)

  override def update(value: String): Unit = {
    /* If a lexical value exists for path, make it a value path. For XML a path might be an object path and value path –
       an XML element with Text node as child and attributes.
     */
    pathType = ValuePath
  }
}

sealed trait PathType

case object ObjectPath extends PathType

case object ValuePath extends PathType