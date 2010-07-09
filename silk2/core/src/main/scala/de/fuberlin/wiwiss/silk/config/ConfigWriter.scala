package de.fuberlin.wiwiss.silk.config

import xml.Elem
import de.fuberlin.wiwiss.silk.linkspec._
import de.fuberlin.wiwiss.silk.datasource.DataSource
import input.{Transformer, Input, PathInput, TransformInput}
import de.fuberlin.wiwiss.silk.output.{AlignmentWriter, Output}

//TODO move methods to the respective classes (e.g. configuration.toXML)
object ConfigWriter
{
    def serializeConfig(config : Configuration) =
    {
        <Silk>
          <Prefixes>
            { config.prefixes.map{case (id, ns) => <Prefix id={id} namespace={ns} /> } }
          </Prefixes>
          <DataSources>
            { config.dataSources.map{case (id, ds) => serializeDatasource(id, ds) } }
          </DataSources>
          <Interlinks>
            { config.linkSpecs.map{case (id, ds) => serializeLinkSpec(id, ds) } }
          </Interlinks>
        </Silk>
    }

    def serializeDatasource(id : String, dataSource : DataSource) = dataSource match
    {
        case DataSource(dataSourceType, params) =>
        {
            <DataSource id={id} type={dataSourceType}>
              { params.map{case (name, value) => <Param name={name} value={value} />} }
            </DataSource>
        }
    }

    def serializeLinkSpec(id : String, linkSpec : LinkSpecification) =
    {
        <Interlink id={id}>
          <LinkType>{linkSpec.linkType}</LinkType>

          <SourceDataset dataSource="source" var={linkSpec.sourceDatasetSpecification.variable}>
            <RestrictTo>{linkSpec.sourceDatasetSpecification.restriction}</RestrictTo>
          </SourceDataset>

          <TargetDataset dataSource="target" var={linkSpec.targetDatasetSpecification.variable}>
            <RestrictTo>{linkSpec.targetDatasetSpecification.restriction}</RestrictTo>
          </TargetDataset>

          <LinkCondition>
              { serializeOperator(linkSpec.condition.rootAggregation) }
          </LinkCondition>

          { serializeLinkFilter(linkSpec.filter) }

          <Outputs>
             { linkSpec.outputs.map(serializeOutput) }
          </Outputs>
        </Interlink>
    }

    def serializeOperator(operator : Operator) : Elem = operator match
    {
        case Aggregation(weight, operators, Aggregator(aggregator, params)) =>
        {
            <Aggregate weight={weight.toString} type={aggregator}>
              { operators.map(serializeOperator) }
            </Aggregate>
        }
        case Comparison(weight, inputs, Metric(metric, params)) =>
        {
            <Compare weight={weight.toString} metric={metric}>
              { inputs.map{input => serializeInput(input)} }
              { params.map{case (name, value) => <Param name={name} value={value} />} }
            </Compare>
        }
    }

    def serializeInput(param : Input) : Elem = param match
    {
        case PathInput(path) => <Input path={path.toString} />
        case TransformInput(inputs, Transformer(transformer, params)) =>
        {
            <TransformInput function={transformer}>
              { inputs.map{input => serializeInput(input)} }
              { params.map{case (name, value) => <Param name={name} value={value} />} }
            </TransformInput>
        }
    }

    def serializeLinkFilter(filter : LinkFilter) : Elem = filter.limit match
    {
        case Some(limit) => <Filter threshold={filter.threshold.toString} limit={limit.toString} />
        case None => <Filter threshold={filter.threshold.toString} />
    }

    //TODO write minConfidence, maxConfidence
    def serializeOutput(output : Output) : Elem = output.writer match
    {
        case AlignmentWriter(outputType, params) =>
        {
            <Output type={outputType}>
              { params.map{case (name, value) => <Param name={name} value={value} /> } }
            </Output>
        }
    }
}
