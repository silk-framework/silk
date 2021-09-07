import React, { memo } from "react";
import { Spinner } from "@gui-elements/index";
import { SpinnerPosition, SpinnerSize, SpinnerStroke } from "@gui-elements/src/components/Spinner/Spinner";

interface IProps {
    className?: string;
    color?: string;
    description?: string; // currently unsupported (TODO)
    position?: SpinnerPosition;
    size?: SpinnerSize;
    stroke?: SpinnerStroke;
    posGlobal?: boolean;
    posLocal?: boolean;
    posInline?: boolean;
}

export const Loading = memo<IProps>(function LoadingIndicator({
    posGlobal = false,
    posLocal = true,
    posInline = false,
    ...otherProps
}) {
    let forwardedProps = {};
    switch (true) {
        case posGlobal:
            forwardedProps = { position: "global", color: "primary" };
            break;
        case posInline:
            forwardedProps = { position: "inline" };
            break;
        default:
            forwardedProps = { position: "local" };
    }

    return <Spinner {...forwardedProps} {...otherProps} />;
});
