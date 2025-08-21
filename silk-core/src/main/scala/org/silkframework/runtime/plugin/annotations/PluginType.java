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

package org.silkframework.runtime.plugin.annotations;

import org.silkframework.runtime.plugin.*;

import java.lang.annotation.*;

/**
 * Annotates the base class of a plugin type.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PluginType {

  /**
   * A human-readable label for this plugin type.
   * If not provided the label will be generated from the class name.
   * Thus, overwriting the label is usually not necessary.
   */
  String label() default "";

  /**
   * A human-readable description of this plugin type.
   */
  String description() default "";

  /** Optional custom metadata about this plugin. */
  Class<? extends CustomPluginDescriptionGenerator> customDescription() default NoCustomPluginDescription.class;

}
