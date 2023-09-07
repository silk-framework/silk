import React from "react";
import { Depiction, Icon } from "@eccenca/gui-elements";
import { useSelector } from "react-redux";
import { commonSel } from "@ducks/common";
import { IPluginOverview } from "@ducks/common/typings";
import { convertTaskTypeToItemType, TaskType } from "@ducks/shared/typings";

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

const customPluginIcon: { artefactList?: IPluginOverview[]; iconMap: Map<string, string> } = {
    iconMap: new Map(),
};

const taskTypeSet: Set<TaskType> = new Set(["Dataset", "Linking", "Transform", "Workflow", "CustomTask"]);
const pluginKey = (itemType: string, pluginId: string): string => `${itemType} ${pluginId}`;

/** Gets the custom item type from the icon store. */
export const getCustomPluginIconFromStore = (itemType: string, pluginId: string): string | undefined => {
    const correctItemType = fixItemType(itemType);
    return customPluginIcon.iconMap.get(pluginKey(correctItemType, pluginId));
};

const fixItemType = (itemType: string) => {
    let returnType = itemType;
    if (taskTypeSet.has(itemType as TaskType)) {
        // Item type is a task type and needs to be converted
        returnType = convertTaskTypeToItemType(itemType as TaskType);
    }
    if (returnType.charAt(0).toLowerCase() !== returnType.charAt(0)) {
        returnType = itemType.toLowerCase();
    }
    return returnType;
};

export const fillCustomPluginStore = async (artefactList: IPluginOverview[]) => {
    if (artefactList !== customPluginIcon.artefactList) {
        // Add icons to map
        customPluginIcon.iconMap = new Map();
        for (let i = 0; i < artefactList.length; i++) {
            const plugin = artefactList[i];
            if (plugin.pluginIcon && (await validateDataUrl(plugin))) {
                customPluginIcon.iconMap.set(
                    pluginKey(convertTaskTypeToItemType(plugin.taskType), plugin.key),
                    plugin.pluginIcon
                );
            }
        }
        customPluginIcon.artefactList = artefactList;
    }
};

const getCustomPluginIcon = async (
    itemType: string,
    pluginId: string,
    artefactList: IPluginOverview[] | undefined
): Promise<string | undefined> => {
    if (artefactList) {
        await fillCustomPluginStore(artefactList);
    }
    return getCustomPluginIconFromStore(itemType, pluginId);
};

const validateDataUrl = async (plugin: IPluginOverview): Promise<boolean> => {
    try {
        const imageObj = new Image();
        imageObj.src = plugin.pluginIcon!;
        await imageObj.decode();
        return true;
    } catch (ex) {
        console.warn(`Plugin '${plugin.title ?? plugin.key}' has an invalid icon data URL!`);
        return false;
    }
};

/** Item icon derived from the item type and optionally the plugin ID. */
export const ItemDepiction = ({ itemType, pluginId, size = { large: true } }: IProps) => {
    const [customPluginIcon, setCustomPluginIcon] = React.useState<string | undefined>(undefined);
    const taskPluginOverviews = useSelector(commonSel.taskPluginOverviewsSelector);

    const fetchValidatedCustomPluginIcon = React.useCallback(async () => {
        if (itemType && pluginId) {
            const icon = await getCustomPluginIcon(itemType, pluginId, taskPluginOverviews);
            setCustomPluginIcon(icon);
        }
    }, [taskPluginOverviews, itemType, pluginId]);

    React.useEffect(() => {
        fetchValidatedCustomPluginIcon();
    }, [taskPluginOverviews]);

    return customPluginIcon ? (
        <Depiction
            image={<img src={customPluginIcon} alt={""} />}
            ratio="1:1"
            resizing="contain"
            forceInlineSvg={true}
            size={size?.small ? "tiny" : "source"}
        />
    ) : (
        <Icon name={createIconNameStack(itemType, pluginId)} {...size} />
    );
};
