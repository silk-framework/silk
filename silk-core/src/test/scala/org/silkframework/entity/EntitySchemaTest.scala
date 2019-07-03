package org.silkframework.entity

import org.scalatest.{FlatSpec, Matchers}
import org.silkframework.entity.paths.{TypedPath, UntypedPath}
import org.silkframework.util.Uri

class EntitySchemaTest extends FlatSpec with Matchers {

  private val entityTest = new EntityTest
  val schema = EntitySchema(typeUri = Uri("name"), typedPaths = IndexedSeq(UntypedPath("path1").asStringTypedPath, UntypedPath("path2").asStringTypedPath), filter = Restriction.empty)

  val subSchema1 = EntitySchema(typeUri = Uri("sub1"), typedPaths = IndexedSeq(UntypedPath("path3").asUntypedValueType, UntypedPath("path4").asStringTypedPath), filter = Restriction.empty, UntypedPath("sub1"))
  val subSchema2 = EntitySchema(typeUri = Uri("sub2"), typedPaths = IndexedSeq(UntypedPath("path5").asStringTypedPath, UntypedPath("path6").asStringTypedPath), filter = Restriction.empty, UntypedPath("sub2"))

  val complexSchema = new MultiEntitySchema(
    EntitySchema(typeUri = Uri(""), typedPaths = IndexedSeq(UntypedPath("path1").asUntypedValueType, UntypedPath("path2").asStringTypedPath), filter = Restriction.empty)
    , IndexedSeq(subSchema1, subSchema2))

  val complexSchemaDifferentType = new MultiEntitySchema(
    EntitySchema(typeUri = Uri(""), typedPaths = IndexedSeq(TypedPath(UntypedPath("path1"), IntegerValueType, isAttribute = false), UntypedPath("path2").asStringTypedPath), filter = Restriction.empty)
    , IndexedSeq(subSchema1, subSchema2))


  it should "not equal the schema from EntityTest cos of untyped path" in{
    schema equals entityTest.schema shouldBe false                  //fails cos of different uri
   complexSchema equals entityTest.complexSchema shouldBe false     //fails cos of different value Type
 }

  it should "equal the schema from EntityTest when using untyped" in{
    complexSchema equalsUntyped entityTest.complexSchema shouldBe true
    complexSchemaDifferentType equalsUntyped entityTest.complexSchema shouldBe false //fails cos even in equalsUntyped a different ValueType which is not UntypedValueType will not equal
  }
}
