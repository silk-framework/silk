## Description

The Remove remote stop words plugin removes stop words from text based on a stop word list that is fetched from a remote URL. Concretely, the plugin first splits the input into tokens using the configured separator regular expression, then removes exactly those tokens whose lowercase form appears in the stop word list, and finally returns the remaining tokens as a single space-separated string.

The stop word list is retrieved from a remote URL such as [this German stop word list](https://raw.githubusercontent.com/stopwords-iso/stopwords-de/refs/heads/master/stopwords-de.txt). The point of the remote URL is not “being remote” for its own sake, but being replaceable: it lets the same operator be used with different languages or project-specific vocabularies, for example German stop words instead of the default stop word list for English.

## Stop word list format and matching behavior

The stop word list is expected to be a plain text file where each line contains exactly one stop word. Matching is case-insensitive, so `The` and `the` are treated as the same stop word. In the case of German words, that also means case mapping matters in the literal way: the uppercase letter of the lowercase `ß` is `ẞ`, not `SS`.

## Splitting the input into words

The separator parameter defines the regular expression used for detecting words, meaning it decides how the input is split into tokens before any lookup against the stop word list happens. By default, the separator is a regular expression for non-whitespace characters.

This separator is therefore part of the semantic contract: it determines what even counts as a candidate token that could be removed by the stop word list.
