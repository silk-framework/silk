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

package de.fuberlin.wiwiss.silk.hadoop.load

import java.io.IOException

/**
 * RDF Quad. At the moment we don't store the fourth component.
 */
class Quad(val subject : String, val predicate : String, val value : String)

object Quad
{
  def parse(line : String) : Quad =
  {
    line.split(' ') match
    {
      case Array(subject, predicate, value) => new Quad(subject, predicate, value)
      case _ => throw new IOException("Invalid line '" + line + "'")
    }
  }
}