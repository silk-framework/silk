# Silk Link Specificsation Language

The Silk framework provides a declarative language for specifying which types of RDF links should be discovered between data sources as well as which conditions data items must fulfill in order to be interlinked. This section describes the language constructs of the Silk Link Specification Language (Silk-LSL).

The example below gives an overview of the main language constructs of Silk-LSL.

    <Silk>
    
      <Prefixes>
        <Prefix id="rdfs" namespace="http://www.w3.org/2000/01/rdf-schema#" />
        <Prefix id="dbpedia" namespace="http://dbpedia.org/ontology/" />
        <Prefix id="gn" namespace="http://www.geonames.org/ontology#" />
      </Prefixes>
    
      <DataSources>
        <DataSource id="dbpedia">
          <Param name="endpointURI" value="http://demo_sparql_server1/sparql" />
          <Param name="graph" value="http://dbpedia.org" />
        </DataSource>
    
        <DataSource id="geonames">
          <Param name="endpointURI" value="http://demo_sparql_server2/sparql" />
          <Param name="graph" value="http://sws.geonames.org/" />
        </DataSource>
      </DataSources>
      
      [<Blocking blocks="100" />]
    
      <Interlinks>
        <Interlink id="cities">
          <LinkType>owl:sameAs</LinkType>
    
          <SourceDataset dataSource="dbpedia" var="a">
            <RestrictTo>
              ?a rdf:type dbpedia:City
            </RestrictTo>
          </SourceDataset>
    
          <TargetDataset dataSource="geonames" var="b">
            <RestrictTo>
              ?b rdf:type gn:P
            </RestrictTo>
          </TargetDataset>
    
          <LinkageRule>
            <Aggregate type="average">
              <Compare metric="levenshteinDistance" threshold="1">
                <Input path="?a/rdfs:label" />
                <Input path="?b/gn:name" />
              </Compare>
              <Compare metric="num" threshold="1000" >
                <Input path="?a/dbpedia:populationTotal" />
                <Input path="?b/gn:population" />
              </Compare>
            </Aggregate>
          </LinkageRule>
    
          <Filter limit="1" />
    
          <Outputs>
            <Output id="output_accepted" />
            <Output id="output_verify" />
          </Outputs>
        </Interlink>
        
      </Interlinks>
      
      <Outputs>
        <Output id="output_accepted" type="file" minConfidence="0.95">
          <Param name="file" value="accepted_links.nt" />
          <Param name="format" value="ntriples" />
        </Output>
        <Output id="output_verify" type="file" maxConfidence="0.95">
          <Param name="file" value="verify_links.nt" />
          <Param name="format" value="alignment" />
        </Output>
      </Outputs>

    </Silk>
    
## Structure and Elements
     
The Silk-LSL is expressed in XML as specified by the corresponding Silk XML Schema. The root tag name is @<Silk>@. A valid document may contain four types of top-level statements beneath the root element:
- prefix definitions
- datasource definitions
- link specifications
- output definitions
     
    <?xml version="1.0" encoding="utf-8" ?>
    <Silk>
        <Prefixes ... />
          ...
        <DataSources ... />
          ...
        [<Blocking ... />]  
          ...
        <Interlinks ... />
          ...
        [<Outputs ... />]
          ...
    </Silk>
      
The Blocking and Outputs statements are optional.
      
### Prefix Definitions

Prefix definitions are top-level statements that allow the binding of a prefix to a namespace:

    <Prefixes>
      <Prefix id="prefix id" namespace="namespace URI" />
    </Prefixes>

Example:

    <Prefixes>
      <Prefix id="rdf" namespace="http://www.w3.org/1999/02/22-rdf-syntax-ns#" />
    </Prefixes>

### Data Source Definitions

Data source definitions are top-level statements that allow the specification of access parameters to local or remote SPARQL endpoints. The defined data sources may later be referred to and used by their ID within link specification statements.

    <DataSources>
      <DataSource id="data source ID" type="dataSource type">
        <Param name="parameter name" value="parameter value" />
        ...
      </DataSource>
    </DataSources>

### Blocking Data Items

Since comparing every source resource to every single target resource results in a number of n*m comparisons (n being the number of source resources, m the number of target resources) which might be too time consuming, blocking can be used to reduce the number of comparisons. Blocking partitions similar data items into clusters reducing the comparison to items in the same cluster.

For example given two datasets describing books, in order to reduce the number of comparisons, we could block the books by publisher. In this case only books from the same publisher will be compared. Given a number of 40.000 books in the first dataset and 30.000 in the second dataset, evaluating the full Cartesian product requires 1.2 billion comparisons.

If we block this datasets by publisher, each book will be allocated to a block based on its publisher. Using 100 blocks, if the books are uniformly distributed, there will be 400 respectively 300 books per block, which reduces the number of comparisons to 12 million.
The @<Blocking>@ statement enables the blocking phase. Additional configuration is not required as Silk will automatically generate a blocking function from the link specification. The optional @blocks@ attribute specifies the number of blocks which will be used. The default value of 100 blocks is appropriate for most use cases. If no @<Blocking>@ statement is supplied in a link specification, the comparison will loop over all resource pairs (Cartesian product).

### Link Specifications

Link specification statements state that a link of a given type should be established between two data items if a specified condition is satisfied. This condition may contain different similarity metrics, aggregation and transformation functions, thresholds and weights.

A Silk linking configuration may contain several link specifications if different types of links should be generated. Link specifications are structured as follows:

    <Interlinks>
      <Interlink id="interlink id">
        <LinkType>link type URI</LinkType>
        <SourceDataset dataSource="dataSource id" var="resource variable name">
            [<RestrictTo>SPARQL restriction</RestrictTo>]
        </SourceDataset>
        <TargetDataset dataSource="dataSource id" var="resource variable name">
            [<RestrictTo>SPARQL restriction</RestrictTo>]
        </TargetDataset>
        </TargetDataset>
        <LinkageRule>
            <Aggregate type="average|max|min|...">
                <Compare metric="similarity metric">
                    <Input path="RDF path" />
                    <TransformInput function="transform function name">
                      <Input path="RDF path" />
                    </TransformInput>
                    <Param name="parameter name" value="literal value" />
                </Compare>
                <Compare ...>
                </Compare>
                ...
            </Aggregate>
        </LinkageRule>
    
        <Filter limit="link limit" />
    
        <Outputs>
          <Output id="myOutput">
        </Outputs>
      </Interlink>
      <Interlink id="...">
        ...
      </Interlink>  
      <Outputs>
        <Output id="myOutput" type="output type" minConfidence="lower threshold" maxConfidence="upper threshold">
          <Param name="parameter name" value="parameter value" />
            ...
        </Ouput>
      </Outputs>
    </Interlinks>

Example:

    <Interlinks>
      <Interlink id="countries">
        <LinkType>owl:sameAs</LinkType>
        <SourceDataset dataSource="dbpedia" var="a">
            <RestrictTo>
                ?a rdf:type dbpedia:Country
            </RestrictTo>
        </SourceDataset>
        <TargetDataset dataSource="factbook" var="b">
        </TargetDataset>
        <LinkageRule>
            <Aggregate type="average">
                <Compare metric="levenstheinDistance" weight="2" threshold="1" >
                    <Input path="?a/rdfs:label" />
                    <Input path="?b/rdfs:label" />
                </Compare>
                <Compare metric="num" threshold="1000" >
                    <Input path="?a/dbpedia:population" />
                    <Param path="?b/fb:totalPopulation" />
                    <Param name="factor" value="2" />
                </Compare>
            </Aggregate>
        </LinkageRule>
        <Filter limit="1" />
        <Outputs>
          <Output type="file" maxConfidence="0.9" >
            <Param name="file" value="verify_links.nt"/>
            <Param name="format" value="ntriples"/>
          </Output>
          <Output type="file" minConfidence="0.9">
            <Param name="file" value="accepted_links.nt"/>
            <Param name="format" value="ntriples"/>
          </Output>
        </Outputs>
      </Interlink>
    </Interlinks>

#### Link Type

The LinkType directive defines the type of the generated links.

Example:

    <LinkType>owl:sameAs</LinkType>

#### Datasets

The SourceDataset and TargetDataset directives define the set of data items which are to be compared. The entitites which are to be interlinked are selected by providing a restriction for each data source. In its simplest form a restriction just selects all entities of a specific type inside the data source.  For instance, in order to interlink cities in DBpedia, a valid restriction may select all entities with the type dbpedia:City. For more complex restrictions, arbritray SPARQL triple patterns are allowed to be specified.

| Parameter  | Description                                         |
|------------|-----------------------------------------------------|
| dataSource | Selects a previously defined data source by its id. |
| var        | Binds each data item to this variable.              |
| RestrictTo | Restricts this dataset using SPARQL clauses.        |


    <SourceDataset dataSource="dataSource id" var="resource variable name">
      [<RestrictTo>SPARQL restriction</RestrictTo>]
    </SourceDataset>
    <TargetDataset dataSource="dataSource id" var="resource variable name">
      [<RestrictTo>SPARQL restriction</RestrictTo>]
    </TargetDataset>

Example:

    <SourceDataset dataSource="dbpedia" var="a">
      <RestrictTo>
        ?a rdf:type dbpedia:Country
      </RestrictTo>
    </SourceDataset>

#### Linkage Rule

The linkage rule specifies how two data items are compared for similarity. Refer to [[Linkage Rule]] for details.

#### Link Filter

The Link Filter allows for filtering the generated links. It only has a single parameter:

@limit@ defines the number of links originating from a single data item. Only the n highest-rated links per source data item will remain after the filtering. If no limit is provided, all links will be returned.

*Examples*

 A limit of 1 link per entity:
 
    <Filter limit="1" />

No limit:

    <Filter />  