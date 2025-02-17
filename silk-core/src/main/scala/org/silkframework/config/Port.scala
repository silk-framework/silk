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
case class FlexibleSchemaPort(explicitSchema: Boolean = false) extends Port {

  override def schemaOpt: Option[EntitySchema] = None

}

/**
  * Port for which the schema is not known in advance.
  *
  * This includes output ports with a schema that depends on external factors (e.g., REST requests).
  */
case object UnknownSchemaPort extends Port {

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
  * Operator accepts a flexible number of inputs with the same port definition.
  *
  * @param portDefinition The port definition of each of the input ports.
  * @param min            The minimum number of ports that need to be connected.
  * @param max            The maximum number of ports that can be connected. None means unlimited.
  */
case class FlexibleNumberOfInputs(portDefinition: Port = FlexibleSchemaPort(),
                                  min: Int = 0,
                                  max: Option[Int] = None) extends InputPorts