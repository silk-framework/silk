/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.silkframework.plugins.dataset.json


import org.silkframework.entity.paths.UntypedPath
import org.silkframework.runtime.resource.ClasspathResourceLoader
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers


class JsonReaderTest extends AnyFlatSpec with Matchers {

  private val exampleJson = json("example.json")

  private lazy val persons = exampleJson.select("persons" :: Nil)

  "On example.json, JsonReader" should "return 2 persons" in {
    persons.size should equal (2)
  }

  it should "return both person names" in {
    evaluate(persons, "name") should equal (Seq("John", "Max"))
  }

  it should "return all three numbers" in {
    evaluate(persons, "phoneNumbers/number") should equal (Seq("123", "456", "789"))
  }

  it should "return both home numbers" in {
    evaluate(persons, """phoneNumbers[type = "home"]/number""") should equal (Seq("123", "789"))
  }

  it should "return empty values for missing property keys" in {
    evaluate(persons, """phoneNumbers[missingKey = "home"]/number""") should equal (Seq())
  }

  it should "support backward paths" in {
    val phoneNumbers = exampleJson.select("persons" :: "phoneNumbers" :: Nil)
    evaluate(phoneNumbers, "\\phoneNumbers/id") should equal (Seq("0", "0", "1"))

    val phoneNumbers2 = exampleJson.select(UntypedPath.parse("/organizations\\organizations/persons/phoneNumbers").operators)
    evaluate(phoneNumbers2, "\\phoneNumbers/id") should equal (Seq("0", "0", "1"))
  }

  it should "support keys with spaces" in {
    val example2Json = json("example2.json")
    val valuesWithSpaces = example2Json.select("values+with+spaces" :: Nil)
    evaluate(valuesWithSpaces, "space+value") should equal (Seq("Berlin", "Hamburg"))
  }

  it should "allow retrieving ids and texts from array values (if navigateToArrays is true)" in {
    val example = json("exampleArrays.json")

    val arrayItems = example.select("data" :: Nil)
    evaluate(arrayItems, "#id") should equal (Seq("65", "66"))
    evaluate(arrayItems, "#text") should equal (Seq("\"A\"", "\"B\""))
    evaluate(arrayItems, "#arrayText") should equal (Seq("[\"A\",\"B\"]"))

    val rootItems = Seq(example)
    evaluate(rootItems, "data/#text") should equal (Seq("\"A\"", "\"B\""))
    evaluate(rootItems, "data/#arrayText") should equal (Seq("[\"A\",\"B\"]"))
  }

  it should "allow retrieving ids and texts from array values (if navigateToArrays is false)" in {
    val example = json("exampleArrays.json", navigateIntoArrays = false)

    // As navigating into arrays is disabled, we do not get the individual items, but rather the whole array
    val array = example.select("data" :: Nil)
    evaluate(array, "#text") should equal (Seq("[\"A\",\"B\"]")) // The whole array as a string
    evaluate(array, "#array/#text") should equal (Seq("\"A\"", "\"B\""))

    val rootItems = Seq(example)
    evaluate(rootItems, "data/#text") should equal (Seq("[\"A\",\"B\"]"))
    evaluate(rootItems, "data/#array/#text") should equal (Seq("\"A\"", "\"B\""))
  }

  it should "allow retrieving type 3 (name based) UUID based on the string represenation of a JSON node" in {
    evaluate(persons, "#uuid") should equal (Seq("0de70622-5999-36e8-a042-79a0de80f02c", "074b8358-f844-3cb4-a6e8-af2bf456e885"))
  }

  it should "allow retrieving a JSON object as string" in {
    val example = json("example.json")
    val persons = example.select("persons" :: Nil)
    evaluate(persons, "#text") should equal (Seq(
      """{"id":"0","name":"John","phoneNumbers":[{"type":"home","number":"123"},{"type":"office","number":"456"}]}""",
      """{"id":"1","name":"Max","phoneNumbers":[{"type":"home","number":"789"}]}"""))
    evaluate(persons, "phoneNumbers/#text") should equal(Seq(
      """{"type":"home","number":"123"}""", """{"type":"office","number":"456"}""",
      """{"type":"home","number":"789"}"""))
  }

  it should "support retrieving the line and column positions" in {
    val example = json("example.json")
    val persons = example.select("persons" :: Nil)
    evaluate(persons, "#line") should equal(Seq("3", "18"))
    evaluate(persons, "#column") should equal(Seq("5", "5"))
    evaluate(persons, "name/#line") should equal(Seq("5", "20"))
    evaluate(persons, "name/#column") should equal(Seq("15", "15"))
  }

  private def evaluate(values: Seq[JsonTraverser], path: String): Seq[String] = {
    values.flatMap(value => value.evaluate(UntypedPath.parse(path).asStringTypedPath))
  }

  private def json(fileName: String, navigateIntoArrays: Boolean = true): JsonTraverser = {
    val resources = ClasspathResourceLoader("org/silkframework/plugins/dataset/json")
    JsonTraverser.fromResource("alibi_task_id", resources.get(fileName), navigateIntoArrays)
  }
}