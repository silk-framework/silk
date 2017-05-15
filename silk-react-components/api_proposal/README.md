
## Retrieval:

-   `GET /transform/tasks/{project}/{transformationTask}/rules`:

    [Example response with comments ./retrieval.yml](./retrieval.yml)

    [Example response: ./retrieval.json](./retrieval.json)

## Filter

Maybe we can release the tree structure with a simple filter?

-   `GET /transform/tasks/{project}/{transformationTask}/rules?type=hierarchical`:

    Based on the example from above:

    [Example response: ./retrieval_hierarch.json](./retrieval_hierarch.json)

## Delete

- `DELETE /transform/tasks/{project}/{transformationTask}/rule/{ruleID}`

## Create

- `POST /transform/tasks/{project}/{transformationTask}/rule/{rule}/rules`

    Payload:
    ```json
    {
      "type": "direct",
      "name": "sourcePath2",
      "comment": "foobar",
      "sourcePath": "/<http://www.w3.org/2000/01/rdf-schema#foobar>",
      "mappingTarget": {
        "URI": "http://www.w3.org/2000/01/rdf-schema#oxofrmbl",
        "valueType": {
          "nodeType": "AutoDetectValueType"
        }
      }
    }
    ```

    Return:

## Update

I would realize the update of rules like already mentioned:
- `PUT /transform/tasks/{project}/{transformationTask}/rule/{ruleID}`

    Before:

    ```json
    {
      "id": "XYZ_2",
      "type": "direct",
      "name": "sourcePath2",
      "comment": "foobar",
      "sourcePath": "/<http://www.w3.org/2000/01/rdf-schema#label>",
      "mappingTarget": {
        "URI": "http://www.w3.org/2000/01/rdf-schema#label2",
        "valueType": {
          "nodeType": "AutoDetectValueType"
        }
      }
    }
    ```

    Payload maybe as [JSON merge patch](https://tools.ietf.org/html/rfc7396) (`Mime: application/merge-patch+json`):

    `PUT (or PATCH?) /transform/tasks/{project}/{transformationTask}/rule/XYZ_2`:

    ```json
    {
        "comment": null,
        "sourcePath": "/<http://xmlns.com/foaf/0.1/knows>"
    }
    ```

    Result:

    ```json
    {
      "id": "XYZ_2",
      "type": "direct",
      "name": "sourcePath2",
      "sourcePath": "/<http://xmlns.com/foaf/0.1/knows>",
      "mappingTarget": {
        "URI": "http://www.w3.org/2000/01/rdf-schema#label2",
        "valueType": {
          "nodeType": "AutoDetectValueType"
        }
      }
    }
    ```

## Reorder

I would realize the reordering of rules like already mentioned:

`PUT /transform/tasks/{project}/{transformationTask}/reorder/{rule}`:

Hierarchy from example above:

```
- XYZ
    - XYZ_1
    - XYZ_2
    - XYZ_3
    - XYZ_4
        - XYZ_5
        - XYZ_6
```

If i want to change the order in `XYZ_4` (Address), i would do:

`PUT /transform/tasks/{project}/{transformationTask}/reorder/XYZ_4`

```json
[
    "XYZ_6",
    "XYZ_5",
]
```

If i want to change the order in `XYZ` (Person), i would do:

`PUT /transform/tasks/{project}/{transformationTask}/reorder/XYZ`

```json
[
    "XYZ_4",
    "XYZ_1",
    "XYZ_2",
    "XYZ_3",
]
```

This would result in:

```
- XYZ
    - XYZ_4
        - XYZ_6
        - XYZ_5
    - XYZ_1
    - XYZ_2
    - XYZ_3
```


## Search

TODO:
