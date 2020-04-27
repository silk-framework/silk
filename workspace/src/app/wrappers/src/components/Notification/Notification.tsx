import React from "react";
import { Toast as BlueprintToast, Classes as BlueprintClassNames } from "@blueprintjs/core";
import * as IntentClassNames from "./../Intent/classnames";
import Icon from "./../Icon/Icon";

function Notification({
    children,
    className,
    message,
    success = false,
    warning = false,
    danger = false,
    timeout,
    ...otherProps
}: any) {
    let intentLevel = IntentClassNames.INFO;
    let iconSymbol = "state-info";
    switch (true) {
        case success:
            intentLevel = IntentClassNames.SUCCESS;
            iconSymbol = "state-success";
            break;
        case warning:
            intentLevel = IntentClassNames.WARNING;
            iconSymbol = "state-warning";
            break;
        case danger:
            intentLevel = IntentClassNames.DANGER;
            iconSymbol = "state-danger";
            break;
    }

    return (
        <BlueprintToast
            className={
                "ecc-notification " +
                intentLevel +
                (className ? " " + className : "") +
                (otherProps.onDismiss ? "" : " ecc-notification--static")
            }
            message={message ? message : children}
            timeout={timeout ? timeout : 0}
            icon={<Icon name={iconSymbol} className={BlueprintClassNames.ICON} />}
            {...otherProps}
        />
    );
}

export default Notification;
