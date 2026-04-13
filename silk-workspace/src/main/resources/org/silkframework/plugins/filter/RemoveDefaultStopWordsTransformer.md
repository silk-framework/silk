## Description

The Remove default stop words plugin removes stop words from text using a built-in default stop word list.

Conceptually, it works like a standard stop word filter: the input is split into word tokens, each token is checked against the default list, and all tokens that appear in the list are removed. The remaining tokens are kept and returned as the filtered text.

Stop word removal is case-insensitive. For example, `The` and `the` are treated as the same stop word. In the case of German words, notice that the upper-case letter of the lower-case `ß` is `ẞ`, not `SS`.

If a different stop word list is needed, the Remove stop words plugin supports providing a stop word list as a resource, and the Remove remote stop words plugin supports fetching the stop word list from a remote URL.
