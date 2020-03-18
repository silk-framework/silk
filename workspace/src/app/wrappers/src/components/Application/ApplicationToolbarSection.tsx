import React from "react";

const ApplicationToolbarSection = ({ children, className = '', ...restProps }: any) => {
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
