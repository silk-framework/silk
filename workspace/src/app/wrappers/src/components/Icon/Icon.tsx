import React from "react";
import Tooltip from "./../Tooltip/Tooltip";
import canonicalIconNames from "./canonicalIconNames.json";

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

function Icon({
    className = "",
    name = "undefined",
    large = false,
    small = false,
    tooltipText,
    tooltipOpenDelay,
    ...restProps
}: any) {
    let sizeConfig = { height: 20, width: 20 };
    if (small) sizeConfig = { height: 16, width: 16 };
    if (large) sizeConfig = { height: 32, width: 32 };
    let iconImportName = "Undefined" + sizeConfig.width;
    if (typeof canonicalIconNames[name] !== "undefined") {
        iconImportName = canonicalIconNames[name] + sizeConfig.width;
    }
    const CarbonIcon = require("@carbon/icons-react")[iconImportName];
    const icon = <CarbonIcon {...restProps} {...sizeConfig} className={"ecc-icon " + className} />;
    return tooltipText ? (
        <Tooltip content={tooltipText} hoverOpenDelay={tooltipOpenDelay}>
            {icon}
        </Tooltip>
    ) : (
        icon
    );
}

export default Icon;
