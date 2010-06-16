package de.fuberlin.wiwiss.silk.config

import xml.{PrettyPrinter, Elem}
import java.io.{OutputStreamWriter, FileOutputStream, OutputStream, File}
import de.fuberlin.wiwiss.silk.output.Output
import de.fuberlin.wiwiss.silk.linkspec._
import de.fuberlin.wiwiss.silk.datasource.DataSource

//TODO write correct types
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
            { config.dataSources.map{case (name, ds) => datasourceToXML(name, ds) } }
          </DataSources>
          <Interlinks>
            { config.linkSpecs.map{case (name, ds) => linkSpecToXml(name, ds) } }
          </Interlinks>
        </Silk>
    }

    private def datasourceToXML(name : String, dataSource : DataSource) =
    {
        <DataSource id={name} type={dataSource.getClass.getSimpleName}>
          { dataSource.params.map{case (name, value) => <Param name={name} value={value} />} }
        </DataSource>
    }

    private def linkSpecToXml(name : String, linkSpec : LinkSpecification) =
    {
        <Interlink>
          <LinkType>{linkSpec.linkType}</LinkType>

          <SourceDataset dataSource={linkSpec.sourceDatasetSpecification.dataSource.getClass.getSimpleName} var={linkSpec.sourceDatasetSpecification.variable}>
            <RestrictTo>{linkSpec.sourceDatasetSpecification.restriction}</RestrictTo>
          </SourceDataset>

          <TargetDataset dataSource={linkSpec.targetDatasetSpecification.dataSource.getClass.getSimpleName} var={linkSpec.targetDatasetSpecification.variable}>
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

    private def outputToXml(output : Output) : Elem =
    {
        <Output type={output.getClass.getSimpleName}>
          { output.params.map{case (name, value) => <Param name={name} value={value} /> } }
        </Output>
    }
}