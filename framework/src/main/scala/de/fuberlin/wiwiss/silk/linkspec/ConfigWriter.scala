package de.fuberlin.wiwiss.silk

import datasource.DataSource
import linkspec._
import xml.{PrettyPrinter, Elem}
import java.io.{OutputStreamWriter, FileOutputStream, OutputStream, File}

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
          { dataSource.params.map{case (name, value) => <param name={name} value={value} />} }
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
              { operatorToXml(linkSpec.linkConditions) }
          </LinkCondition>

          <Thresholds accept={linkSpec.acceptThreshold.toString} verify={linkSpec.verifyThreshold.toString} />
          <!-- Limit max={} method={} / -->

          <Outputs>
             { linkSpec.outputs.map(outputToXml) }
          </Outputs>
        </Interlink>
    }

    private def operatorToXml(operator : Operator) : Elem = operator match
    {
        case aggregation : Aggregation =>
        {
            <Aggregate type={aggregation.getClass.getSimpleName}>
              { aggregation.operators.map(operatorToXml) }
            </Aggregate>
        }
        case metric : Metric =>
        {
            <Compare metric={metric.getClass.getSimpleName}>
              { metric.params.map{case (name, param) => paramToXml(name, param)} }
            </Compare>
        }
    }

    private def paramToXml(name : String, param : AnyParam) : Elem = param match
    {
        case p : Param => <Param name={name} value={p.value} />
        case p : PathParam => <PathParam name={name} path={p.path.toString} />
        case p : TransformParam =>
        {
            <TransformParam name={name} function={p.getClass.getSimpleName}>
              { p.params.map{case (name, param) => paramToXml(name, param)} }
            </TransformParam>
        }
    }

    private def outputToXml(output : Output) : Elem =
    {
        <Output type={output.getClass.getSimpleName}>
          { output.params.map{case (name, value) => <Param name={name} value={value} /> } }
        </Output>
    }
}