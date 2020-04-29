import React, { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { workspaceOp, workspaceSel } from "@ducks/workspace";
import { Intent } from "@wrappers/blueprint/constants";
import { Card, CardContent, CardHeader, CardTitle, Divider, Button, Notification, Spacing } from "@wrappers/index";
import MarkdownModal from "../../../shared/modals/MarkdownModal";
import { AppToaster } from "../../../../services/toaster";
import { commonSel } from "@ducks/common";
import Loading from "../../../shared/Loading";

export const WarningWidget = () => {
    const dispatch = useDispatch();
    const projectId = useSelector(commonSel.currentProjectIdSelector);
    const warningList = useSelector(workspaceSel.warningListSelector);

    const warnWidget = useSelector(workspaceSel.widgetsSelector).warnings;
    const { isLoading } = warnWidget;

    const [currentMarkdown, setCurrentMarkdown] = useState("");
    const [isOpen, setIsOpen] = useState<boolean>(false);

    useEffect(() => {
        dispatch(workspaceOp.fetchWarningListAsync());
    }, [workspaceOp]);

    const handleOpen = () => setIsOpen(true);
    const handleClose = () => {
        setCurrentMarkdown("");
        setIsOpen(false);
    };

    const handleOpenMarkDown = async (taskId) => {
        try {
            const markdown: string = await workspaceOp.fetchWarningMarkdownAsync(projectId, taskId);
            handleOpen();
            setCurrentMarkdown(markdown);
        } catch {
            AppToaster.show({
                message: `Sorry but we can't find the markdown information for this report`,
                intent: Intent.DANGER,
                timeout: 0,
            });
        }
    };

    if (isLoading) return <Loading />;

    return warningList.length > 0 ? (
        <Card>
            <CardHeader>
                <CardTitle>
                    <h3>Error log</h3>
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
                                        text="Show report"
                                        onClick={() => handleOpenMarkDown(warn.taskId)}
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
    ) : null;
};
