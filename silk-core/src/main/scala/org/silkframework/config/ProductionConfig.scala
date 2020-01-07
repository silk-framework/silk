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
package org.silkframework.config

import com.typesafe.config.{Config => TypesafeConfig}

/**
  * Production config related properties.
  */
object ProductionConfig {
  /** Is the application currently in safe mode */
  private var _inSafeMode: Boolean = true

  /** Is the safe mode configured, i.e. the application can be set into safe-mode. Else it is always "unsafe". */
  def safeModeEnabled: Boolean = {
    val cfg: TypesafeConfig = DefaultConfig.instance()
    cfg.getBoolean("config.production.safeMode")
  }

  /** Is the application currently in safe mode. */
  def inSafeMode: Boolean = synchronized {
    safeModeEnabled && _inSafeMode
  }

  /** Puts the application into safe-mode. This has no effect if safe-mode has not been enabled in the configuration. */
  def setSafeMode(enable: Boolean): Unit = synchronized {
    _inSafeMode = enable
  }
}
