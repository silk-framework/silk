import javax.inject.{Inject, Provider, Singleton}

import org.silkframework.workbench.utils.SilkErrorHandler
import play.api.routing.Router
import play.api.{Configuration, Environment, OptionalSourceMapper}

/**
  * Error handler used for testing.
  */
@Singleton
class ErrorHandler @Inject() (env: Environment,
                              config: Configuration,
                              sourceMapper: OptionalSourceMapper,
                              router: Provider[Router]) extends SilkErrorHandler(env, config, sourceMapper, router)
