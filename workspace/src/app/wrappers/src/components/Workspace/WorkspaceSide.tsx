import React from "react";
// import PropTypes from 'prop-types';
import WorkspaceColumn from "./WorkspaceColumn";

const WorkspaceMain = ({ children, className = '', ...restProps }: any) => {
    return (
        <WorkspaceColumn
            {...restProps}
            className={'ecc-workspace__side '+className}
            sm={4} md={8} lg={4} xlg={4}
        >
            { children }
        </WorkspaceColumn>
    )
}

export default WorkspaceMain;
