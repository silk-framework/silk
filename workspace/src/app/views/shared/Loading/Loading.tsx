import { Spinner } from "@eccenca/gui-elements";
import { SpinnerPosition, SpinnerSize, SpinnerStroke } from "@eccenca/gui-elements/src/components/Spinner/Spinner";
import React, { memo } from "react";

interface IProps {
    className?: string;
    color?: string;
    description?: string; // currently unsupported
    position?: SpinnerPosition;
    size?: SpinnerSize;
    stroke?: SpinnerStroke;
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
