import React from "react";

function WorkspaceSection({ children, className = '', ...restProps }: any) {
    return (
        <section
            {...restProps}
            className={'ecc-workspace__section '+className}
        >
            { children }
        </section>
    )
}

export default WorkspaceSection;
