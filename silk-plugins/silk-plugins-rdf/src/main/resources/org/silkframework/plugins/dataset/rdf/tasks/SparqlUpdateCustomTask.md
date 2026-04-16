The SPARQL UPDATE query plugin is a task for outputting SPARQL UPDATE queries from the input RDF data source.

## Description

The SPARQL Update query plugin is an example of a _task_. Notice well that this plugin is neither a _RDF task_
nor a _RDF dataset_. This is in contrast to e.g. the SPARQL Select query and the SPARQL endpoint, respectively.

More specifically, this means the following: This plugin does not execute SPARQL queries of any sort, but _generates_
them. It generates [SPARQL Update](https://www.w3.org/TR/sparql11-update/) queries from a templating engine. In order to
_execute_ these queries, we need to connect this task from an input into an output RDF dataset.

## Templating

The SPARQL Update query plugin uses a template in order to construct and output SPARQL update queries.
Three template engines are supported: `Jinja` (the default), `Simple`, and
[`Velocity Engine`](https://velocity.apache.org/engine/2.4.1/user-guide.html).
The `Simple` and `Velocity Engine` modes are deprecated.

### Example of the `Jinja` mode

[Jinja](https://jinja.palletsprojects.com/) is the recommended template engine. It uses `{{ }}` for expressions and
`{% %}` for control flow statements such as conditionals.

```
DELETE DATA { {{ row.uri("PROP_FROM_ENTITY_SCHEMA1") }} rdf:label {{ row.plainLiteral("PROP_FROM_ENTITY_SCHEMA2") }} } ;
{% if row.exists("PROP_FROM_ENTITY_SCHEMA1") %}
  INSERT DATA { {{ row.uri("PROP_FROM_ENTITY_SCHEMA1") }} rdf:label {{ row.plainLiteral("PROP_FROM_ENTITY_SCHEMA3") }} } ;
{% endif %}
```

Input values are accessible via methods on the `row` variable:

- `row.uri(inputPath)`: Renders an input value as **URI**. Throws an exception if the value isn't a valid URI.
- `row.plainLiteral(inputPath)`: Renders an input value as **plain literal**, i.e. it escapes problematic characters.
- `row.rawUnsafe(inputPath)`: Renders an input value as is, i.e. **no escaping** is done. This should **only** be used if the input values can be trusted.
- `row.exists(inputPath)`: Returns `true` if a value for the input path **exists**, else `false`.

The methods `uri`, `plainLiteral` and `rawUnsafe` throw an exception if no input value is available for the given input path.

In addition to input values, properties of the input and output tasks can be accessed via the `inputProperties` and
`outputProperties` objects in the same way as the `row` object. For example with `{{ inputProperties.uri("graph") }}`.

### Example of the `Simple` mode (deprecated)

```
  DELETE DATA { ${<PROP_FROM_ENTITY_SCHEMA1>} rdf:label ${"PROP_FROM_ENTITY_SCHEMA2"} }
  INSERT DATA { ${<PROP_FROM_ENTITY_SCHEMA1>} rdf:label ${"PROP_FROM_ENTITY_SCHEMA3"} }
```

This will insert the URI serialization of the property value `PROP_FROM_ENTITY_SCHEMA1` for the
`${<PROP_FROM_ENTITY_SCHEMA1>}` expression.
Furthermore, it will insert a plain literal serialization for the property values `PROP_FROM_ENTITY_SCHEMA2` and
`PROP_FROM_ENTITY_SCHEMA3` for the template literal expressions.

It is also possible to write something like `${"PROP"}^^<http://someDatatype>`  or `${"PROP"}@en`. In other words, we
can combine variable substitutions with fixed expressions to construct semi-flexible expressions within the template.

### Example of the `Velocity Engine` mode (deprecated)

```
  DELETE DATA { $row.uri("PROP_FROM_ENTITY_SCHEMA1") rdf:label $row.plainLiteral("PROP_FROM_ENTITY_SCHEMA2") }
  #if ( $row.exists("PROP_FROM_ENTITY_SCHEMA1") )
    INSERT DATA { $row.uri("PROP_FROM_ENTITY_SCHEMA1") rdf:label $row.plainLiteral("PROP_FROM_ENTITY_SCHEMA3") }
  #end
```

Input values are accessible via various methods of the `row` variable (used with `$row`):

- `$row.uri(inputPath: String)`: Renders an input value as **URI**. Throws an exception if the value isn't a valid URI.
- `$row.plainLiteral(inputPath: String)`: Renders an input value as **plain literal**, i.e. it escapes problematic
  characters, etc.
- `$row.rawUnsafe(inputPath: String)`: Renders an input value as is, i.e. **no escaping** is done.
  This should **only** be used if the input values can be trusted.
- `$row.exists(inputPath: String)`: Returns `true` if a value for the input path **exists**, else `false`.

The methods `uri`, `plainLiteral` and `rawUnsafe` throw an exception if no input value is available for the given
input path.

In addition to input values, properties of the input and output tasks can be accessed via the `inputProperties` and
`outputProperties` objects. The available keys in these objects are dynamic and correspond exactly to the configuration
parameters of the tasks connected to the input and output ports of this operator.

- To find the available keys for `$inputProperties`, check the parameter names of the task connected to the input port.
- To find the available keys for `$outputProperties`, check the parameter names of the task connected to the output port.

For example, if the connected input task has a parameter named `graph`, you can access it as `$inputProperties.uri("graph")`.
Similarly, if the connected output task has a parameter named `endpoint`, you can access it as `$outputProperties.uri("endpoint")`.

Both `inputProperties` and `outputProperties` support the same methods as the `row` object:

- `uri(inputPath: String)`
- `plainLiteral(inputPath: String)`
- `rawUnsafe(inputPath: String)`
- `exists(inputPath: String)`

For more information about the Velocity Engine, visit http://velocity.apache.org.

### Internal Specifics

In contrast to the SPARQL select operator, no `FROM` clause gets injected into the query.
