# Overview

An path input retrieves all values which are connected to the entities by a specific path.
Every path statement begins with a variable (as defined in the datasets), which may be followed by a series of path elements. If a path cannot be resolved due to a missing property or a too restrictive filter, an empty result set is returned.

The following operators can be used to traverse the graph:

| Operator | Name             | Use                                                               | Description                                                                                                                                        |
|----------|------------------|-------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------|
| /        | forward operator | `<path_segment>/<property>`                                       | Moves forward from a subject resource (set) through a property to its object resource (set).                                                       |
|          | reverse operator | `<path_segment>\<property>`                                       | Moves backward from an object resource (set) through a property to its subject resource (set).                                                     |
| \[ \]    | filter operator  | <code><path_segment>\[<property> <comp_operator> <value>\]</code> 
                               <code><path_segment>\[@lang <comp_operator> <value>\]</code>       | Reduces the currently selected set of resources to the ones matching the filter expression. comp\_operator may be one of &gt;, <, >=, &lt;=, =, != |

# Examples

## XML**

### Select the English label of a movie

<Input path="?movie/rdfs:label[@lang = 'en']" />

### Select the label (set) of the director(s) of a movie

<Input path="?movie/dbpedia:director/rdfs:label" />

### Select the albums of a given artist (albums have an dbpedia:artist property)

<Input path="?artist\dbpedia:artist[rdf:type = dbpedia:Album]" />

## Scala API

### Select the English label of a movie

Path.parse("?movie/rdfs:label[@lang = 'en']")

### Select the label (set) of the director(s) of a movie

Path.parse("?movie/dbpedia:director/rdfs:label")

### Select the albums of a given artist (albums have an dbpedia:artist property)

Path.parse("?artist\dbpedia:artist[rdf:type = dbpedia:Album]")