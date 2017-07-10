package org.silkframework

import java.io.File
import java.net.URLDecoder

import org.scalatest.{FlatSpec, Matchers}
import org.silkframework.util.ConfigTestTrait

import scala.io.Source

class SilkTest extends FlatSpec with Matchers {

  val exampleDir = URLDecoder.decode(getClass.getClassLoader.getResource("org/silkframework/example/").getFile, "UTF-8")
  val linkSpecFile = new File(exampleDir, "linkSpec.xml")
  val outputFile = new File(exampleDir, "links.nt")
  val projectFile = new File(exampleDir, "project.zip")
  outputFile.delete()

  "Silk" should "execute the example link spec" in {
    Silk.executeFile(linkSpecFile)
    outputFile.exists() should be(true)
    Source.fromFile(outputFile).getLines().size should be (110)
  }

  "Silk" should "execute workflows" in {
    val project = Silk.executeProject(projectFile, "workflow")
    val output = project.resources.get("output.nt")
    output.loadAsString.split("\n").length should be (110)
    // Clean up
    project.resources.delete("output.nt")
    project.resources.delete("source.nt")
    project.resources.delete("target.nt")
  }

}