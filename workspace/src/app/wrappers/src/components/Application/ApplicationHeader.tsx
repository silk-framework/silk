import React from "react";
// import PropTypes from 'prop-types';
import { Header as CarbonHeader } from "carbon-components-react/lib/components/UIShell";

const ApplicationHeader = ({ children, ...restProps }: any) => {
    return (
        <CarbonHeader {...restProps} className="ecc-application__header">
            { children }
        </CarbonHeader>
    )
}

export default ApplicationHeader;
