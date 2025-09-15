Extracts physical quantities, such as length or weight values.
Values are expected of the form `{Number}{UnitPrefix}{Symbol}` and are converted to the base unit.

Example: Let a value such as `"10km, 3mg"`, containing both a distance and a weight, be given. If the `symbol` parameter is set to `m`, then the extracted value will be `1000` (i.e. the distance). If, instead, the `symbol` parameter is set to `g`, then the extracted value will be `0.001` (i.e. the weight).