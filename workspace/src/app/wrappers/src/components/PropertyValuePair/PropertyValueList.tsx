import React from "react";

function PropertyValueList({ className = "", children, ...otherProps }: any) {
    return (
        <dl className={"ecc-propertyvalue__list" + (className ? " " + className : "")} {...otherProps}>
            {children}
        </dl>
    );
}

export default PropertyValueList;
