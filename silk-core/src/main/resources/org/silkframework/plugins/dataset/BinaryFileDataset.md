Reads and writes binary files. A typical use-case for this dataset is to process PDF documents or images using workflow operators that accept or output files. If an operator reads from this dataset that does not support files directly (such as transformation or linking tasks), it will only receive the file metadata, which includes the file path.

## ZIP files

This dataset can be used to compress/decompress ZIP files. If a ZIP file is configured, the behaviour is as follows:
- Writing a ZIP file to this dataset will overwrite the configured ZIP file.
- Writing one or many non-ZIP files will overwrite the dataset file with a ZIP that contains all written files.
- When reading files, the dataset will return all files inside the ZIP that match the configured regex. If the regex is empty, the ZIP file itself will be returned.

## Replaceable datasets

It can be used with the `replacable input` flag to replace the configured file in a workflow execution request.
Same for the `replacable output` flag, which will return the file content as a result of a workflow execution request.

## MIME type

The generic MIME type for files of this dataset is `application/octet-stream`.