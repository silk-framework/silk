package de.fuberlin.wiwiss.silk.preprocessing.output


import de.fuberlin.wiwiss.silk.preprocessing.entity.{Entity,Property}
import scalax.io._

/**
 * Output writer
 * Writes triples to a file
 *
 * @param id The output id
 * @param file The path to the file
 * @param format The format of the data
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
