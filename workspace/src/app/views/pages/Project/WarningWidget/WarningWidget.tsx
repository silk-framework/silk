import React, { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { workspaceOp, workspaceSel } from "@ducks/workspace";
import { Intent } from "@eccenca/gui-elements/blueprint/constants";
import {
    Card,
    CardContent,
    CardHeader,
    CardTitle,
    Divider,
    Button,
    Notification,
    Spacing,
} from "@eccenca/gui-elements";
import MarkdownModal from "../../../shared/modals/MarkdownModal";
import { AppToaster } from "../../../../services/toaster";
import { commonSel } from "@ducks/common";
import Loading from "../../../shared/Loading";
import { useTranslation } from "react-i18next";

export const WarningWidget = () => {
    const dispatch = useDispatch();
    const projectId = useSelector(commonSel.currentProjectIdSelector);
    const warningList = useSelector(workspaceSel.warningListSelector);

    const warnWidget = useSelector(workspaceSel.widgetsSelector).warnings;
    const { isLoading } = warnWidget;

    const [currentMarkdown, setCurrentMarkdown] = useState("");
    const [isOpen, setIsOpen] = useState<boolean>(false);
    const [t] = useTranslation();

    useEffect(() => {
        if (projectId) {
            dispatch(workspaceOp.fetchWarningListAsync(projectId));
        }
    }, [workspaceOp, projectId]);

    const handleOpen = () => setIsOpen(true);
    const handleClose = () => {
        setCurrentMarkdown("");
        setIsOpen(false);
    };

    const handleOpenMarkDown = async (taskId, projectId) => {
        try {
            const markdown: string = await workspaceOp.fetchWarningMarkdownAsync(taskId, projectId);
            handleOpen();
            setCurrentMarkdown(markdown);
        } catch {
            AppToaster.show({
                message: t(
                    "http.error.not.markdown",
                    "Sorry but we can't find the markdown information for this report"
                ),
                intent: Intent.DANGER,
                timeout: 0,
            });
        }
    };

    if (isLoading) return <Loading description={t("widget.WarningWidget.loading", "Loading log messages.")} />;

    return warningList.length > 0 ? (
        <>
            <Spacing />
            <Card>
                <CardHeader>
                    <CardTitle>
                        <h2>{t("widget.WarningWidget.title", "Error log")}</h2>
                    </CardTitle>
                </CardHeader>
                <Divider />
                <CardContent>
                    <ul>
                        {warningList.map((warn, id) => (
                            <li key={"notification_" + id}>
                                <Notification
                                    danger
                                    actions={
                                        <Button
                                            minimal
                                            text={t("common.action.ShowSmth", { smth: "report" })}
                                            onClick={() => projectId && handleOpenMarkDown(warn.taskId, projectId)}
                                        />
                                    }
                                >
                                    {warn.errorSummary}
                                </Notification>
                                <Spacing size={"tiny"} />
                            </li>
                        ))}
                    </ul>
                    <MarkdownModal isOpen={isOpen} onDiscard={handleClose} markdown={currentMarkdown} />
                </CardContent>
            </Card>
        </>
    ) : null;
};
