package models.transform

import de.fuberlin.wiwiss.silk.execution.ExecuteTransform
import de.fuberlin.wiwiss.silk.runtime.activity.{ActivityControl, ActivityContext}
import models.TaskData

object CurrentExecuteTransformTask extends TaskData[ActivityControl](null) {
}
