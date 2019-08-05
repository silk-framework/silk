package org.silkframework.util

import java.lang.reflect.{ParameterizedType, TypeVariable}

object ClassUtil {

  /**
    * Given a concrete class, extracts all generic type parameters of an interface it implements.
    */
  def getInterfaceTypeParameters(clazz: Class[_], interface: Class[_]): Seq[Class[_]] = {
    val (parametrizedInterface, inheritanceTrail) = findInterface(clazz, interface).get
    for(index <- parametrizedInterface.getActualTypeArguments.indices) yield {
      getTypeArgument(parametrizedInterface, index, inheritanceTrail)
    }
  }

  private def findInterface(clazz: Class[_], interface: Class[_], inheritanceTrail: List[Class[_]] = List.empty): Option[(ParameterizedType, List[Class[_]])] = {
    clazz.getGenericInterfaces.collect { case pt: ParameterizedType => pt }.find(_.getRawType == interface) match {
      case Some(executorInterface) =>
        Some((executorInterface, clazz :: inheritanceTrail))
      case None =>
        val superInterfaces =
          for (superInterface <- clazz.getGenericInterfaces) yield superInterface match {
            case c: Class[_] => c
            case pt: ParameterizedType => pt.getRawType.asInstanceOf[Class[_]]
          }
        val superTypes = superInterfaces ++ Option(clazz.getSuperclass)
        superTypes.flatMap(c => findInterface(c, interface, clazz :: inheritanceTrail)).headOption
    }
  }

  private def getTypeArgument(pt: ParameterizedType, index: Int, inheritanceTrail: List[Class[_]]): Class[_] = {
    pt.getActualTypeArguments.apply(index) match {
      case c: Class[_] => c
      case tv: TypeVariable[_] =>
        val actualType = for (descendent <- inheritanceTrail;
                              interface <- descendent.getGenericInterfaces ++ Option(descendent.getGenericSuperclass) if interface.isInstanceOf[ParameterizedType];
                              paramType = interface.asInstanceOf[ParameterizedType];
                              rawType = paramType.getRawType.asInstanceOf[Class[_]];
                              (typeParam, idx) <- rawType.getTypeParameters.zipWithIndex
                              if typeParam.getName == tv.getName && paramType.getActualTypeArguments()(idx).isInstanceOf[Class[_]]) yield {
          paramType.getActualTypeArguments()(idx).asInstanceOf[Class[_]]
        }
        actualType.headOption.getOrElse(throw new Exception("Type variable " + tv.getName + " could not be resolved!"))
    }
  }

  private val fullBuildInfoExpectedNamespaces = Seq(
    "org.silkframework",
    "com.eccenca"
  ).map(ns => "urn:" + ns.replace('.', ':'))

  def getClassVersionUri(cls: Class[_]): Uri ={
    val packageUri = "urn:" + cls.getPackage.getName.replace('.', ':') + ":"
    ScalaReflectUtils.findClosestBuildInfo(cls).map(bi => {
      val buildInfo = ScalaReflectUtils.listMembers(bi)
      val commit = buildInfo.get("gitHeadCommit").asInstanceOf[Option[Option[String]]].flatten
      val version = Option(buildInfo.getOrElse("version", null))

      if(fullBuildInfoExpectedNamespaces.exists(ns => packageUri.startsWith(ns) && (commit.isEmpty || version.isEmpty)))
        throw new IllegalStateException("BuildInfo not sufficient for package " + cls.getPackage.getName + ". Make sure to include the BuildInfo command in build.sbt.")

      Uri(packageUri + cls.getSimpleName +
        version.map(v => ":" + v.toString).getOrElse("") +
        commit.map(c => ":" + c.toString).getOrElse(""))
    }).getOrElse(
      if(fullBuildInfoExpectedNamespaces.exists(ns => packageUri.startsWith(ns)))
        throw new IllegalStateException("No BuildInfo found in package " + cls.getPackage.getName + ". Make sure to include the BuildInfo command in build.sbt.")
      else
        Uri(packageUri + cls.getSimpleName)
    )
  }
}
