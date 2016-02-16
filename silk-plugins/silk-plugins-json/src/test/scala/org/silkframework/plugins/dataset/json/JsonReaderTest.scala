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

import org.scalatest.{FlatSpec, Matchers}
import org.silkframework.entity.Path
import org.silkframework.runtime.resource.ClasspathResourceLoader
import play.api.libs.json.JsValue


class JsonReaderTest extends FlatSpec with Matchers {

  private val json = {
    val resources = new ClasspathResourceLoader("org/silkframework/plugins/dataset/json")
    JsonParser.load(resources.get("example.json"))
  }

  private val persons = JsonParser.select(json, "persons" :: Nil)

  private val phoneNumbers = JsonParser.select(json, "persons" :: "phoneNumbers" :: Nil)

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

  it should "support backward paths" in {
    evaluate(phoneNumbers, "\\persons/id") should equal (Seq("0", "1"))
  }

  private def evaluate(values: Seq[JsValue], path: String): Seq[String] = {
    values.flatMap(value => JsonParser.evaluate(value, Path.parse(path)))
  }
}