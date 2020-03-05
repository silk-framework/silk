import React from "react";

const WorkspaceSection = ({ children, className = '', ...restProps }: any) => {
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
