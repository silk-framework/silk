package org.silkframework.entity

/**
  *
  */
trait EntityTrait {
  type SchemaType <: SchemaTrait

  def uri: String
}