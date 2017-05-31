package org.silkframework.entity

class MultiEntitySchema(pivotSchema: EntitySchema,
                        val subSchemata: Seq[EntitySchema]) extends EntitySchema(pivotSchema.typeUri,
                                                                                 pivotSchema.typedPaths,
                                                                                 pivotSchema.filter,
                                                                                 pivotSchema.subPath) {
}
