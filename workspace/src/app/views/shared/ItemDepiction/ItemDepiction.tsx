import React from "react";
import { Icon } from "@eccenca/gui-elements";
import {useSelector} from "react-redux";
import {commonSel} from "@ducks/common";
import {IPluginOverview} from "@ducks/common/typings";
import {convertTaskTypeToItemType, TaskType} from "@ducks/shared/typings";

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

const customPluginIcon: {artefactList?: IPluginOverview[], iconMap: Map<string, string>} = {
    iconMap: new Map()
}

const taskTypeSet: Set<TaskType> = new Set(["Dataset", "Linking", "Transform", "Workflow", "CustomTask"])
const pluginKey = (itemType: string, pluginId: string): string => `${itemType} ${pluginId}`
const getCustomPluginIcon = (itemType: string, pluginId: string, artefactList: IPluginOverview[] | undefined): string | undefined => {
    const correctItemType = taskTypeSet.has(itemType as TaskType) ?
        // Item type is a task type and needs to be converted
        convertTaskTypeToItemType(itemType as TaskType) :
        itemType
    if(artefactList && artefactList !== customPluginIcon.artefactList) {
        // Add icons to map
        customPluginIcon.iconMap = new Map()
        artefactList.forEach(plugin => {
            if(plugin.pluginIcon) {
                customPluginIcon.iconMap.set(pluginKey(convertTaskTypeToItemType(plugin.taskType), plugin.key), plugin.pluginIcon)
            }
        })
        customPluginIcon.artefactList = artefactList
    }
    return customPluginIcon.iconMap.get(pluginKey(correctItemType, pluginId))
}

/** Item icon derived from the item type and optionally the plugin ID. */
export const ItemDepiction = ({ itemType, pluginId, size = { large: true } }: IProps) => {
    const taskPluginOverviews = useSelector(commonSel.taskPluginOverviewsSelector);
    const customPluginIcon = itemType && pluginId ?
        getCustomPluginIcon(itemType, pluginId, taskPluginOverviews) :
        undefined
    return customPluginIcon ?
        // TODO: CMEM-5002: Replace img element with custom component to display data-URL icons
        <img src={customPluginIcon} alt={""} /> :
        <Icon name={createIconNameStack(itemType, pluginId)} {...size} />;
};
