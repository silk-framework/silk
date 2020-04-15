package org.silkframework.rule

import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat}
import org.silkframework.runtime.serialization.XmlSerialization.{fromXml, toXml}
import org.silkframework.util.Identifier

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

}

object MappingRules {

  final val empty: MappingRules = MappingRules(None, Seq.empty, Seq.empty)

  implicit def toSeq(rules: MappingRules): Seq[TransformRule] = rules.allRules

  implicit def fromSeq(rules: Seq[TransformRule]): MappingRules = {
    MappingRules(
      uriRule = rules.collectFirst{ case u: UriMapping => u },
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