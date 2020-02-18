import React, { useEffect, useState } from "react";
import Card from "../../../../../wrappers/card";
import { useDispatch, useSelector } from "react-redux";
import { workspaceOp, workspaceSel } from "@ducks/workspace";
import { IconNames, Intent } from "@wrappers/constants";
import Icon from "@wrappers/icon";
import MarkdownModal from "../../../../components/modals/MarkdownModal";
import { AppToaster } from "../../../../../services/toaster";

const ConfigurationWidget = () => {
    const dispatch = useDispatch();
    const projectId = useSelector(workspaceSel.currentProjectIdSelector);
    const warningList = useSelector(workspaceSel.warningListSelector);

    const [currentMarkdown, setCurrentMarkdown] = useState('');
    const [isOpen, setIsOpen] = useState<boolean>(false);

    useEffect(() => {
        getWarningList();
    }, []);

    const handleOpen = () => setIsOpen(true);
    const handleClose = () => {
        setCurrentMarkdown('');
        setIsOpen(false);
    };

    const getWarningList = () => {
        dispatch(workspaceOp.fetchWarningListAsync());
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
                timeout: 2000
            });
        }
    };

    return (
        <Card>
            <h3>Warning</h3>
            <div>
                {
                    warningList.map(warn =>
                        <div>
                            {warn.errorSummary}
                            <Icon icon={IconNames.INFO_SIGN} onClick={() => handleOpenMarkDown(warn.taskId)}/>
                        </div>
                    )
                }
                <MarkdownModal isOpen={isOpen} onDiscard={handleClose} markdown={currentMarkdown}/>
            </div>
        </Card>
    )
};

export default ConfigurationWidget;
