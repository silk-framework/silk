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
export const createIconNameStack = (itemType?: string, pluginId?: string): string[] => {
    const generatedIconNames: string[] = [];
    pluginId && generatedIconNames.push((itemType ? itemType + "-" : "") + pluginId);
    itemType && generatedIconNames.push(itemType);
    const prefixedGeneratedIconNames = generatedIconNames.map((type) => {
        return "artefact-" + type.toLowerCase();
    });
    return prefixedGeneratedIconNames.filter((x, i, a) => a.indexOf(x) === i);
};

/** Item icon derived from the item type and optionally the plugin ID. */
export const ItemDepiction = ({ itemType, pluginId, size = { large: true } }: IProps) => {
    return <Icon name={createIconNameStack(itemType, pluginId)} {...size} />;
};
