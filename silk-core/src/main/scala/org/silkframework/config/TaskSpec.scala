package org.silkframework.config

import org.silkframework.entity.SchemaTrait
import org.silkframework.util.Identifier

/**
  * Base trait of all task specifications.
  */
trait TaskSpec {

  /**
    * The schemata of the input data for this task.
    * A separate entity schema is returned for each input.
    * Or None is returned, which means that this task can handle any number of inputs and any kind
    * of entity schema.
    * A result of Some(Seq()) on the other hand means that this task has no inputs at all.
    */
  def inputSchemataOpt: Option[Seq[SchemaTrait]]

  /**
    * The schema of the output data.
    * Returns None, if the schema is unknown or if no output is written by this task.
    */
  def outputSchemaOpt: Option[SchemaTrait]

  /**
    * The tasks that are referenced by this task.
    */
  def referencedTasks: Set[Identifier] = Set.empty

}
