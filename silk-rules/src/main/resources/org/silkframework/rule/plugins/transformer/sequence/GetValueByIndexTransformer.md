Returns the value found at the specified index.

## Description

A negative index counts from the end of the sequence, e.g. -1 returns the last value. Fails or returns an empty
result depending on failIfNotFound is set or not. Please be aware that this will work only if the data source
supports some kind of ordering like XML or JSON. This is probably not a good idea to do with RDF models.

If emptyStringToEmptyResult is true then instead of a result with an empty String, an empty result is returned.
