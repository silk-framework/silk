import React, { memo } from "react";
import { Spinner, SpinnerProps } from "@eccenca/gui-elements";

interface IProps {
    className?: string;
    color?: string;
    description?: string; // currently unsupported
    position?: SpinnerProps["position"];
    size?: SpinnerProps["size"];
    stroke?: SpinnerProps["stroke"];
    posGlobal?: boolean;
    posLocal?: boolean;
    posInline?: boolean;
    delay?: number;
}

export const Loading = memo<IProps>(function LoadingIndicator({
    posGlobal = false,
    posLocal = true,
    posInline = false,
    delay,
    ...otherProps
}) {
    let forwardedProps = {};
    switch (true) {
        case posGlobal:
            forwardedProps = { position: "global", color: "primary", className: "spinner-global" };
            break;
        case posInline:
            forwardedProps = { position: "inline" };
            break;
        default:
            forwardedProps = { position: "local" };
    }
    const showDelay = delay ?? (posGlobal ? 0 : 1000);
    return <Spinner {...forwardedProps} {...otherProps} delay={showDelay} />;
});
