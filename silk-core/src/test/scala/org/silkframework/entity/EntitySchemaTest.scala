package org.silkframework.entity

import org.scalatest.{FlatSpec, Matchers}
import org.silkframework.util.Uri

class EntitySchemaTest extends FlatSpec with Matchers {

  private val entityTest = new EntityTest
  val schema = EntitySchema(typeUri = Uri("name"), typedPaths = IndexedSeq(Path("path1").asStringTypedPath, Path("path2").asStringTypedPath), filter = Restriction.empty)

  val subSchema1 = EntitySchema(typeUri = Uri("sub1"), typedPaths = IndexedSeq(Path("path3").asUntypedValueType, Path("path4").asStringTypedPath), filter = Restriction.empty, Path("sub1"))
  val subSchema2 = EntitySchema(typeUri = Uri("sub2"), typedPaths = IndexedSeq(Path("path5").asStringTypedPath, Path("path6").asStringTypedPath), filter = Restriction.empty, Path("sub2"))

  val complexSchema = new MultiEntitySchema(
    EntitySchema(typeUri = Uri(""), typedPaths = IndexedSeq(Path("path1").asUntypedValueType, Path("path2").asStringTypedPath), filter = Restriction.empty)
    , IndexedSeq(subSchema1, subSchema2))

  val complexSchemaDifferentType = new MultiEntitySchema(
    EntitySchema(typeUri = Uri(""), typedPaths = IndexedSeq(TypedPath(Path("path1"), IntegerValueType, false), Path("path2").asStringTypedPath), filter = Restriction.empty)
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
