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

package org.silkframework.dataset.sql

import org.silkframework.runtime.activity.UserContext

/**
 * Represents a SQL endpoint and provides an interface to execute queries on it.
 */
trait SqlEndpoint {

  /**
    * Executes a fixed statement to manipulate data.
    */
  def updateStatement(query: String)(implicit userContext: UserContext): Unit = {
    throw new UnsupportedOperationException(s"Endpoint type $getClass does not support issuing SQL queries/statements")
  }
}
