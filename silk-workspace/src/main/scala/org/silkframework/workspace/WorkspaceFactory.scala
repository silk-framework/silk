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

package org.silkframework.workspace

import org.silkframework.runtime.activity.UserContext

/**
 * Workspace manager that returns the workspace
 */
trait WorkspaceFactory {

  /**
   * The current workspace of this user.
   */
  def workspace(implicit userContext: UserContext): Workspace
}

object WorkspaceFactory {
  private lazy val defaultWorkspaceFactory = new FileWorkspaceFactory

  @volatile // factory method for creating the workspace factory
  var factory: WorkspaceFactory = defaultWorkspaceFactory

  /**
   * Retrieves the current workspace factory
   */
  def apply(): WorkspaceFactory = factory
}