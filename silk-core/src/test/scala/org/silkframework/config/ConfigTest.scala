package org.silkframework.config

import com.google.inject.{Guice, Inject}
import com.typesafe.config.{Config => TypesafeConfig}
import net.codingwell.scalaguice.ScalaModule
import org.scalatest.{FlatSpec, MustMatchers}
/**
  * Created on 9/27/16.
  */
class ConfigTest extends FlatSpec with MustMatchers {
  behavior of "Config dependency injection"
  it should "inject into Scala objects" in {
    Guice.createInjector(new ScalaModule() {
      def configure() {
        bind[ConfigTestHelper.type].toInstance(ConfigTestHelper)
      }
    })
    ConfigTestHelper.get mustBe a[DefaultConfig]
  }

  it should "override Config with a custom test version" in {
    ConfigTestHelper.get mustBe a[DefaultConfig]
    Guice.createInjector(new ScalaModule() {
      def configure() {
        bind[Config].to[TestConfig]
        bind[ConfigTestHelper.type].toInstance(ConfigTestHelper)
      }
    })
    ConfigTestHelper.get mustBe a[TestConfig]
  }
}

class TestConfig extends Config {
  override def apply(): TypesafeConfig = null
}

object ConfigTestHelper {
  @Inject
  private val configMgr: Config = DefaultConfig.instance

  def get(): Config = configMgr
}