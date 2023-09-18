package org.silkframework.entity.metadata

/**
  * Entity level metadata object defining the internal order of entities in a [[com.eccenca.di.spark.entities.SparkEntities]] collection
  * @param initial - initial order number assigned at the ingestion of the collection
  * @param subsequent - subsequent order numbers if initial is equal
  *   these become necessary when inserting Entities in an already established initial order
  *   Example
  *   | initial | subsequent |
  *       0
  *       1
  *       2
  *       3         <- missing digits in subsequent compared to other entities with the same initial order are assumed as '0'
  *       3       1 <- when inserting always start with subsequent order 1 (this is the first entity of the first round of inserting
  *       3       2                   (always duplicating the initial order of the entity after which to insert)
  *       3       201 <- this entity has been inserted after entity with order 3:21 already existed (and belongs to the third round of inserting also after 3:2)
  *       3       21  <- this is first entity of the second round of inserting (inserted after 3:2
  *       3       22  <- second entity of the second round of inserting
  *       3       23
  *       4
  *       5
  */
case class InternalOrder(initial: Long, subsequent: Seq[Int] = Seq()) extends Comparable[InternalOrder] with Serializable {

  override def hashCode(): Int = initial.toString.hashCode * 31 + subsequent.foldLeft(1)(31 * _ + _.hashCode())

  override def equals(obj: scala.Any): Boolean = {
    obj match{
      case io: InternalOrder => compareTo(io) == 0
      case _ => false
    }
  }

  override def compareTo(io: InternalOrder): Int = {
    if(this.initial.compare(io.initial) != 0){
      this.initial.compare(io.initial)      //first test the initial order
    }
    else{
      val maxDigits = Seq(this.subsequent.length, io.subsequent.length).max
      val ap = this.subsequent.padTo(maxDigits, 0)
      val bp = io.subsequent.padTo(maxDigits, 0)
      for(i <- ap.indices){         //now iterate over all subsequent orders
        if(ap(i).compare(bp(i)) != 0){
          return ap(i).compare(bp(i))
        }
      }
      0
    }
  }
}

object InternalOrder{

  implicit val ordering: Ordering[InternalOrder] = Ordering.fromLessThan((io1: InternalOrder, io2: InternalOrder) => io1.compareTo(io2) < 0)

  /**
    * Increases the subsequential order of the given index by one
    * @param io   the [[InternalOrder]] directly before the new instance
    * @param ind  the index of the subsequential order sequence which to increase
    * @return
    */
  private def addOneSubsequential(io: InternalOrder, ind: Int): InternalOrder = {
    if(io.subsequent.length <= ind){
      InternalOrder(io.initial, io.subsequent.padTo(ind, 0) ++ Seq(1))
    }
    else{
      val lastCount = io.subsequent(ind)
      InternalOrder(io.initial, io.subsequent.dropRight(io.subsequent.length - ind) ++ Seq(lastCount + 1))
    }
  }

  /**
    * Will select the correct index which shall be increase to create a new instance of [[InternalOrder]] based on both surrounding orders
    * @param a - the subsequential index sequence of the entity directly before the new instance
    * @param b - the subsequential index sequence of the entity directly after the new instance
    * @return - the index which should be increased
    */
  private def getNextSubsequentIndex(a: Seq[Int], b: Seq[Int]): Int ={
    val maxDigits = Seq(a.length, b.length).max
    val ap = a.padTo(maxDigits, 0)
    val bp = b.padTo(maxDigits, 0)
    for(i <- 0 until maxDigits){
      val diff = bp(i) - ap(i)
      if(diff < 0 || diff > 1){
        return i
      }
    }
    maxDigits
  }

  /**
    * Will calculate a new instance of [[InternalOrder]] based on the instances directly surrounding the new instance (when sorted)
    * @param before - the [[InternalOrder]] directly before the new instance
    * @param after - the [[InternalOrder]] directly after the new instance
    */
  def establishOrderBetween(before: InternalOrder, after: InternalOrder): InternalOrder ={
    assert(ordering.lt(before, after), "InternalOrder of first argument has to be lower than the second argument.")

    if(before.initial == after.initial){
      val nextIndex = getNextSubsequentIndex(before.subsequent, after.subsequent)
      addOneSubsequential(before, nextIndex)
    }
    else{
      addOneSubsequential(before, 0)
    }
  }
}
