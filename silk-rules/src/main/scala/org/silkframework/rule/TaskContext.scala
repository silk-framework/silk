package org.silkframework.rule

import org.silkframework.config.Task

case class TaskContext(inputTasks: Seq[Task[_]])