import React from "react";
// import PropTypes from 'prop-types';
import canonicalIconNames from './canonicalIconNames.json';

/*
    Properties from us:

    * name: string, our defined canonical icon name
*/

/*
    Properties from parent (Carbon Icon)

// The CSS class name.
className: string,
// The icon title.
iconTitle: string,
// The icon description.
description: string.isRequired,
// The `role` attribute. (default: img)
role: string,
// The CSS styles.
style: object,

for more see https://www.npmjs.com/package/@carbon/icons-react

*/

// TODO: add list of canonical icon names and their identifier in carbon icons
// TODO: add properties for intention/state (e.g. success, info, earning, error)

const Icon = ({
    className = '',
    name = 'undefined',
    large = false,
    ...restProps
}: any) => {
    let sizeConfig = { height: 20, width: 20 };
    if (large) sizeConfig = { height: 32, width: 32 };
    let iconImportName = 'Undefined'+sizeConfig.width;
    if (typeof canonicalIconNames[name] !== 'undefined') {
        iconImportName = canonicalIconNames[name]+sizeConfig.width;
    }
    const CarbonIcon = require('@carbon/icons-react')[iconImportName];
    return (
        <CarbonIcon {...restProps} {...sizeConfig} className={'ecc-icon '+className} />
    )
}

export default Icon;
