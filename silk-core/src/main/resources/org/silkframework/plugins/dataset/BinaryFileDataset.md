Reads and writes binary files. A typical use-case for this dataset is to process PDF documents or images using workflow operators that accept or output files.

It can be used with the `replacable input` flag to replace the configured file in a workflow execution request.
Same for the `replacable output` flag, which will return the file content as a result of a workflow execution request.

If an operator reads from this dataset that does not support files directly (such as transformation or linking tasks), it will only receive the file metadata, which includes the file path.