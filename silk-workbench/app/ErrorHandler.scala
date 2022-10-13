import config.WorkbenchConfig.WorkspaceReact

import javax.inject.{Inject, Provider, Singleton}
import org.silkframework.workbench.utils.SilkErrorHandler
import play.api.routing.Router
import play.api.{Configuration, Environment, OptionalSourceMapper}

import scala.concurrent.ExecutionContext

@Singleton
class ErrorHandler @Inject() (env: Environment,
                              config: Configuration,
                              sourceMapper: OptionalSourceMapper,
                              router: Provider[Router],
                              executionContext: ExecutionContext,
                              workspaceReact: WorkspaceReact) extends SilkErrorHandler(env, config, sourceMapper, router, executionContext, workspaceReact)
