import React from "react";
import { Icon } from "@gui-elements/index";

const sizes = ["large", "small"] as const;
type Sizes = typeof sizes[number];

interface IProps {
    itemType?: string;
    pluginId?: string;
    size?: {
        [K in Sizes]?: boolean;
    };
}

/** Item icon derived from the item type and optionally the plugin ID. */
export const ItemDepiction = ({ itemType, pluginId, size = { large: true } }: IProps) => {
    const iconNameStack = [].concat([(itemType ? itemType + "-" : "") + pluginId]).concat(itemType ? [itemType] : []);

    return (
        <Icon
            name={iconNameStack
                .map((type) => {
                    return "artefact-" + type.toLowerCase();
                })
                .filter((x, i, a) => a.indexOf(x) === i)}
            {...size}
        />
    );
};
