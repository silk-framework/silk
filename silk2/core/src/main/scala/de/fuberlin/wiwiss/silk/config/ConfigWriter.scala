package de.fuberlin.wiwiss.silk.config

import xml.{PrettyPrinter, Elem}
import java.io.{OutputStreamWriter, FileOutputStream, OutputStream, File}
import de.fuberlin.wiwiss.silk.output.Output
import de.fuberlin.wiwiss.silk.linkspec._
import de.fuberlin.wiwiss.silk.datasource.DataSource

object ConfigWriter
{
    def write(config : Configuration, file : File)
    {
        val fileStream = new FileOutputStream(file)
        try
        {
            write(config, fileStream)
        }
        finally
        {
            fileStream.close()
        }
    }

    def write(config : Configuration, outputStream : OutputStream)
    {
        val printer = new PrettyPrinter(140, 2)
        val writer = new OutputStreamWriter(outputStream)
        val xml = serialize(config)

        writer.write(printer.format(xml))
        writer.write("\n")
        writer.flush()
    }

    def serialize(config : Configuration) =
    {
        <Silk>
          <Prefixes>
            { config.prefixes.map{case (id, ns) => <Prefix id={id} namespace={ns} /> } }
          </Prefixes>
          <DataSources>
            { config.dataSources.map{case (id, ds) => datasourceToXML(id, ds) } }
          </DataSources>
          <Interlinks>
            { config.linkSpecs.map{case (id, ds) => linkSpecToXml(id, ds) } }
          </Interlinks>
        </Silk>
    }

    private def datasourceToXML(id : String, dataSource : DataSource) = dataSource match
    {
        case DataSource(dataSourceType, params) =>
        {
            <DataSource id={id} type={dataSourceType}>
              { params.map{case (name, value) => <Param name={name} value={value} />} }
            </DataSource>
        }
    }

    private def linkSpecToXml(id : String, linkSpec : LinkSpecification) =
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
              { operatorToXml(linkSpec.condition) }
          </LinkCondition>

          { linkFilterToXml(linkSpec.filter) }

          <Outputs>
             { linkSpec.outputs.map(outputToXml) }
          </Outputs>
        </Interlink>
    }

    private def operatorToXml(operator : Operator) : Elem = operator match
    {
        case Aggregation(weight, operators, Aggregator(aggregator, params)) =>
        {
            <Aggregate weight={weight.toString} type={aggregator}>
              { operators.map(operatorToXml) }
            </Aggregate>
        }
        case Comparison(weight, inputs, Metric(metric, params)) =>
        {
            <Compare weight={weight.toString} metric={metric}>
              { inputs.map{input => inputToXml(input)} }
              { params.map{case (name, value) => <Param name={name} value={value} />} }
            </Compare>
        }
    }

    private def inputToXml(param : Input) : Elem = param match
    {
        case PathInput(path) => <Input path={path.toString} />
        case TransformInput(inputs, Transformer(transformer, params)) =>
        {
            <TransformInput function={transformer}>
              { inputs.map{input => inputToXml(input)} }
              { params.map{case (name, value) => <Param name={name} value={value} />} }
            </TransformInput>
        }
    }

    private def linkFilterToXml(filter : LinkFilter) : Elem = filter.limit match
    {
        case Some(limit) => <Filter threshold={filter.threshold.toString} limit={limit.toString} />
        case None => <Filter threshold={filter.threshold.toString} />
    }

    private def outputToXml(output : Output) : Elem = output match
    {
        case Output(outputType, params) =>
        {
            <Output type={outputType}>
              { params.map{case (name, value) => <Param name={name} value={value} /> } }
            </Output>
        }
    }
}