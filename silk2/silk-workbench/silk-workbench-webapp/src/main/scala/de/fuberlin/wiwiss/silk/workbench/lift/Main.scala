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

package de.fuberlin.wiwiss.silk.workbench.lift

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.webapp.WebAppContext

/**
 * Starts the Workbench.
 */
object Main {
  def main(args : Array[String]) {
    val server = new Server(8080)

    val webapp = new WebAppContext();
    webapp.setContextPath("/");
    val protectionDomain = Main.getClass.getProtectionDomain
    val location = protectionDomain.getCodeSource.getLocation.toExternalForm
    webapp.setWar(location);
    server.setHandler(webapp);

    server.start()
  }
}
