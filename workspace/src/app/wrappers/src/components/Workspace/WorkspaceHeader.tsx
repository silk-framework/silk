import React from "react";

function WorkspaceHeader({ children, className = '' }: any) {
    return (
        <div className={'ecc-workspace__header '+className}>
            { children }
        </div>
    )
}

export default WorkspaceHeader;
