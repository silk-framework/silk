import React from "react";
// import PropTypes from 'prop-types';
import { Grid as CarbonGrid } from "carbon-components-react/lib/components/Grid";

function WorkspaceGrid({ children, className = '', ...restProps }: any) {
    return (
        <CarbonGrid {...restProps} className={'ecc-workspace__grid '+className} fullWidth={true}>
            { children }
        </CarbonGrid>
    )
}

export default WorkspaceGrid;
