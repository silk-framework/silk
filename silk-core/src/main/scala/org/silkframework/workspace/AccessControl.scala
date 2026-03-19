package org.silkframework.workspace

import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat}

import scala.xml.Node

/**
  * Access control configuration for a project.
  *
  * @param groups The set of access control group identifiers.
  */
case class AccessControl(groups: Set[String])

object AccessControl {

  /** Empty access control with no groups. */
  val empty: AccessControl = AccessControl(Set.empty)

  /**
    * XML serialization format.
    */
  implicit object AccessControlXmlFormat extends XmlFormat[AccessControl] {
    /**
      * Deserialize a value from XML.
      */
    def read(node: Node)(implicit readContext: ReadContext): AccessControl = {
      val groups = (node \ "Group").map(_.text).toSet
      AccessControl(groups)
    }

    /**
      * Serialize a value to XML.
      */
    def write(accessControl: AccessControl)(implicit writeContext: WriteContext[Node]): Node = {
      <AccessControl>
        {accessControl.groups.toSeq.sorted.map(group => <Group>{group}</Group>)}
      </AccessControl>
    }
  }

}
