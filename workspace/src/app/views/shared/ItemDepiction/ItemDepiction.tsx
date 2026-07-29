import React from "react";
import { Icon } from "@eccenca/gui-elements";
import { convertTaskTypeToItemType, TaskType } from "@ducks/shared/typings";

const sizes = ["large", "small"] as const;
type Sizes = (typeof sizes)[number];

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
    const realItemType = itemType ? convertTaskTypeToItemType(itemType as TaskType, true) : undefined;
    pluginId && generatedIconNames.push((realItemType ? realItemType + "-" : "") + pluginId);
    realItemType && generatedIconNames.push(realItemType);
    const prefixedGeneratedIconNames = generatedIconNames.map((type) => {
        return "artefact-" + convertTaskTypeToItemType(type as TaskType, true).toLowerCase();
    });
    return prefixedGeneratedIconNames.filter((x, i, a) => a.indexOf(x) === i);
};

/** Item icon derived from the item type and optionally the plugin ID.
 *
 * Defaults to the `small` (16px) icon size: every consumer renders inside the 36px
 * `OverviewItemDepiction` tile (or an equivalent list-row slot), and Lucide's stroke width
 * scales with the icon box — the former 32px `large` default rendered visibly fatter and
 * bigger than every other icon in the app. */
export const ItemDepiction = ({ itemType, pluginId, size = { small: true } }: IProps) => (
    <Icon name={createIconNameStack(itemType, pluginId)} {...size} />
);
