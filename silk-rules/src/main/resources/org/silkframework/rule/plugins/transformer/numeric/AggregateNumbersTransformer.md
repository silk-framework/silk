The `aggregateNumbers` plugin applies an aggregation operator to the sequence of input values.
The allowed aggregation operators are **sum** (`+`), **product** (`*`), **minimum** (`min`), **maximum** (`max`)
and **average** (`average`).

Notice that the "number" `Infinity` is allowed as input and to be expected as possible output. For example, the
_minimum_ of `1` and `Infinity` is `1`, but its _maximum_, its _sum_, its _product_ and its _average_ are `Infinity`.

The **aggregation operations** of this plugin are exclusively _numerical_. They are exactly the expected operations upon
a list or sequence of numbers, in order to 'aggregate', 'reduce', 'sum' or
'[join](https://en.wikipedia.org/wiki/Fold_(higher-order_function))' them.

The computations are done with
[double-precision floating-point numbers](https://en.wikipedia.org/wiki/Double-precision_floating-point_format).
This means that e.g. integers such as `1` or `2` will be converted to `1.0` and `2.0`.
This also regards the _output_ of the operation, as in `1 + 1` leading to `2.0` rather than the integer `2`.

_**Only** the five listed aggregation operations are allowed_ (and understood) by this numeric transformer plugin. If an
_invalid operation_ is given, an error or exception will occur. On the other hand, if any _values_ aren't (valid)
numbers, they will be ignored.
