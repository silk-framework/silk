import { MAPPING_RULE_TYPE_COMPLEX, MAPPING_RULE_TYPE_DIRECT, MAPPING_RULE_TYPE_OBJECT } from "../utils/constants";
import { Icon, Spacing } from "@eccenca/gui-elements";
import React from "react";

export const ThingIcon = ({ type, status, message }) => {
    let iconName = "item-question";
    let tooltip = "";
    switch (type) {
        case MAPPING_RULE_TYPE_DIRECT:
        case MAPPING_RULE_TYPE_COMPLEX:
            tooltip = "Value mapping";
            iconName = "artefact-file";
            break;
        case MAPPING_RULE_TYPE_OBJECT:
            tooltip = "Object mapping";
            iconName = "artefact-project";
            break;
        default:
            iconName = "item-question";
    }

    return (
        <>
            <Icon
                className="ecc-silk-mapping__ruleitem-icon"
                name={status === "error" ? "warning" : iconName}
                tooltip={status === "error" ? `${tooltip} (${message})` : tooltip}
                small
            />
            <Spacing vertical size={"tiny"} />
        </>
    );
};
