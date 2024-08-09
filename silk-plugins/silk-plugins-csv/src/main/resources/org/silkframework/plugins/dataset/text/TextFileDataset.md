Reads and writes plain text files.

## Writing

All values of each entity will be written as plain text. Multiple values per entity are separated by spaces. Each entity will be written to a new line. 

## Reading

The entire text will be read as a single entity with a single property. Note that even if multiple entities have been written to this dataset before, those would still be read back as a single entity. The default type is `document`, the default path is `text`. Both values can be configured in the advanced section.
