
eccenca DataIntegration API.

<details>
  <summary>Security scheme</summary>

The default security scheme is OAuth 2.0.
However, this can be changed in the configuration.

If a user is not authenticated, endpoints will return HTTP error code 401.

If a user is not authorized to use eccenca DataIntegration, HTTP error code 403 will be returned.

</details>

<details>
  <summary>Media types</summary>

The default [media type](https://en.wikipedia.org/wiki/Media_type) of most responses is `application/json`.
Other possible response media types can be reached by changing the `Accept` header of the request.

Possible values of this HTTP header field are API dependent and listed as part of the specific HTTP method.

Dependent on the specific API, eccenca DataIntegration works with the following application media types which correspond to the following specification documents:

Media Type                         | Specification Document
------------------------------------|-----------------------------------------------------------------------
`application/x-www-form-urlencoded` | [HTML 4.01 Specification, Forms](https://www.w3.org/TR/html401/interact/forms.html)
`application/json` | [The JavaScript Object Notation (JSON) Data Interchange Format](https://tools.ietf.org/html/rfc8259)
`application/xml` | [XML Media Types](https://tools.ietf.org/html/rfc7303)
`application/n-triples` | [RDF 1.1 N-Triples - A line-based syntax for an RDF graph](https://www.w3.org/TR/n-triples/)
`application/problem+json` | [Problem Details for HTTP APIs](https://tools.ietf.org/html/rfc7807)

</details>

<details>
  <summary>Error responses</summary>

Unless otherwise specified, errors will be returned in the [Problem Details for HTTP APIs](https://tools.ietf.org/html/rfc7807) format.

Minimal example:

    {
      "title": "The error type"
      "detail": "Human-readable error message"
    }

For a detailed documentation of the complete error schema and a more complex example, check the `Default error format` endpoint.

</details>
