import React from "react";

function SectionHeader({ children, className = '', ...restProps }: any) {
    return (
        <header
            {...restProps}
            className={'ecc-structure__section__header '+className}
        >
            { children }
        </header>
    )
}

export default SectionHeader;
