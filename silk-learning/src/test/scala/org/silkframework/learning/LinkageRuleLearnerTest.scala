package org.silkframework.learning

import buildinfo.BuildInfo
import org.scalatest.{FunSuite, MustMatchers}
import org.silkframework.learning.active.ActiveLearningFactory
import org.silkframework.rule.LinkFilter
import org.silkframework.runtime.plugin.AnyPlugin
import org.silkframework.util.ScalaReflectUtils

class LinkageRuleLearnerTest extends FunSuite with MustMatchers {

  test ("must get current and parent package") {
    ScalaReflectUtils.getPackageName(this.getClass) mustBe "org.silkframework.learning"

    val zw = ScalaReflectUtils.getEnclosingPackage(this.getClass)
    zw.fullName mustBe "org.silkframework.learning"
    ScalaReflectUtils.getEnclosingPackage(zw.owner).fullName mustBe "org.silkframework"
  }

  Seq(this.getClass, LinkFilter.getClass).foreach(cls =>{
    test("test for build info for class " + cls.getName){
      ScalaReflectUtils.findClosestBuildInfo(cls) match{
        case Some(x) =>
          ScalaReflectUtils.listMembers(x).keySet.contains("gitHeadCommit") mustBe true
        case None => fail()
      }}
   })

  test ("generate a unique uri for each instance of AnyPlugin") {
    val iri = new ActiveLearningFactory().pluginIri
    iri.startsWith("urn:org:silkframework:learning:active:ActiveLearningFactory:") mustBe true
    val parts = iri.split(":").reverse
    parts.head mustBe BuildInfo.gitHeadCommit.get
    parts.tail.head mustBe BuildInfo.version

  }
}
