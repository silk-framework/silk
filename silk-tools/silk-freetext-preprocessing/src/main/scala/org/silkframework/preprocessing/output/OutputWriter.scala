package org.silkframework.preprocessing.output

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


  def write(entities:Iterable[Entity]){
    val content = for(entity <- entities; property <- entity.properties) yield {
        "<" + entity.uri + "> " + "<" + property.path + "> \"" + property.value + "\" ."
    }
    output.write(content.mkString("\n"))
  }

}
