# @eccenca/material-design-lite

> This fork of [material-design-lite] modifies it to be used with react-mdl and contains several other fixes.

## Table of Contents

- [Background](#background)
- [Install](#install)
- [Usage](#usage)
- [License](#license)

## Background

[material-design-lite] and [react-mdl] are both deprecated and unlikely to be updated in the future. Meanwhile there are some pain points which we fix with this package.

1. react-mdl requires a `1.2.1` build of material-design-lite with additional patches applied [\[1\]][1]. We however want to use the newer `1.3.0` version. This package applies the patches from react-mdl on the `1.3.0` version.
2. material-design-lite does not use SASS-colors (See https://github.com/google/material-design-lite/issues/146). In this package we fix that
3. material-design-lite comes with over 60MB of pre-generated CSS which is not needed if only SASS is used.
4. material-design-lite init is really slow if iterating over large arrays of items. This package fixes this by wrapping upgradeElementInternal with `setTimout(..., 0)`

## Install

```
yarn add @eccenca/material-design-lite
```

## Usage

To import the javascript, simply require it:

```js
import '@eccenca/material-design-lite';
```

To use the sass, simply use:

```js
import '@eccenca/material-design-lite/src/material-design-lite';
```

## License

This projects is licensed under `(Apache-2.0 AND MIT)`, as it contains source code from [material-design-lite] which is `Apache-2.0` licensed and [react-mdl] which is `MIT` licensed.

[material-design-lite]: https://github.com/google/material-design-lite
[react-mdl]: https://github.com/react-mdl/react-mdl
[1]: https://github.com/react-mdl/react-mdl/tree/v1.10.3#requirements
