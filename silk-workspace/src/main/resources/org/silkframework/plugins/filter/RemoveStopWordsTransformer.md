The stop word list is specified as a resource, e.g. a file identical to
[this German stop word list](https://raw.githubusercontent.com/stopwords-iso/stopwords-de/refs/heads/master/stopwords-de.txt).

Such a stop word list resource is useful, for instance, to specify the stop words of a specific language or
application domain.

Regardless of the stop word list used, the following comments apply:

* Each line in the stop word list should contain a single stop word.
* The removal of stop words is case-sensitive. For example, 'The' and 'the' are considered the same.
* The separator defines a regular expression (regex) that is used for detecting words.
* By default, the separator is a regular expression for non-whitespace characters.

Additionally, notice the simpler filter 'removeDefaultStopWords', which uses a default stop word list.
