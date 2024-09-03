package org.silkframework.rule

import org.silkframework.config.Task

/**
 * The context in which a task is executed.
 *
 * @param inputTasks The input tasks.
 *                   If the task is executed within a workflow, those are the connected input task(s).
 *                   If the task is executed standalone, those are the configured default input(s).
 */
case class TaskContext(inputTasks: Seq[Task[_]])