import React from "react";
// import PropTypes from 'prop-types';
import { Column as CarbonColumn } from "carbon-components-react/lib/components/Grid";

function GridColumn({
    children,
    className = '',
    small = false,
    medium = false,
    full = true,
    verticalAlign = '',
    ...otherProps
}: any) {
    let sizeConfig = {};
    if (small) sizeConfig = { md:2, lg:3 };
    if (medium) sizeConfig = { md:3, lg:5 };
    return (
        <CarbonColumn
            {...otherProps}
            {...sizeConfig}
            className={
                'ecc-grid__column' +
                (verticalAlign ? ' ecc-grid__column--vertical-'+verticalAlign : '') +
                (className ? ' '+className : '')
            }
        >
            { children }
        </CarbonColumn>
    )
}

export default GridColumn;
