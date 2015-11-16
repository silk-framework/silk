# Data Sources

- Sparql Endpoints
- RDF files
- JSON files
- XML files
- CSV files
- MySQL database

# Similarity Measures

The following similarity measures are included:
## Characterbased

Character-based distance measures compare strings on the character level. They are well suited for
handling typographical errors.

| Function and parameters | Name | Description |
| --- | --- | --- |
| jaro() | Jaro distance | String similarity based on the Jaro distance metric. |
| jaroWinkler() | Jaro-Winkler distance | String similarity based on the Jaro-Winkler distance measure. |
| levenshtein([minChar: Char = '0'], [maxChar: Char = 'z']) | Normalized Levenshtein distance | Normalized Levenshtein distance. |
| levenshteinDistance([minChar: Char = '0'], [maxChar: Char = 'z']) | Levenshtein distance | Levenshtein distance. |
| qGrams([q: Int = '2'], [minChar: Char = '0'], [maxChar: Char = 'z']) | qGrams | String similarity based on q-grams (by default q=2). |
| substring([granularity: String = '3']) | SubString | Return 0 to 1 for strong similarity to weak similarity |

## Tokenbased

While character-based distance measures work well for typographical
errors, there are a number of tasks where token-base distance measures are better suited:
- Strings where parts are reordered e.g. &ldquo;John Doe&rdquo; and &ldquo;Doe, John&rdquo;
- Texts consisting of multiple words

| Function and parameters | Name | Description |
| --- | --- | --- |
| cosine([k: Int = '3']) | Cosine | Cosine Distance Measure. |
| dice() | Dice coefficient | Dice similarity coefficient. |
| jaccard() | Jaccard | Jaccard similarity coefficient. |
| softjaccard([maxDistance: Int = '1']) | Soft Jaccard | Soft Jaccard similarity coefficient. Same as Jaccard distance but values within an levenhstein distance of 'maxDistance' are considered equivalent. |
| tokenwiseDistance([ignoreCase: Boolean = 'true'], [metricName: String = 'levenshtein'], [splitRegex: String = '[\s\d\p{Punct}]+'], [stopwords: String = ''], [stopwordWeight: Double = '0.01'], [nonStopwordWeight: Double = '0.1'], [useIncrementalIdfWeights: Boolean = 'false'], [matchThreshold: Double = '0.0'], [orderingImpact: Double = '0.0'], [adjustByTokenLength: Boolean = 'false']) | Token-wise Distance | Token-wise string distance using the specified metric |
## Numeric
| Function and parameters | Name | Description |
| --- | --- | --- |
| date() | Date | The distance in days between two dates ('YYYY-MM-DD' format). |
| dateTime() | DateTime | Distance between two date time values (xsd:dateTime format) in seconds. |
| insideNumericInterval([separator: String]) | Inside numeric interval | Checks if a number is contained inside a numeric interval, such as '1900 - 2000' |
| num([minValue: Double = '-Infinity'], [maxValue: Double = 'Infinity']) | Numeric similarity | Computes the numeric distance between two numbers. |
| wgs84([unit: String = 'km']) | Geographical distance | Computes the geographical distance between two points. Author: Konrad Höffner (MOLE subgroup of Research Group AKSW, University of Leipzig) |
## Equality
| Function and parameters | Name | Description |
| --- | --- | --- |
| constant([value: Double = '1.0']) | Constant | Always returns a constant similarity value. |
| equality() | Equality | Return 0 if strings are equal, 1 otherwise. |
| inequality() | Inequality | Return 1 if strings are equal, 0 otherwise. |
| lowerThan([orEqual: Boolean = 'false']) | LowerThan | Return 1 if the source value is lower than the target value, 0 otherwise. If both strings are numbers, numerical order is used for comparison. Otherwise, alphanumerical order is used |
| relaxedEquality() | RelaxedEquality | Return 1 if strings are equal, 0 otherwise. Lower/upper case and differences like ö/o, n/ñ, c/ç etc. are treated as equal. |
## Asian
| Function and parameters | Name | Description |
| --- | --- | --- |
| cjkReadingDistance([minChar: Char = '0'], [maxChar: Char = 'z']) | CJK Reading Distance | CJK Reading Distance. |
| korean TranslitDistance([minChar: Char = '0'], [maxChar: Char = 'z']) | Korean translit distance | Transliterated Korean distance. |
| koreanPhonemeDistance([minChar: Char = '0'], [maxChar: Char = 'z']) | Korean phoneme distance | Korean phoneme distance. |

