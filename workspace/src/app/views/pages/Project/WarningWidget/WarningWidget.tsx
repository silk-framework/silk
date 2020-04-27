import React, { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { workspaceOp, workspaceSel } from "@ducks/workspace";
import { Intent } from "@wrappers/blueprint/constants";
import { Card, CardContent, CardHeader, CardTitle, Divider, Icon } from "@wrappers/index";
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

    return (
        <Card>
            <CardHeader>
                <CardTitle>
                    <h3>Warning</h3>
                </CardTitle>
            </CardHeader>
            {isLoading ? (
                <Loading />
            ) : (
                <>
                    <Divider />
                    <CardContent>
                        {warningList.map((warn) => (
                            <div>
                                {warn.errorSummary}
                                <Icon name="item-info" onClick={() => handleOpenMarkDown(warn.taskId)} />
                            </div>
                        ))}
                        <MarkdownModal isOpen={isOpen} onDiscard={handleClose} markdown={currentMarkdown} />
                    </CardContent>
                </>
            )}
        </Card>
    );
};
