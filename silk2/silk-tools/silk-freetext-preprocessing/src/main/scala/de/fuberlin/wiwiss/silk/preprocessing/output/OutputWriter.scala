package de.fuberlin.wiwiss.silk.preprocessing.output


import de.fuberlin.wiwiss.silk.preprocessing.entity.{Entity,Property}
import scalax.io._

/**
 * Created by Petar on 31/01/14.
 */

case class OutputWriter(id:String,
                        file:String,
                        format:String) {

  val output:Output = Resource.fromFile(file)


  def write(entities:Traversable[Entity]){
    val content = for(entity <- entities; property <- entity.properties) yield {
        "<" + entity.uri + "> " + "<" + property.path + "> \"" + property.value + "\" ."
    }
    output.write(content.mkString("\n"))
  }

}
