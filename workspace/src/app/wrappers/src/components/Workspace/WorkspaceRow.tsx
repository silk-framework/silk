import React from "react";
// import PropTypes from 'prop-types';
import { Row as CarbonRow } from "carbon-components-react/lib/components/Grid";

const WorkspaceRow = ({ children, className = '', ...restProps }: any) => {
    return (
        <CarbonRow {...restProps} className={'ecc-workspace__row '+className}>
            { children }
        </CarbonRow>
    )
}

export default WorkspaceRow;
