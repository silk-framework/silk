package org.silkframework.rule

import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat}
import org.silkframework.runtime.serialization.XmlSerialization.{fromXml, toXml}
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.Identifier

import scala.collection.mutable.ArrayBuffer
import scala.language.implicitConversions
import scala.xml.{Node, Null}

/**
  * A set of mapping Rules consisting of a URI rule, a sequence of type rules and a sequence of property rules.
  */
case class MappingRules(uriRule: Option[UriMapping] = None,
                        typeRules: Seq[TypeMapping] = Seq.empty,
                        propertyRules: Seq[TransformRule] = Seq.empty) {

  /** All rules (URI rule, type rules and property rules). Does not include recursive children. */
  def allRules: Seq[TransformRule] = uriRule.toSeq ++ typeRules ++ propertyRules

  /** All rules (URI rule, type rules and property rules) of the complete transform tree, i.e. including children etc. */
  def allRulesRecursive: Seq[TransformRule] = {
    val rules = new ArrayBuffer[TransformRule]()
    def addRulesRecursively(mappingRules: MappingRules): Unit = {
      val allRules = mappingRules.allRules
      if(allRules.nonEmpty) {
        rules.addAll(allRules)
        allRules.foreach {
          case r: ContainerTransformRule =>
            addRulesRecursively(r.rules)
          case _ =>
        }
      }
    }
    addRulesRecursively(this)
    rules.toSeq
  }
}

object MappingRules {

  final val empty: MappingRules = MappingRules(None, Seq.empty, Seq.empty)

  implicit def toSeq(rules: MappingRules): Seq[TransformRule] = rules.allRules

  implicit def fromSeq(rules: Seq[TransformRule]): MappingRules = {
    val uriRules = rules.collect{ case u: UriMapping => u }
    if(uriRules.size > 1) {
      throw new ValidationException(s"A mapping rule holds at most one URI rule, but '${uriRules.head.id}' and " +
        s"'${uriRules(1).id}' were given. Update the existing URI rule instead of adding a second one.")
    }
    MappingRules(
      uriRule = uriRules.headOption,
      typeRules = rules.collect{ case t: TypeMapping => t },
      propertyRules = rules.filterNot(r => r.isInstanceOf[UriMapping] || r.isInstanceOf[TypeMapping])
    )
  }

  def apply(rules: TransformRule*): MappingRules = {
    fromSeq(rules)
  }

  implicit object MappingRulesFormat extends XmlFormat[MappingRules] {
    /**
      * Deserialize a value from XML.
      */
    override def read(node: Node)(implicit readContext: ReadContext): MappingRules = {
      val rules = (node \ "_").map(fromXml[TransformRule])
      MappingRules.fromSeq(rules)
    }

    /**
      * Serialize a value to XML.
      */
    override def write(value: MappingRules)(implicit writeContext: WriteContext[Node]): Node = {
      <MappingRules>
        {value.allRules.map(toXml[TransformRule])}
      </MappingRules>
    }
  }

}