## Spatial Distances
| Function and parameters | Name | Description |
| --- | --- | --- |
| CentroidDistanceMetric([blockingParameter: Double = 1.0]) | Centroid distance | Computes the distance between the centroids of two geometries in meters. |
| MinDistanceMetric([blockingParameter: Double = 1.0]) | Min distance | Computes the minimum distance between two geometries in meters. |

## Spatial Relations
| Function and parameters | Name | Description |
| --- | --- | --- |
| SContainsMetric([blockingParameter: Double = 1.0]) | Spatial Contains | Computes the relation "contains" between two geometries. |
| CrossesMetric([blockingParameter: Double = 1.0]) | Crosses | Computes the relation "crosses" between two geometries. |
| DisjointMetric([blockingParameter: Double = 1.0]) | Disjoint | Computes the relation "disjoint" between two geometries. |
| SEqualsMetric([blockingParameter: Double = 1.0]) | Spatial Equals  | Computes the relation "equals" between two geometries. |
| IntersectsMetric([blockingParameter: Double = 1.0]) | Intersects | Computes the relation "intersects" between two geometries. |
| SOverlapsMetric([blockingParameter: Double = 1.0]) | Spatial Overlaps | Computes the relation "overlaps" between two geometries. |
| RelateMetric([blockingParameter: Double = 1.0], relation: String) | Relate | Computes every relation from DE-9IM between two geometries. |
| TouchesMetric([blockingParameter: Double = 1.0]) | Touches | Computes the relation "touches" between two geometries. |
| WithinMetric([blockingParameter: Double = 1.0]) | Within | Computes the relation "within" between two geometries. |

## Temporal Distances
| Function and parameters | Name | Description |
| --- | --- | --- |
| DaysDistanceMetric([blockingParameter: Double = 1.0]) | Days distance | Computes the distance in days between two time periods or instants. |
| HoursDistanceMetric([blockingParameter: Double = 1.0]) | Hours distance | Computes the distance in hours between two time periods or instants. |
| MillisecsDistanceMetric([blockingParameter: Double = 1.0]) | Millisecs distance | Computes the distance in millisecs between two time periods or instants. |
| MinsDistanceMetric([blockingParameter: Double = 1.0]) | Mins distance | Computes the distance in mins between two time periods or instants. |
| MonthsDistanceMetric([blockingParameter: Double = 1.0]) | Months distance | Computes the distance in months between two time periods or instants. |
| SecsDistanceMetric([blockingParameter: Double = 1.0]) | Secs distance | Computes the distance in secs between two time periods or instants. |
| YearsDistanceMetric([blockingParameter: Double = 1.0]) | Years distance | Computes the distance in years between two time periods or instants. |

