import React from "react";
// import PropTypes from 'prop-types';
import { HeaderGlobalBar as CarbonHeaderGlobalBar } from "carbon-components-react/lib/components/UIShell";

const ApplicationToolbar = ({ children, className = '', ...restProps }: any) => {
    return (
        <CarbonHeaderGlobalBar
            {...restProps}
            className={'ecc-application__toolbar ' + className}
        >
            { children }
        </CarbonHeaderGlobalBar>
    )
}

export default ApplicationToolbar;
