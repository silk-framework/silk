package org.silkframework.runtime.resource

/**
  * A resource loader whose resources are provided in the constructor.
  * Can be used to combine multiple resources loaders or to nest a resource loader under a specific path.
  *
  * @param resources All resources at the root.
  * @param children All child resource loaders.
  */
class CombinedResourceLoader(resources: List[Resource] = Nil, children: Map[String, ResourceLoader] = Map.empty) extends ResourceLoader {

  val basePath: String = ""

  override def list: List[String] = resources.map(_.name)

  override def get(name: String, mustExist: Boolean): Resource = {
    resources.find(_.name == name) match {
      case Some(resource) => resource
      case None =>
        throw new NoSuchElementException(s"No resource $name found")
    }
  }

  override def listChildren: List[String] = children.keys.toList

  override def child(name: String): ResourceLoader = {
    children.get(name) match {
      case Some(resourceLoader) => resourceLoader
      case None =>
        throw new NoSuchElementException(s"No child $name found")
    }
  }

  override def parent: Option[ResourceLoader] = None
}
