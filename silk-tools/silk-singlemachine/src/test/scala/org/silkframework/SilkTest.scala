package org.silkframework

import java.io.File
import org.scalatest.{FlatSpec, Matchers}
import scala.io.Source

class SilkTest extends FlatSpec with Matchers {

  val linkSpecFile = new File(getClass.getClassLoader.getResource("org/silkframework/example/linkSpec.xml").getFile)
  val outputFile = new File(getClass.getClassLoader.getResource("org/silkframework/example/links.nt").getFile)

  "Silk" should "execute the example link spec" in {
    Silk.executeFile(linkSpecFile)
    Source.fromFile(outputFile).getLines().size should be (110)
  }

}