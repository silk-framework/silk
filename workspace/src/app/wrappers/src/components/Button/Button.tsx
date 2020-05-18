import React from "react";
import {
    AnchorButton as BlueprintAnchorButton,
    Button as BlueprintButton,
    Intent as BlueprintIntent,
} from "@blueprintjs/core";
import Icon from "../Icon/Icon";

function Button({
    children,
    className = "",
    affirmative = false,
    disruptive = false,
    elevated = false,
    hasStatePrimary = false,
    hasStateSuccess = false,
    hasStateWarning = false,
    hasStateDanger = false,
    icon = false,
    rightIcon = false,
    ...restProps
}: any) {
    let intention = null;
    switch (true) {
        case affirmative || elevated || hasStatePrimary:
            intention = BlueprintIntent.PRIMARY;
            break;
        case hasStateSuccess:
            intention = BlueprintIntent.SUCCESS;
            break;
        case hasStateWarning:
            intention = BlueprintIntent.WARNING;
            break;
        case disruptive || hasStateDanger:
            intention = BlueprintIntent.DANGER;
            break;
        default:
            break;
    }

    let ButtonType = restProps.href ? BlueprintAnchorButton : BlueprintButton;

    return (
        <ButtonType
            {...restProps}
            className={"ecc-button " + className}
            intent={intention}
            icon={typeof icon === "string" ? <Icon name={icon} /> : icon}
            rightIcon={typeof rightIcon === "string" ? <Icon name={rightIcon} /> : rightIcon}
        >
            {children}
        </ButtonType>
    );
}

export default Button;
