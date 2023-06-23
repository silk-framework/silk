package org.silkframework.openapi

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.networknt.schema.{JsonSchemaFactory, SpecVersion}
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
    val openApiNode = JsonMapper.builder().build().readTree(contents)
    val factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4).getSchema(schemaStream)
    val validationResult = factory.validate(openApiNode)
    for(validationMessage <- validationResult.asScala.toSeq) yield {
      validationMessage.getMessage
    }
  }

}
