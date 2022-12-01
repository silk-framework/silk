import { IItemLink } from "@ducks/shared/typings";
import { MenuItem } from "@eccenca/gui-elements";
import React, { useState } from "react";

import { getItemLinkIcons } from "../../../utils/getItemLinkIcons";
import { pluginRegistry } from "../../../views/plugins/PluginRegistry";
import { ProjectTaskTabView } from "./ProjectTaskTabView";

interface IProps {
    srcLinks: IItemLink[];
    startLink?: IItemLink;
    pluginId?: string;
    projectId?: string;
    taskId?: string;
}

/** An I-frame supported version for item links. */
export const useProjectTabsView = ({ srcLinks, startLink, pluginId, taskId, projectId }: IProps) => {
    const [activeTab, setActiveTab] = useState<IItemLink | string | undefined>(startLink);
    const taskViews = pluginId ? pluginRegistry.taskViews(pluginId) : [];
    const menuItems = taskViews.map(({ id, label }) => (
        <MenuItem key={id} text={label} icon={getItemLinkIcons(label)} onClick={() => changeTab(id)} />
    ));

    // handler for link change
    const changeTab = (linkItem?: IItemLink | string) => {
        setActiveTab(linkItem);
    };
    const taskViewConfig = pluginId ? { pluginId, taskId, projectId } : undefined;
    const returnElement: JSX.Element | null = activeTab ? (
        <ProjectTaskTabView
            srcLinks={srcLinks.map((link) => {
                return {
                    ...link,
                    itemType: undefined,
                };
            })}
            startWithLink={activeTab}
            startFullscreen={true}
            taskViewConfig={taskViewConfig}
            handlerRemoveModal={() => changeTab(undefined)}
        />
    ) : null;

    return { projectTabView: returnElement, changeTab, menuItems };
};
