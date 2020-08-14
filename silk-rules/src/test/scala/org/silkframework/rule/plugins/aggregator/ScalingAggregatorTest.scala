/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.silkframework.rule.plugins.aggregator

import org.silkframework.rule.plugins.aggegrator.ScalingAggregator
import org.silkframework.rule.similarity.Aggregator
import org.silkframework.test.PluginTest
import org.silkframework.testutil.approximatelyEqualTo


class ScalingAggregatorTest extends PluginTest {

  it should "scale the input similarity values by the specified factor" in {
    ScalingAggregator(factor = 0.5).evaluate((1, 1.0) :: Nil).get should be(approximatelyEqualTo(0.5))
    ScalingAggregator(factor = 0.1).evaluate((1, 0.1) :: Nil).get should be(approximatelyEqualTo(0.01))
  }

  it should "fail if factor is invalid" in {
    an [IllegalArgumentException] should be thrownBy ScalingAggregator(factor = 1.1)
    an [IllegalArgumentException] should be thrownBy ScalingAggregator(factor = -0.1)
  }

  override def pluginObject: Aggregator = ScalingAggregator()
}
