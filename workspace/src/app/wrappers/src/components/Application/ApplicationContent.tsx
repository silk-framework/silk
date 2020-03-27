import React from "react";
// import PropTypes from 'prop-types';
import { Content as CarbonContent } from "carbon-components-react/lib/components/UIShell";

function ApplicationContent({ children }: any) {
    return (
        <CarbonContent className="ecc-application__content">
            { children }
        </CarbonContent>
    )
}

export default ApplicationContent;