## Temporal Relations
| Function and parameters | Name | Description |
| --- | --- | --- |
| AfterMetric([blockingParameter: Double = 1.0]) | After | Computes the relation "after" between two time periods or instants. |
| BeforeMetric([blockingParameter: Double = 1.0]) | Before | Computes the relation "before" between two time periods or instants. |
| TContainsMetric([blockingParameter: Double = 1.0]) | Temporal Contains | Computes the relation "contains" between two time periods or instants. |
| DuringMetric([blockingParameter: Double = 1.0]) | During | Computes the relation "during" between two time periods or instants. |
| TEqualsMetric([blockingParameter: Double = 1.0]) | Temporal Equals | Computes the relation "equals" between two time periods or instants. |
| FinishesMetric([blockingParameter: Double = 1.0]) | Finishes | Computes the relation "finishes" between two time periods or instants. |
| IsFinishedByMetric([blockingParameter: Double = 1.0]) | IsFinishedBy | Computes the relation "isFinishedBy" between two time periods or instants. |
| IsMetByMetric([blockingParameter: Double = 1.0]) | IsMetBy | Computes the relation "isMetBy" between two time periods or instants. |
| IsOverlappedByMetric([blockingParameter: Double = 1.0]) | IsOverlappedBy | Computes the relation "isOverlappedBy" between two time periods or instants. |
| IsStartedByMetric([blockingParameter: Double = 1.0]) | IsStartedBy | Computes the relation "isStartedBy" between two time periods or instants. |
| MeetsMetric([blockingParameter: Double = 1.0]) | Meets | Computes the relation "meets" between two time periods or instants. |
| TOverlapsMetric([blockingParameter: Double = 1.0]) | Temporal Overlaps | Computes the relation "overlaps" between two time periods or instants. |
| StartsMetric([blockingParameter: Double = 1.0]) | Starts | Computes the relation "starts" between two time periods or instants. |

# Transformations

