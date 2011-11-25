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

package de.fuberlin.wiwiss.silk.workbench.workspace

import java.io.{IOException, File}
import java.util.logging.{Level, Logger}

class FileUser extends User {
  override def workspace = FileUser.workspace
}

object FileUser {
  private lazy val workspaceDir = new File(System.getProperty("user.home") + "/.silk/workspace/")

  val workspace = {
    try {
      if(!workspaceDir.exists && !workspaceDir.mkdirs()) throw new IOException("Could not create workspace directory at: " + workspaceDir.getCanonicalPath)

      new FileWorkspace(workspaceDir)
    }
    catch {
      case ex: Exception => {
        Logger.getLogger(FileUser.getClass.getName).log(Level.SEVERE, "Error loading workspace", ex)
        throw ex
      }
    }
  }
}

