import React from "react";

const WorkspaceHeader = ({ children, className = '' }: any) => {
    return (
        <div className={'ecc-workspace__header '+className}>
            { children }
        </div>
    )
}

export default WorkspaceHeader;
