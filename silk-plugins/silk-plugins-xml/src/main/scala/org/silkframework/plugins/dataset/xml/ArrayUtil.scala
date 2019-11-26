package org.silkframework.plugins.dataset.xml

import java.util

import scala.reflect.ClassTag

/**
  * Array utility functions.
  */
object ArrayUtil {
  def filterArray[T : ClassTag](arr: Array[T], cond: T => Boolean): Array[T] = {
    var idx = 0
    val indices = new util.ArrayList[Int]()
    while(idx < arr.length) {
      if(cond(arr(idx))) {
        indices.add(idx)
      }
      idx += 1
    }
    val outputArr = new Array[T](indices.size())
    idx = 0
    while(idx < indices.size()) {
      outputArr(idx) = arr(indices.get(idx))
      idx += 1
    }
    outputArr
  }
}
