import { IItemLink } from "@ducks/shared/typings";
import { ProjectTaskTabView } from "./ProjectTaskTabView";
import React, { useState } from "react";
import { pluginRegistry, ViewActionsTaskContext } from "../../../views/plugins/PluginRegistry";
import { MenuItem } from "@eccenca/gui-elements";
import { getItemLinkIcons } from "../../../utils/getItemLinkIcons";
import { useSelector } from "react-redux";
import { commonSel } from "@ducks/common";

interface IProps {
    srcLinks: IItemLink[];
    startLink?: IItemLink;
    pluginId?: string;
    projectId?: string;
    taskId?: string;
    /** Fetches the current view task context information. */
    fetchTaskContext?: () => ViewActionsTaskContext | undefined;
    /** Called when the task tab view is closed. Only valid when this is an modal version of the task tabs. */
    onCloseModal?: () => any;
    /** In case of a modal version of the task tabs, propagate this modal ID to the opened modal. */
    modalId?: string;
}

/** Shows custom views of a project task. */
export const useProjectTaskTabsView = ({
    srcLinks,
    startLink,
    pluginId,
    taskId,
    projectId,
    onCloseModal,
    fetchTaskContext,
    modalId
}: IProps) => {
    const [activeTab, setActiveTab] = useState<IItemLink | string | undefined>(startLink);
    const initialSettings = useSelector(commonSel.initialSettingsSelector);
    const taskViews = (pluginId ? pluginRegistry.taskViews(pluginId) : []).filter(
        (plugin) => !plugin.available || plugin.available(initialSettings)
    );
    const menuItems = taskViews.map(({ id, label }) => (
        <MenuItem
            data-test-id={id}
            key={id}
            text={label}
            icon={getItemLinkIcons(label)}
            onClick={() => changeTab(id)}
        />
    ));

    // handler for link change
    const changeTab = (linkItem?: IItemLink | string) => {
        setActiveTab(linkItem);
    };
    const taskViewConfig = pluginId ? { pluginId, taskId, projectId } : undefined;
    const taskContext = fetchTaskContext?.();
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
            handlerRemoveModal={() => {
                onCloseModal?.();
                changeTab(undefined);
            }}
            viewActions={{
                taskContext,
            }}
            modalId={modalId}
        />
    ) : null;

    return { projectTabView: returnElement, changeTab, menuItems };
};
