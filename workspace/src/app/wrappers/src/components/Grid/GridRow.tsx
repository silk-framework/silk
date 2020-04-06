import React from "react";
// import PropTypes from 'prop-types';
import { Row as CarbonRow } from "carbon-components-react/lib/components/Grid";

function GridRow({ children, className = '', ...restProps }: any) {
    return (
        <CarbonRow {...restProps} className={'ecc-grid__row '+className}>
            { children }
        </CarbonRow>
    )
}

export default GridRow;
