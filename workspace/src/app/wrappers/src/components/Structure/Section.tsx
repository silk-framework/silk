import React from "react";

function Section({ children, className = '', ...restProps }: any) {
    return (
        <section
            {...restProps}
            className={'ecc-structure__section '+className}
        >
            { children }
        </section>
    )
}

export default Section;
