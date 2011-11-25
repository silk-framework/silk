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

package de.fuberlin.wiwiss.silk.hadoop

import org.apache.hadoop.conf.Configured
import org.apache.hadoop.util.{ToolRunner, Tool}

object Silk
{
  def main(args : Array[String])
  {
    val res = ToolRunner.run(new Silk(), args)
    System.exit(res)
  }
}

class Silk extends Configured with Tool
{
  def run(args : Array[String]) : Int =
  {
    args match
    {
      case Array("load", configFile, outputDir)           => new Load(configFile, outputDir, None          , getConf())()
      case Array("load", configFile, outputDir, linkSpec) => new Load(configFile, outputDir, Some(linkSpec), getConf())()
      case Array("match", inputDir, outputDir)            => new Match(inputDir , outputDir, None          , getConf())()
      case Array("match", inputDir, outputDir, linkSpec)  => new Match(inputDir , outputDir, Some(linkSpec), getConf())()
      case _ => printUsage()
    }

    0
  }

  private def printUsage()
  {
    println("usage:")
    println("  load configFile ouputDir [linkSpec]")
    println("  match inputDir ouputDir [linkSpec]")
  }
}
