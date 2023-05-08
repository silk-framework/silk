package org.silkframework.openapi

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class OpenApiGeneratorTest extends AnyFlatSpec with Matchers {

  "OpenApiGenerator" should "sort paths hierarchically" in {
    val paths =
      Seq(
        "/api/workspace/projects/{project}/metadata",
        "/api/workspace/projects/{project}/metadata/addTag",
        "/api/workspace/projects/{project}/metadataExpanded",
        "/workspace/projects/{project}/tags/newTag",
        "/workspace/projects/{project}/tags"
      )

    paths.sortWith(OpenApiGenerator.comparePaths) shouldBe Seq(
      "/api/workspace/projects/{project}/metadata",
      "/api/workspace/projects/{project}/metadataExpanded",
      "/api/workspace/projects/{project}/metadata/addTag",
      "/workspace/projects/{project}/tags",
      "/workspace/projects/{project}/tags/newTag"
    )
  }


}
