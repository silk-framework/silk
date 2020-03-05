import React from "react";
// import PropTypes from 'prop-types';
import WorkspaceColumn from "./WorkspaceColumn";

const WorkspaceMain = ({ children, className = '', ...restProps }: any) => {
    return (
        <WorkspaceColumn
            {...restProps}
            className={'ecc-workspace__main '+className}
            
        >
            { children }
        </WorkspaceColumn>
    )
}

export default WorkspaceMain;
