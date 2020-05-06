import React from "react";
import Label from "../Label/Label";
import * as IntentClassNames from "../Intent/classnames";

function Formitem({
    hasStatePrimary = false,
    hasStateSuccess = false,
    hasStateWarning = false,
    hasStateDanger = false,
    children,
    className,
    disabled,
    labelAttributes = {},
    helperText,
    messageText,
    ...otherProps
}: any) {
    let classIntent = "";
    switch (true) {
        case hasStatePrimary:
            classIntent = " " + IntentClassNames.PRIMARY;
            break;
        case hasStateSuccess:
            classIntent = " " + IntentClassNames.SUCCESS;
            break;
        case hasStateWarning:
            classIntent = " " + IntentClassNames.WARNING;
            break;
        case hasStateDanger:
            classIntent = " " + IntentClassNames.DANGER;
            break;
        default:
            break;
    }

    const label = <Label {...labelAttributes} disabled={disabled} />;

    const userhelp =
        helperText &&
        (typeof helperText === "string" ? (
            <p className={"ecc-formitem__helpertext"}>{helperText}</p>
        ) : (
            <div className={"ecc-formitem__helpertext"}>{helperText}</div>
        ));

    const inputfields = children && <div className={"ecc-formitem__inputfields"}>{children}</div>;

    const notification =
        messageText &&
        (typeof messageText === "string" ? (
            <p className={"ecc-formitem__message" + classIntent}>{messageText}</p>
        ) : (
            <div className={"ecc-formitem__message" + classIntent}>{messageText}</div>
        ));

    return (
        <div
            className={
                "ecc-formitem" + (className ? " " + className : "") + (disabled ? " ecc-formitem--disabled" : "")
            }
        >
            {label}
            {userhelp}
            {inputfields}
            {notification}
        </div>
    );
}

export default Formitem;
