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

package de.fuberlin.wiwiss.silk.workspace

/**
 * A user.
 */
trait User {

  /**
   * The current workspace of this user.
   */
  def workspace: Workspace
}

object User {
  private lazy val defaultUser = new FileUser

  var userManager: () => User = () => defaultUser

  /**
   * Retrieves the current user.
   */
  def apply() = userManager()
}