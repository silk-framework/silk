import React from "react";
// import PropTypes from 'prop-types';
import WorkspaceGrid from "./WorkspaceGrid";
import WorkspaceRow from "./WorkspaceRow";

const WorkspaceContent = ({ children, className = '', ...restProps }: any) => {
    return (
        <WorkspaceGrid
            {...restProps}
            as={'article'}
            className={'ecc-workspace__content '+className}
            fullWidth={true}
        >
            <WorkspaceRow>
                { children }
            </WorkspaceRow>
        </WorkspaceGrid>
    )
}

export default WorkspaceContent;
