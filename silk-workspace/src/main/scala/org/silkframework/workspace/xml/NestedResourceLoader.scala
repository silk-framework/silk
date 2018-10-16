package org.silkframework.workspace.xml

import org.silkframework.runtime.resource.{Resource, ResourceLoader}

class NestedResourceLoader(resources: List[Resource] = Nil, children: Map[String, ResourceLoader] = Map.empty) extends ResourceLoader {

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
