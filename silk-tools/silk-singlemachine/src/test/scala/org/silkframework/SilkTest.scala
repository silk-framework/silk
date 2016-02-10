package org.silkframework

import java.io.File

import org.scalatest.{FlatSpec, Matchers}

import scala.io.Source

class SilkTest extends FlatSpec with Matchers {

  val exampleDir = getClass.getClassLoader.getResource("org/silkframework/example/").getFile
  val linkSpecFile = new File(exampleDir + "linkSpec.xml")
  val outputFile = new File(exampleDir + "links.nt")
  outputFile.delete()

  "Silk" should "execute the example link spec" in {
    Silk.executeFile(linkSpecFile)
    outputFile.exists() should be(true)
    Source.fromFile(outputFile).getLines().size should be (110)
  }

}