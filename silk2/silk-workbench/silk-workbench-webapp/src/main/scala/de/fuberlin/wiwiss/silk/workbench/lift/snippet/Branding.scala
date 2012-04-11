package de.fuberlin.wiwiss.silk.workbench.lift.snippet

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

import net.liftweb.util._
import Helpers._
import java.util.Properties
import java.io.{FileNotFoundException, FileReader, File}

class Branding {
  var workbenchName = "Silk Workbench"

  try {
    val configPath = scala.util.Properties.envOrElse("SILK_WORKBENCH_CONFIG_PATH", "");
    val configFile = new File(configPath + "config.properties");

    val properties = new Properties()
    properties.load(new FileReader(configFile))


    if(properties.getProperty("workbenchName") != null) workbenchName = properties.getProperty("workbenchName")
  } catch {
    case _ : FileNotFoundException =>
      {
      }
  }

  def render = "*" #> workbenchName
}