## Description

The Numeric operation plugin applies one of the four basic arithmetic operators to the sequence of input values and returns the numeric result as a string.

For configuring it, we provide an operator parameter that determines whether the input values should be added, subtracted, multiplied, or divided. Only the four operators `+`, `-`, `*`, and `/` are accepted. If any other operator is configured, the plugin fails at configuration time.

For calling it, we provide one or more input sequences. All values from all inputs are treated as operands of one single operation: the plugin collects them, parses them as numbers, and reduces the resulting operand sequence by repeatedly applying the configured operator. The computations are done with [double-precision floating-point numbers](https://en.wikipedia.org/wiki/Double-precision_floating-point_format), which is why inputs like `1` are treated as `1.0` and why results such as `1 + 1` are rendered as `2.0` rather than as an integer.

## Applying the operator to multiple input values

The Numeric operation plugin does not apply the operator to one left value and one right value only. It applies the operator across the full sequence of parsed operands, independent of how those operands are distributed across the inputs, and returns exactly one value as the final reduction result.

## Invalid values

Every input value must be a valid number. If any value is not a number, the plugin fails instead of skipping that value or treating it as `0`. At least one numeric input value is required, because the operator is applied by reducing the operand sequence into a single result.

## Division

Division is configured with the `/` operator. The symbol `÷` is not supported. Division by `0` does not fail and yields `Infinity`.