The following transform and normalization functions are included:
## Replace
| Function and parameters | Name | Description |
| --- | --- | --- |
| regexReplace(regex: String, replace: String) | Regex replace | Replace all occurrences of a regex "regex" with "replace" in a string. |
| replace(search: String, replace: String) | Replace | Replace all occurrences of a string "search" with "replace" in a string. |
## Combine
| Function and parameters | Name | Description |
| --- | --- | --- |
| concat([glue: String = '']) | Concatenate | Concatenates strings from two inputs. |
| concatMultiValues([glue: String = ''], [removeDuplicates: Boolean = 'false']) | ConcatenateMultipleValues | Concatenates multiple values received for an input. If applied to multiple inputs, yields at most one value per input. Optionally removes duplicate values. |
| merge() | Merge | Merges the values of all inputs. |
## Normalize
| Function and parameters | Name | Description |
| --- | --- | --- |
| alphaReduce() | Alpha reduce | Strips all non-alphabetic characters from a string. |
| capitalize([allWords: Boolean = 'false']) | Capitalize | Capitalizes the string i.e. converts the first character to upper case. If 'allWords' is set to true, all words are capitalized and not only the first character. |
| lowerCase() | Lower case | Converts a string to lower case. |
| removeBlanks() | Remove blanks | Remove whitespace from a string. |
| removeParentheses() | Remove Parentheses | Remove all parentheses including their content, e.g., transforms 'Berlin (City)' -> 'Berlin'. |
| removeSpecialChars() | Remove special chars | Remove special characters (including punctuation) from a string. |
| stripUriPrefix() | Strip URI prefix | Strips the URI prefix and decodes the remainder. Leaves values unchanged which don't start with 'http:' |
| trim() | Trim | Remove leading and trailing whitespaces. |
| upperCase() | Upper case | Converts a string to upper case. |
## Linguistic
| Function and parameters | Name | Description |
| --- | --- | --- |
| NYSIIS([refined: Boolean = 'true']) | NYSIIS | NYSIIS phonetic encoding. Provided by the StringMetric library: http://rockymadden.com/stringmetric/ |
| metaphone() | Metaphone | Metaphone phonetic encoding. Provided by the StringMetric library: http://rockymadden.com/stringmetric/ |
| normalizeChars() | normalizeChars | Replaces diacritical characters with non-diacritical ones (eg, ö -> o), plus some specialities like transforming æ -> ae, ß -> ss. |
| soundex([refined: Boolean = 'true']) | Soundex | Soundex algorithm. Provided by the StringMetric library: http://rockymadden.com/stringmetric/ |
| spotlight() | Spotlight | Concatenates all values to a string and gets a weighted entity vector from the Spotlight service. |
| stem() | Stem | Stems a string using the Porter Stemmer. |
## Substring
| Function and parameters | Name | Description |
| --- | --- | --- |
| stripPostfix(postfix: String) | Strip postfix | Strips a postfix of a string. |
| stripPrefix(prefix: String) | Strip prefix | Strips a prefix of a string. |
| stripUriPrefix() | Strip URI prefix | Strips the URI prefix and decodes the remainder. Leaves values unchanged which don't start with 'http:' |
| substring([beginIndex: Int = '0'], [endIndex: Int = '0']) | Substring | Returns a substring between 'beginIndex' (inclusive) and 'endIndex' (exclusive). If 'endIndex' is 0 (default), it is ignored and the entire remaining string starting with 'beginIndex' is returned. If 'endIndex' is negative, -endIndex characters are removed from the end.' |
| trim() | Trim | Remove leading and trailing whitespaces. |
| untilCharacter(untilCharacter: Char) | Until Character | Give a substring until the character given |
## Conversion
| Function and parameters | Name | Description |
| --- | --- | --- |
| convertCharset([sourceCharset: String = 'ISO-8859-1'], [targetCharset: String = 'UTF-8']) | Convert Charset | Convert the string from "sourceCharset" to "targetCharset". |
## Filter
| Function and parameters | Name | Description |
| --- | --- | --- |
| filterByLength([min: Int = '0'], [max: Int = '2147483647']) | filter by length | Removes all strings that are shorter than 'min' characters and longer than 'max' characters. |
| filterByRegex(regex: String, [negate: Boolean = 'false']) | filter by regex | Removes all strings that do NOT match a regex. If 'negate' is true, only strings will be removed that match the regex. |
| removeEmptyValues() | Remove empty values | Removes empty values. |
| removeValues(blacklist: String) | Remove values | Removes values. |
## Tokenization
| Function and parameters | Name | Description |
| --- | --- | --- |
| camelcasetokenizer() | Camel Case Tokenizer | Tokenizes a camel case string. That is it splits strings between a lower case characted and an upper case character. |
| tokenize([regex: String = '\s']) | Tokenize | Tokenizes all input values. |
## Numeric
| Function and parameters | Name | Description |
| --- | --- | --- |
| aggregateNumbers(operator: String) | Aggregate Numbers |  Aggregates all numbers in this set using a mathematical operation. |
| compareNumbers([comparator: String = '<']) | Compare Numbers |  Compares the numbers of two sets. |
| numReduce() | Numeric reduce | Strip all non-numeric characters from a string. |
## Date
| Function and parameters | Name | Description |
| --- | --- | --- |
| compareDates([comparator: String = '<']) | Compare Dates |  Compares two dates. Returns 1 if the comparison yields true and 0 otherwise. If there are multiple dates in both sets, the comparator must be true for all dates. e.g. {2014-08-02,2014-08-03} < {2014-08-03} yields 0 as not all dates in the first set are smaller than in the second. Accepts one parameter: comparator: One of '<', '<=', '=', '>=', '>'  |
| datetoTimestamp() | Date to timestamp | Convert an xsd:date to a Unix timestamp |
| duration() | Duration | Computes the time difference between two data times. |
| durationInDays() | Duration in Days | Converts an xsd:duration to days. |
| durationInSeconds() | Duration in Seconds | Converts an xsd:duration to seconds. |
| timeToDate() | Timestamp to date | convert Unix timestamp to xsd:date |


## Spatial
| Function and parameters | Name | Description |
| --- | --- | --- |
| AreaTransformer | Area Transformer | Returns the area of the input geometry. |
| BufferTransformer(distance: Double) | Buffer Transformer | Returns the buffered geometry of the input geometry. |
| EnvelopeTransformer | Envelope Transformer | Returns the envelope (minimum bounding rectangle) of the input geometry. |
| GeometryTransformer | Geometry Transformer | Trasforms a geometry expressed in GeoSPARQL, stSPARQL or W3C Geo vocabulary from any serialization (WKT or GML) and any Coordinate Reference System (CRS) to WKT and WGS 84 (latitude-longitude). |
| PointsToCentroidCTransformer | Points-To-Centroid Transformer | Transforms a cluster of points expressed in W3C Geo vocabulary to their centroid expressed in WKT and WGS 84 (latitude-longitude). |
| SimplifyTransformer(distanceTolerance: Double, [preserveTopology: Boolean = false]) | Simplify Transformer | Simplifies a geometry according to a given distance tolerance. |
