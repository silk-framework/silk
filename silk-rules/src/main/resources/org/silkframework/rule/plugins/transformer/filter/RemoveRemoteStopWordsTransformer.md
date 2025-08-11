The stop word list is retrieved from a remote URL such as
[this German stop word list](https://raw.githubusercontent.com/stopwords-iso/stopwords-de/refs/heads/master/stopwords-de.txt).

Such an overridable stop word list file may be used, for instance, to specify the stop words of a different
language, such as German instead of the
[default stop word list](https://gist.githubusercontent.com/rg089/35e00abf8941d72d419224cfd5b5925d/raw/12d899b70156fd0041fa9778d657330b024b959c/stopwords.txt)
for the English language.

Regardless of the stop word list used, the following comments apply:

* Each line in the stop word list should contain a single stop word.
* The removal of stop words is case-sensitive. For example, 'The' and 'the' are considered the same.
* The separator defines a regular expression (regex) that is used for detecting words.
* By default, the separator is a regular expression for non-whitespace characters.

Additionally, notice the simpler filter 'removeDefaultStopWords', which uses a default stop word list.
