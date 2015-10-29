# Silk Link Discovery Framework

The Silk Link Discovery Framework is a tool for discovering relationships between data items
within different Linked Data sources. More information about Silk can be found on http://silk-framework.com.

## Requirements

- JDK 7 or later
- [Simple Build Tool](http://www.scala-sbt.org/) (sbt)

Downloading and installing sbt is not necessary as it is available from this directory. Depending on the operating system you may need to adapt the commands below to run it from the local directory, i.e., by using `./sbt` instead of `sbt`

## Running the Silk Workbench

- Execute: `sbt "project workbench" run`
- In your browser, navigate to 'http://localhost:9000'

## Packaging the Silk Workbench

- Execute: `sbt "project workbench" dist`
- The package should be available in the folder silk-workbench/target/universal

## Building a Silk Single Machine Jar

- Execute: `sbt "project singlemachine" assembly`
