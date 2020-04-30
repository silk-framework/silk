import React from "react";
import * as TypographyClassNames from "./classnames";

function HtmlContentBlock({
    className = "",
    children,
    small = false, // currently unsupported
    large = false, // currently unsupported
    muted = false, // currently unsupported
    disabled = false, // currently unsupported
    ...otherProps
}: any) {
    return (
        <div
            className={
                "ecc-typography__contentblock" +
                (className ? " " + className : "") +
                (small ? " " + TypographyClassNames.SMALL : "") +
                (large ? " " + TypographyClassNames.LARGE : "") +
                (muted ? " " + TypographyClassNames.MUTED : "") +
                (disabled ? " " + TypographyClassNames.DISABLED : "")
            }
        >
            {children}
        </div>
    );
}

export default HtmlContentBlock;
