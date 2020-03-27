import React from "react";
// import PropTypes from 'prop-types';
import { HeaderGlobalAction as CarbonHeaderGlobalAction } from "carbon-components-react/lib/components/UIShell";

function ApplicationToolbarAction({ children, className = '', ...restProps }: any) {
    return (
        <CarbonHeaderGlobalAction
            {...restProps}
            className={'ecc-application__toolbar__action ' + className}
        >
            { children }
        </CarbonHeaderGlobalAction>
    )
}

export default ApplicationToolbarAction;
