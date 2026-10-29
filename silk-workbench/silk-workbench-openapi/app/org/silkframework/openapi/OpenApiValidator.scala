package org.silkframework.openapi

import com.networknt.schema.dialect.Dialects
import com.networknt.schema.{ExecutionContext, InputFormat, SchemaRegistry}
import config.WorkbenchConfig
import io.aurora.utils.play.swagger.{ApiListingCache, PlayApiScanner, PlaySwaggerConfig, RouteWrapper, SwaggerPlugin}
import io.swagger.v3.parser.OpenAPIV3Parser
import io.swagger.v3.parser.core.models.ParseOptions
import controllers.openapi.routes.OpenApi
import play.api.libs.json.Json
import play.api.mvc.RequestHeader

import java.util
import scala.jdk.CollectionConverters.CollectionHasAsScala

/**
  * Validates OpenAPI specifications.
  * Currently tailored for the Swagger UI validation calls, which may send an arbitrary URL.
  * Code is based on https://github.com/swagger-api/validator-badge.
  */
object OpenApiValidator {

  def validate(swaggerPlugin: SwaggerPlugin, url: Option[String] = None)(implicit request: RequestHeader): ValidationResult = {
    url match {
      case Some(u) if u == OpenApi.openApiJson.absoluteURL(WorkbenchConfig().useHttps) =>
        validateLocal(OpenApiGenerator.generateJson(swaggerPlugin), validateJsonSchema = true)
      case Some(u) if u == OpenApi.openApiYaml.absoluteURL(WorkbenchConfig().useHttps) =>
        validateLocal(OpenApiGenerator.generateYaml(swaggerPlugin), validateJsonSchema = false)
      case Some(u) =>
        validateRemote(u)
      case None =>
        validateCurrentSpec(swaggerPlugin)
    }
  }

  def validateCurrentSpec(swaggerPlugin: SwaggerPlugin): ValidationResult = {
    validateLocal(OpenApiGenerator.generateJson(swaggerPlugin), validateJsonSchema = true)
  }

  // Validate our own spec
  private def validateLocal(contents: String, validateJsonSchema: Boolean): ValidationResult = {
    val parser = new OpenAPIV3Parser()
    val parseResult = parser.readContents(contents)
    val parserMessages = parseResult.getMessages.asScala.toSeq
    val schemaMessages = if(validateJsonSchema) validateSchema(contents) else Seq.empty
    ValidationResult(parserMessages ++ schemaMessages)
  }

  // Validate remote spec
  private def validateRemote(url: String): ValidationResult = {
    val parser = new OpenAPIV3Parser()
    val parseOptions = new ParseOptions()
    val result = parser.readLocation(url, new util.ArrayList(), parseOptions)
    ValidationResult(result.getMessages.asScala.toSeq)
  }

  private def validateSchema(contents: String): Seq[String] = {
    val schemaStream = getClass.getClassLoader.getResourceAsStream("openApiSchemaV3.json")
    val schema = SchemaRegistry.withDialect(Dialects.getDraft4()).getSchema(schemaStream, InputFormat.JSON)
    val noopCustomizer: java.util.function.Consumer[ExecutionContext] = _ => ()
    val validationResult = schema.validate(contents, InputFormat.JSON, noopCustomizer)
    for(validationMessage <- validationResult.asScala.toSeq) yield {
      validationMessage.getMessage
    }
  }

}
