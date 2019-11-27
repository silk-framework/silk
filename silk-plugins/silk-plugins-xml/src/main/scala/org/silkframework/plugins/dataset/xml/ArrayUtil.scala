package org.silkframework.plugins.dataset.xml

/**
  * Array utility functions.
  */
object ArrayUtil {

  def filterNodeArray(arr: Array[InMemoryXmlNode], cond: InMemoryXmlNode => Boolean): Array[InMemoryXmlNode] = {
    var idx = 0
    var count = 0
    while(idx < arr.length) {
      if(cond(arr(idx))) {
        count += 1
      }
      idx += 1
    }

    val result = new Array[InMemoryXmlNode](count)
    idx = 0
    var targetIndex = 0
    while(idx < arr.length) {
      if(cond(arr(idx))) {
        result(targetIndex) = arr(idx)
        targetIndex += 1
      }
      idx += 1
    }
    result
  }
}
