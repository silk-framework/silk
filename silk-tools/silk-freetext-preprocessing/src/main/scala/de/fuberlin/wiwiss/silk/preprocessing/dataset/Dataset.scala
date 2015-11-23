package org.silkframework.preprocessing.dataset

import org.silkframework.preprocessing.entity.{Property, Entity}
import scala.xml.Node
import org.silkframework.preprocessing.util.jena.JenaSource

/**
 * Represents a dataset of entities
 *
 * @param id The dataset id (same with the source)
 * @param entities The data
 */
class Dataset(id: String, entities:Traversable[Entity]){

  private val paths = loadPaths


  def entitySet = entities

  def pathSet = paths

  def filter(path:String):Traversable[Entity] = {
    for(entity <- entities) yield {
      new Entity(entity.uri, entity.properties.filter(p => solvePath(p.path, path)))
    }
  }

  def solvePath(propertyPath:String, path:String):Boolean = {
    if(path.r.findAllIn(propertyPath).length >= 1) true
    else false
  }

  def loadPaths = {
    entities.flatMap(entity => entity.properties).groupBy(p => p.path).keySet
  }

  def findPath(path:String):String = {
    paths.filter(p => solvePath(p, path)).headOption.getOrElse(path)
  }

}
