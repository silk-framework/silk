package org.silkframework.openapi

import org.scalatest.{FlatSpec, Matchers}

class OpenApiGeneratorTest extends FlatSpec with Matchers {

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
