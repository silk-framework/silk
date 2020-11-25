import React from "react";
import { Icon } from "@gui-elements/index";

interface IProps {
    itemType?: string;
    pluginId?: string;
}

/** Item icon derived from the item type and optionally the plugin ID. */
export const ItemDepiction = ({ itemType, pluginId }: IProps) => {
    const iconNameStack = [].concat([(itemType ? itemType + "-" : "") + pluginId]).concat(itemType ? [itemType] : []);

    return (
        <Icon
            name={iconNameStack
                .map((type) => {
                    return "artefact-" + type.toLowerCase();
                })
                .filter((x, i, a) => a.indexOf(x) === i)}
            large
        />
    );
};
