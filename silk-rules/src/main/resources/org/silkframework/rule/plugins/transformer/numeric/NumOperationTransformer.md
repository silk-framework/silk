The `numOperation` plugin applies one of the four basic arithmetic operators to the sequence of input values.
These are the fundamental operations of **addition** (`+`), **subtraction** (`-`), **multiplication** (`*`)
and **division** (`/`).

Notice that the symbol `÷` can't be used for the 'division' operator, and remember that one
should never divide by null. Doing so will result in `Infinity`.

The computations are done with
[double-precision floating-point numbers](https://en.wikipedia.org/wiki/Double-precision_floating-point_format).
This means that e.g. integers such as `1` or `2` will be converted to `1.0` and `2.0`.
This also regards the _output_ of the operation, as in `1 + 1` leading to `2.0` rather than the integer `2`.

_**Only** the four basic arithmetic operations are allowed_ (and understood) by this numeric transformer plugin. If an
_invalid operation_ is given, an error or exception will occur. In the same manner, if the _values_ aren't (valid)
numbers, a validation exception will be raised.
