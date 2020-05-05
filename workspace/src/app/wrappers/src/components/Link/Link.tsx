import React from "react";
import { Link as CarbonLink } from "carbon-components-react";

function Link({ className = "", children, ...otherProps }: any) {
    return (
        <CarbonLink className={"ecc-link " + className} {...otherProps}>
            {children}
        </CarbonLink>
    );
}

export default Link;
