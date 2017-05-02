package org.silkframework.workspace.activity.transform

import org.silkframework.entity.{EntitySchema, SchemaTrait}
import org.silkframework.rule.TransformSpec
import org.silkframework.runtime.activity.{Activity, ActivityContext}
import org.silkframework.workspace.ProjectTask
import org.silkframework.workspace.activity.PathsCacheTrait

/**
 * Holds the most frequent paths.
 */
class TransformPathsCache(task: ProjectTask[TransformSpec]) extends Activity[EntitySchema] with PathsCacheTrait {

  override def name: String = s"Paths cache ${task.id}"

  override def initialValue: Option[EntitySchema] = Some(EntitySchema.empty)

  /**
   * Loads the most frequent paths.
   */
  override def run(context: ActivityContext[EntitySchema]): Unit = {
    val transform = task.data

    //Create an entity description from the transformation task
    val currentEntityDesc: EntitySchema = SchemaTrait.toEntitySchema(transform.inputSchema) // TODO: Support nested schemata?

    //Check if paths have not been loaded yet or if the restriction has been changed
    if (context.value().typedPaths.isEmpty || currentEntityDesc.typeUri != context.value().typeUri) {
      // Retrieve the data sources
      val paths = retrievePathsOfInput(task.data.selection.inputId, Some(transform.selection), task, context)
      //Add the frequent paths to the entity description
      context.value() = currentEntityDesc.copy(typedPaths = (currentEntityDesc.typedPaths ++ paths).distinct)
    }
  }
}