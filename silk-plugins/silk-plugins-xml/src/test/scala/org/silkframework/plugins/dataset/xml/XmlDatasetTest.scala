package org.silkframework.plugins.dataset.xml

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.entity.EntitySchema
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.runtime.activity.TestUserContextTrait
import org.silkframework.runtime.plugin.MultilineStringParameter
import org.silkframework.runtime.resource.{ClasspathResource, InMemoryResourceManager, ReadOnlyResource}
import org.silkframework.runtime.validation.ValidationException

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
    retrieveIDs(zipDataset()).flatMap(_.values.flatten) mustBe Seq("1", "2")
  }

  it should "read all files defined by the file regex" in {
    retrieveIDs(zipDataset(fileRegex = Some("\\.xml"))).flatMap(_.values.flatten) mustBe Seq("1", "2", "3")
    retrieveIDs(zipDataset(fileRegex = Some("\\.xml.bak$"))).flatMap(_.values.flatten) mustBe Seq("3")
  }

  private def retrieveIDs(dataset: XmlDataset) = {
    dataset.source.retrieve(EntitySchema("Person", typedPaths = IndexedSeq(UntypedPath("ID").asStringTypedPath))).toArray.toSeq
  }

  private def testOutputTemplate(outputTemplate: String) = {
    XmlDataset(InMemoryResourceManager.apply().get("test.xml"), outputTemplate = MultilineStringParameter(outputTemplate))
  }
}
