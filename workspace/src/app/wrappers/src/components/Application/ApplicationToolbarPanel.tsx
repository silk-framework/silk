import React from "react";
// import PropTypes from 'prop-types';
import { HeaderPanel as CarbonHeaderPanel } from "carbon-components-react/lib/components/UIShell";

function ApplicationToolbarPanel({ children, className = '', ...restProps }: any) {
    return (
        <CarbonHeaderPanel
            {...restProps}
            className={'ecc-application__toolbar__panel ' + className}
        >
            { children }
        </CarbonHeaderPanel>
    )
}

export default ApplicationToolbarPanel;
