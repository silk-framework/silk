import React from "react";
import { useTranslation } from "react-i18next";
import { TaskActivityOverview } from "../TaskActivityOverview/TaskActivityOverview";
import { HeaderPopoverButton } from "./HeaderPopoverButton";

interface IProps {
    projectId: string;
    taskId: string;
}

/** Page header button showing the task's activity overview (status + controls) in a popover.
 * The overview (and its websocket updates) is only mounted while the popover is open. */
export const TaskActivitiesMenu = ({ projectId, taskId }: IProps) => {
    const [t] = useTranslation();
    return (
        <HeaderPopoverButton
            icon="application-activities"
            title={t("widget.TaskActivityOverview.title", "Activity overview")}
            data-test-id="header-activities-menu"
        >
            <TaskActivityOverview projectId={projectId} taskId={taskId} />
        </HeaderPopoverButton>
    );
};
