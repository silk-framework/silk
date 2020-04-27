import React from "react";
import Notification from "./Notification";

function Toast({ children, className, timeout = 10000, ...otherProps }: any) {
    return (
        <Notification
            className={"ecc-notification--toast" + (className ? " " + className : "")}
            timeout={timeout}
            {...otherProps}
        >
            {children}
        </Notification>
    );
}

export default Toast;
