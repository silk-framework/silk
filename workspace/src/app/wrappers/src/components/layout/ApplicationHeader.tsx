import React from "react";
// import PropTypes from 'prop-types';
import { Header as CarbonHeader } from "carbon-components-react/lib/components/UIShell";

const ApplicationHeader = ({ children }: any) => {
    return (
        <CarbonHeader className="ecc-application__header">
            { children }
        </CarbonHeader>
    )
}

export default ApplicationHeader;
