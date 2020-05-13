import React from "react";

function PropertyValue({ className = "", children, ...otherProps }: any) {
    return (
        <dd className={"ecc-propertyvalue__value" + (className ? " " + className : "")} {...otherProps}>
            <div>{children}</div>
        </dd>
    );
}

export default PropertyValue;
