package org.silkframework.plugins.dataset.xml

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.entity.EntitySchema
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.runtime.activity.TestUserContextTrait
import org.silkframework.runtime.plugin.MultilineStringParameter
import org.silkframework.runtime.resource.{ClasspathResource, InMemoryResourceManager, ReadOnlyResource}
import org.silkframework.runtime.validation.ValidationException

import scala.util.Try

class XmlDatasetTest extends FlatSpec with MustMatchers with TestUserContextTrait {
  behavior of "XML dataset"

  it should "only accept valid output templates" in {
    // Should not throw exception
    testOutputTemplate("<?Root?>")
    testOutputTemplate("<Root><?Entity?></Root>")
    // Should throw exception
    intercept[ValidationException] {
      testOutputTemplate("no XML element")
    }
    intercept[ValidationException] {
      testOutputTemplate("<Root><?Entity?><?Entity2?></Root>")
    }
    intercept[ValidationException] {
      testOutputTemplate("<Root><?Entity?></Root2>")
    }
  }

  private def zipDataset(fileRegex: Option[String] = None): XmlDataset = {
    val dataset = XmlDataset(ReadOnlyResource(ClasspathResource("org/silkframework/plugins/dataset/xml/persons.zip")))
    fileRegex match {
      case Some(regex) => dataset.copy(zipFileRegex = regex)
      case None => dataset
    }
  }

  it should "only read from files with 'xml' suffix from bulk zip files" in {
    retrieveIDs(zipDataset()).flatMap(_.values.flatten) mustBe Seq("4", "1", "2")
  }

  it should "read all files defined by the file regex" in {
    retrieveIDs(zipDataset(fileRegex = Some("\\.xml"))).flatMap(_.values.flatten) mustBe Seq("4", "1", "2", "3") // All files containing .xml
    retrieveIDs(zipDataset(fileRegex = Some("^[^/]*\\.xml$"))).flatMap(_.values.flatten) mustBe Seq("1", "2") // All files ending with .xml in root dir
    retrieveIDs(zipDataset(fileRegex = Some("\\.xml.bak$"))).flatMap(_.values.flatten) mustBe Seq("3") // all files ending with .xml.bak
  }

  it should "generate an understandable error message if a file could not be read inside the ZIP file" in {
    val failedExecution = Try(retrieveIDs(zipDataset(fileRegex = Some(".*"))))
    failedExecution.isFailure mustBe true
    val errorMessage = failedExecution.failed.get.getMessage
    errorMessage must include ("brokenXml.broken")
    errorMessage must include ("persons.zip")
  }

  private def retrieveIDs(dataset: XmlDataset) = {
    dataset.source.retrieve(EntitySchema("Person", typedPaths = IndexedSeq(UntypedPath("ID").asStringTypedPath))).entities.toArray.toSeq
  }

  private def testOutputTemplate(outputTemplate: String) = {
    XmlDataset(InMemoryResourceManager.apply().get("test.xml"), outputTemplate = MultilineStringParameter(outputTemplate))
  }
}
