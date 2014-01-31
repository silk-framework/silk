package de.fuberlin.wiwiss.silk.preprocessing.entity


case class Entity(uri:String, properties:Traversable[Property]) {


  override def toString = {
   "<" + uri + "> {\n"+ properties.mkString +"}"
  }


}

case class Property(path:String, value:String) {
  override def toString = {
    "<" + path + "> " + value + "\n"
  }
}
