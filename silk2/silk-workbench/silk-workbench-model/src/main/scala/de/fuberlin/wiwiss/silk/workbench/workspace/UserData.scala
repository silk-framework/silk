package de.fuberlin.wiwiss.silk.workbench.workspace

import collection.mutable.{Publisher, WeakHashMap}

/** Holds user specific data. */
class UserData[T](initialValue : T) extends Publisher[UserData.ValueUpdated[T]]
{
  private val values = new WeakHashMap[User, T]()

  def apply() : T =
  {
    values.get(User()) match
    {
      case Some(value) => value
      case None => initialValue
    }
  }

  def update(newValue : T)
  {
    values.update(User(), newValue)
    publish(UserData.ValueUpdated(newValue))
  }
}

object UserData
{
  case class ValueUpdated[T](newValue : T)
}
