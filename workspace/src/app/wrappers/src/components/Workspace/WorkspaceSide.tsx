import React from "react";
// import PropTypes from 'prop-types';
import WorkspaceColumn from "./WorkspaceColumn";

const WorkspaceSide = ({ children, className = '', ...restProps }: any) => {
    return (
        <WorkspaceColumn
            {...restProps}
            className={'ecc-workspace__side '+className}
            sm={4} md={8} lg={5} xlg={5}
        >
            { children }
        </WorkspaceColumn>
    )
}

export default WorkspaceSide;
