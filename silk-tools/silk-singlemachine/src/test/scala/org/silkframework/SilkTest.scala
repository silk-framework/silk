package org.silkframework

import java.io.File
import java.net.URLDecoder

import org.scalatest.{FlatSpec, Matchers}

import scala.io.Source

class SilkTest extends FlatSpec with Matchers {

  val exampleDir = URLDecoder.decode(getClass.getClassLoader.getResource("org/silkframework/example/").getFile, "UTF-8")
  val linkSpecFile = new File(exampleDir, "linkSpec.xml")
  val outputFile = new File(exampleDir, "links.nt")
  outputFile.delete()

  "Silk" should "execute the example link spec" in {
    Silk.executeFile(linkSpecFile)
    outputFile.exists() should be(true)
    Source.fromFile(outputFile).getLines().size should be (110)
  }

}