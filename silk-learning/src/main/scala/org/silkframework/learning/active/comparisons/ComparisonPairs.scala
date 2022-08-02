package org.silkframework.learning.active.comparisons

import org.silkframework.entity.paths.TypedPath
import org.silkframework.runtime.serialization.WriteContext
import org.silkframework.serialization.json.EntitySerializers.PairJsonFormat
import org.silkframework.serialization.json.WriteOnlyJsonFormat
import org.silkframework.util.DPair
import play.api.libs.json.{JsValue, Json}

/**
  * Holds the current state of the active learning workflow.
  *
  * @param comparisonPaths The matching paths from source and target.
  */
case class ComparisonPairs(comparisonPaths: Seq[DPair[TypedPath]],
                           randomSeed: Long)

case class ComparisonPair(

                         )

object ComparisonPairs {

  def initial(randomSeed: Long): ComparisonPairs = {
    ComparisonPairs(Seq.empty, randomSeed = randomSeed)
  }

  implicit object TypedPathJsonFormat extends WriteOnlyJsonFormat[TypedPath] {

    override def write(value: TypedPath)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      Json.obj(
        "path" -> value.normalizedSerialization,
        "valueType" -> value.valueType.id
      )
    }
  }


  implicit object ComparisonPairsJsonFormat extends WriteOnlyJsonFormat[ComparisonPairs] {

    private val comparisonPathsFormat = new PairJsonFormat()(TypedPathJsonFormat)

    override def write(value: ComparisonPairs)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      Json.obj(
        "comparisonPaths" -> value.comparisonPaths.map(comparisonPathsFormat.write)
      )
    }
  }
}


