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

/** Creates possible icon names ordered by priority. This can be used directly with the Icon component. */
export const createIconNameStack = (itemType: string, pluginId: string): string[] => {
    return [].concat([(itemType ? itemType + "-" : "") + pluginId]).concat(itemType ? [itemType] : []);
};

/** Item icon derived from the item type and optionally the plugin ID. */
export const ItemDepiction = ({ itemType, pluginId, size = { large: true } }: IProps) => {
    const iconNameStack = createIconNameStack(itemType, pluginId);

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
