import React from "react";
import { Notification, Spacing, WorkspaceContent } from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";

interface ForbiddenNotificationProps {
    detail?: string;
}

export function ProjectForbiddenNotification({ detail }: ForbiddenNotificationProps) {
    const [t] = useTranslation();
    return (
        <WorkspaceContent>
            <Notification intent="warning">
                {t("pages.project.errors.forbidden", "Access is forbidden")}
                <Spacing size={"small"} />
                {detail ? ` ${detail}` : null}
            </Notification>
        </WorkspaceContent>
    );
}
