import React from "react";

/*
    TODO:

    * allow grow factors for children
*/

function FieldItemRow({ children, className, ...otherProps }: any) {
    return <div className={"ecc-fielditem__row" + (className ? " " + className : "")}>{children}</div>;
}

export default FieldItemRow;
