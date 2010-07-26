package de.fuberlin.wiwiss.silk.server.model

case class ServerConfig(writeUnmatchedInstances : Boolean = false,
                        returnUnmatchedInstances : Boolean = false)