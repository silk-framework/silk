package org.silkframework.runtime.plugin

case class TransformExampleValue(parameters: Map[String, String], input: Seq[Seq[String]], output: Seq[String], throwsException: String) {

  def formatted: String = {
    s"Returns ${format(output)} for parameters ${format(parameters)} and input values ${format(input.map(format))}."
  }

  private def format(traversable: Traversable[_]): String = {
    traversable.mkString("[", ", ", "]")
  }

}

object TransformExampleValue {

  def retrieve(transformer: Class[_]): Seq[TransformExampleValue] = {
    val transformExamples = transformer.getAnnotationsByType(classOf[TransformExample])
    for(example <- transformExamples) yield {
      TransformExampleValue(
        parameters = retrieveParameters(example),
        input = Seq(example.input1(), example.input2(), example.input3(), example.input4(), example.input5()).map(_.toList).filter(_.nonEmpty),
        output = example.output().toList,
        throwsException = example.throwsException()
      )
    }
  }

  private def retrieveParameters(transformExample: TransformExample): Map[String, String] = {
    assert(transformExample.parameters().length % 2 == 0, "TransformExample.parameters must have an even number of values")
    transformExample.parameters().grouped(2).map(group => (group(0), group(1))).toMap
  }


}