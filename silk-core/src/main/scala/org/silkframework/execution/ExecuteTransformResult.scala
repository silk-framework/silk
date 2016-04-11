package org.silkframework.execution

/**
  * Created on 4/11/16.
  */
case class ExecuteTransformResult(entityCounter: Long, entityErrorCounter: Long, ruleErrorCounter: Map[String, Long])
