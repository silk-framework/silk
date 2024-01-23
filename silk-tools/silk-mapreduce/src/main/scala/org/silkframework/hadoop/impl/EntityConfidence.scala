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

package org.silkframework.hadoop.impl

import java.io.{DataInput, DataOutput}

import org.apache.hadoop.io.Writable

class EntityConfidence(var similarity : Double, var targetUri : String) extends Writable
{
  def this() = this(0.0, null)

  override def write(out : DataOutput)
  {
    out.writeDouble(similarity)
    StreamUtils.writeString(out, targetUri)
  }

  override def readFields(in : DataInput)
  {
    similarity = in.readDouble()
    targetUri = StreamUtils.readString(in)
  }

  override def toString = targetUri + " (" + similarity + ")"

  override def equals(that: Any): Boolean = that match {
    case ec: EntityConfidence => (ec.similarity, ec.targetUri)==(similarity, targetUri)
    case _ => false
  }
}