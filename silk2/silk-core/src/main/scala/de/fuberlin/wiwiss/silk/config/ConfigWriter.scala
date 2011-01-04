package de.fuberlin.wiwiss.silk.config

import xml.Elem
import de.fuberlin.wiwiss.silk.linkspec._
import input.{Transformer, Input, PathInput, TransformInput}
import de.fuberlin.wiwiss.silk.output.{LinkWriter, Output}
import de.fuberlin.wiwiss.silk.datasource.{Source, DataSource}

/**
 * Writes a Silk Configuration.
 */
//TODO move methods to the respective classes (e.g. configuration.toXML)?
object ConfigWriter
{
    def serializeConfig(config : Configuration) =
    {
        <Silk>
          <Prefixes>
            { config.prefixes.map{case (id, ns) => <Prefix id={id} namespace={ns} /> } }
          </Prefixes>
          <DataSources>
            { config.sources.map(serializeSource) }
          </DataSources>
          <Interlinks>
            { config.linkSpecs.map(serializeLinkSpec) }
          </Interlinks>
        </Silk>
    }

    def serializeSource(source : Source) = source.dataSource match
    {
        case DataSource(dataSourceType, params) =>
        {
            <DataSource id={source.id} type={dataSourceType}>
              { params.map{case (name, value) => <Param name={name} value={value} />} }
            </DataSource>
        }
    }

    def serializeLinkSpec(linkSpec : LinkSpecification) =
    {
        <Interlink id={linkSpec.id}>
          <LinkType>{"<" + linkSpec.linkType + ">"}</LinkType>

          <SourceDataset dataSource={linkSpec.datasets.source.source.id} var={linkSpec.datasets.source.variable}>
            <RestrictTo>{linkSpec.datasets.source.restriction}</RestrictTo>
          </SourceDataset>

          <TargetDataset dataSource={linkSpec.datasets.target.source.id} var={linkSpec.datasets.target.variable}>
            <RestrictTo>{linkSpec.datasets.target.restriction}</RestrictTo>
          </TargetDataset>

          { serializeLinkCondition(linkSpec.condition) }

          { serializeLinkFilter(linkSpec.filter) }

          <Outputs>
             { linkSpec.outputs.map(serializeOutput) }
          </Outputs>
        </Interlink>
    }

    def serializeLinkCondition(linkCondition : LinkCondition) =
    {
        <LinkCondition>
            { serializeOperator(linkCondition.rootAggregation) }
        </LinkCondition>
    }

    def serializeOperator(operator : Operator) : Elem = operator match
    {
        case Aggregation(required, weight, operators, Aggregator(aggregator, params)) =>
        {
            <Aggregate required={required.toString} weight={weight.toString} type={aggregator}>
              { operators.map(serializeOperator) }
            </Aggregate>
        }
        case Comparison(required, weight, inputs, Metric(metric, params)) =>
        {
            <Compare required={required.toString} weight={weight.toString} metric={metric}>
              { serializeInput(inputs.source) }
              { serializeInput(inputs.target) }
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
        case LinkWriter(outputType, params) =>
        {
            <Output type={outputType}>
              { params.map{case (name, value) => <Param name={name} value={value} /> } }
            </Output>
        }
    }
}
