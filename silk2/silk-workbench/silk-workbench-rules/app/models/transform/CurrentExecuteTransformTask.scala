package models.transform

import de.fuberlin.wiwiss.silk.execution.ExecuteTransform
import de.fuberlin.wiwiss.silk.runtime.task.{TaskControl, TaskContext}
import models.TaskData

object CurrentExecuteTransformTask extends TaskData[TaskControl](null) {
}
