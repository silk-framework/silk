import React from "react";
import Label from "../Label/Label";

function PropertyName({ className = "", children, ...otherProps }: any) {
    return (
        <dt className={"ecc-propertyvalue__property" + (className ? " " + className : "")} {...otherProps}>
            <div>{typeof children === "string" ? <Label text={children} isLayoutForElement="span" /> : children}</div>
        </dt>
    );
}

export default PropertyName;
