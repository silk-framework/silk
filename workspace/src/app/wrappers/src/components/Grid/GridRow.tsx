import React from "react";
// import PropTypes from 'prop-types';
import { Row as CarbonRow } from "carbon-components-react/lib/components/Grid";

function GridRow({
    children,
    className = '',
    wrapColumns = false,
    ...otherProps
}: any) {
    return (
        <CarbonRow
            {...otherProps}
            className={
                'ecc-grid__row' +
                (wrapColumns ? ' ecc-grid__row--wrapcolumns' : '') +
                (className ? ' ' + className : '')
            }
        >
            { children }
        </CarbonRow>
    )
}

export default GridRow;
