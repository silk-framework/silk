import React from "react";
import Label from "../Label/Label";
import * as IntentClassNames from "../Intent/classnames";

function FieldItem({
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
            <p className={"ecc-fielditem__helpertext"}>{helperText}</p>
        ) : (
            <div className={"ecc-fielditem__helpertext"}>{helperText}</div>
        ));

    const inputfields = children && <div className={"ecc-fielditem__inputfields"}>{children}</div>;

    const notification =
        messageText &&
        (typeof messageText === "string" ? (
            <p className={"ecc-fielditem__message" + classIntent}>{messageText}</p>
        ) : (
            <div className={"ecc-fielditem__message" + classIntent}>{messageText}</div>
        ));

    return (
        <div
            className={
                "ecc-fielditem" + (className ? " " + className : "") + (disabled ? " ecc-fielditem--disabled" : "")
            }
        >
            {label}
            {userhelp}
            {inputfields}
            {notification}
        </div>
    );
}

export default FieldItem;
