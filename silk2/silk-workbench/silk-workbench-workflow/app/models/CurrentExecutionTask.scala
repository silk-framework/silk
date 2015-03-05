package models

import de.fuberlin.wiwiss.silk.runtime.activity.{ActivityControl, ActivityContext, Activity}

object CurrentExecutionTask extends TaskData[ActivityControl[_]](null) {

}
