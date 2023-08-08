package org.silkframework.config

import org.silkframework.entity.EntitySchema

/**
  * Specifies the type of an input or output port.
  */
sealed trait Port {

  def schemaOpt: Option[EntitySchema]

}

/**
  * Input or output port that has a fixed schema.
  */
case class FixedSchemaPort(schema: EntitySchema) extends Port {

  override def schemaOpt: Option[EntitySchema] = Some(schema)

}

/**
  * Port that does not have a fixed schema, but will adapt its schema to the connected port.
  *
  * Flexible input ports will adapt the schema to the connected output.
  * Flexible output ports will adapt the schema to the connected input.
  */
object FlexibleSchemaPort extends Port {

  override def schemaOpt: Option[EntitySchema] = None

}

/**
  * Port for which the schema is not known in advance.
  * Only valid for outputs.
  */
object UnknownSchemaPort extends Port {

  override def schemaOpt: Option[EntitySchema] = None
}


/**
  * Specifies the input ports of a workflow operator.
  */
sealed trait InputPorts

/**
  * Operator accepts a fixed number of inputs.
  */
case class FixedNumberOfInputs(ports: Seq[Port]) extends InputPorts

/**
  * Operator accepts a flexible number of inputs.
  * At the moment, each input is a flexible schema port.
  */
case class FlexibleNumberOfInputs() extends InputPorts