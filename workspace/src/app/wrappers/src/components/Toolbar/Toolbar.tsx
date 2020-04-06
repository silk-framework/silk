import React from "react";

function Toolbar({ children, className = '', ...restProps }: any) {
    return (
        <div
            {...restProps}
            className={'ecc-toolbar '+className}
        >
            { children }
        </div>
    )
}

export default Toolbar;
