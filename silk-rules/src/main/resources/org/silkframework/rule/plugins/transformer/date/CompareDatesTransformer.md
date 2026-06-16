## Description

The Compare dates plugin compares two input date values and returns `1` if the configured comparison holds, otherwise `0`.

This plugin is a date transformer plugin. For configuring it, we provide a comparator parameter that determines whether the two inputs should be checked for being smaller, greater, smaller than or equal, greater than or equal, or equal. For calling it, we provide two inputs containing the date values that should be compared.

Only valid XSD date literals are considered in the comparison. If one side contains no valid date at all, the result is `0`.

The Compare dates plugin is therefore not a simple left-against-right comparison of one value with one other value. As soon as one side contains several valid dates, the plugin compares the date information contained in the two inputs as a whole and returns one boolean-style result for that overall comparison.

## Comparing multiple date values

If several valid dates are present on one or both sides, the plugin does not compare them pairwise by position. It also does not return `1` merely because some date on the left side is smaller or greater than some date on the right side.

For the ordering comparators, the relation must hold across the two inputs as a whole. This is decided through the extreme values on both sides:

* For `<`, the latest valid date on the left side must still be earlier than the earliest valid date on the right side.
* For `<=`, the latest valid date on the left side must be earlier than or equal to the earliest valid date on the right side.
* For `>`, the earliest valid date on the left side must still be later than the latest valid date on the right side.
* For `>=`, the earliest valid date on the left side must be later than or equal to the latest valid date on the right side.

In other words: for the ordering comparators, one side must lie completely before or completely after the other side, depending on the chosen comparator. That is the point of the extrema-based comparison.

## Equality

Equality is stricter than the ordering cases.

It is not enough that the two inputs happen to share one date somewhere. Instead, each side must amount to exactly one distinct valid date value, and those two date values must be identical.

So equality only succeeds if the left side collapses to one single valid date, the right side collapses to one single valid date, and both dates are the same. If one side contains several different valid dates, equality returns `0`.

## Invalid values

Only valid XSD date literals are taken into account.

Values that are not valid XSD date literals are ignored. If, after this filtering step, one side contains no valid date at all, then the result is `0`.

So invalid values do not make the plugin fail. They are simply ignored, and if no valid date remains on one side, the comparison returns `0`.
