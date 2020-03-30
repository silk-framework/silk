import React from "react";

function ApplicationToolbarSection({ children, className = '', ...restProps }: any) {
    return (
        <div
            {...restProps}
            className={'ecc-application__toolbar__section ' + className}
        >
            { children }
        </div>
    )
}

export default ApplicationToolbarSection;
