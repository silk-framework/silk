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

import org.silkframework.runtime.plugin.PluginCategories;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Plugin {

  /** The unique id of the annotated plugin */
  String id();

  /** A human-readable label of the annotated plugin */
  String label();

  /**
   * A list of categories.
   * See {@link PluginCategories for special values}.
   */
  String[] categories() default { PluginCategories.uncategorized };

  /** A short (few sentence) description of this plugin. */
  String description() default "No description";

  /**
   * Optional further documentation for this plugin.
   * Classpath to a Markdown file.
   * Typically the Markdown file is at the same classpath as the documented plugin,
   * in which case the local file name can be provided instead of the full classpath.
   */
  String documentationFile() default "";

  /**
   * Optional icon as data URL string. If not set the plugin will have a generic icon.
   * This icon is rendered in the UI.
   */
  String pluginIcon() default "";
}
