import React from "react";
// import PropTypes from 'prop-types';
import { Row as CarbonRow } from "carbon-components-react/lib/components/Grid";

function GridRow({ children, className = "", dontWrapColumns = true, ...otherProps }: any) {
    return (
        <CarbonRow
            {...otherProps}
            className={
                "ecc-grid__row" +
                (dontWrapColumns ? " ecc-grid__row--dontwrapcolumns" : "") +
                (className ? " " + className : "")
            }
        >
            {children}
        </CarbonRow>
    );
}

export default GridRow;
