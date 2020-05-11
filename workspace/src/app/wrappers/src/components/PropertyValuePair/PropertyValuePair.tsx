import React from "react";

function PropertyValuePair({ className = "", children, hasSpacing = false, hasDivider = false, ...otherProps }: any) {
    return (
        <div
            className={
                "ecc-propertyvalue__pair" +
                (className ? " " + className : "") +
                (hasSpacing ? " ecc-propertyvalue__pair--hasspacing" : "") +
                (hasDivider ? " ecc-propertyvalue__pair--hasdivider" : "")
            }
            {...otherProps}
        >
            {children}
        </div>
    );
}

export default PropertyValuePair;
