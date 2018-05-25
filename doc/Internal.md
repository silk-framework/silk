# Core API

## Datasets

All dataset-related classes are located in the package `org.silkframework.dataset`.

### `DatasetSpec`

Used to represent a dataset task in a project. Extends `TaskSpec` and is on the same level as `LinkSpec`, `TransformSpec`, etc.

Currently, provides two properties

- The Dataset Plugin instance (e.g., `CsvDataset`)
- The URI-property

In the future, it can be extended with other dataset type agnostic properties.

### `Dataset`

A dataset plugin, e.g., `CsvDataset`.

In the future, should only be declarative (i.e., hold all dataset specific properties). It should not contain execution/access methods.

### `DatasetExecutor`

Executes a specific Dataset plugin. There might be multiple executors for the same dataset (e.g., for different execution types).

### `DatasetAccess`

Contains access methods to read and write data from/to a dataset.

Given a dataset, the corresponding `DatasetAccess` instance is provided by its `DatasetExecutor`. It can be retrieved using the `ExecutorRegistry`.
Multiple dataset executors may be defined for the same Dataset.
For instance, given the `CsvDataset` class, `LocalCsvDatasetExecutor` and `SparkCsvDatasetExecutor` my provide different DatasetAccess implementations.
