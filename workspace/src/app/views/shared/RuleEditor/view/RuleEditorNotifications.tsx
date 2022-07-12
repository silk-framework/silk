import React, { useState, useEffect } from "react";
import { ContextOverlay, Icon, IconButton, Spacing, Notification } from "@eccenca/gui-elements";
import { useNotificationsQueue } from "../../ApplicationNotifications/NotificationsMenu";
import { RuleSaveNodeError } from "../RuleEditor.typings";
import { useTranslation } from "react-i18next";

interface RuleEditorNotificationsProps {
    integratedView?: boolean;
    queueEditorNotifications?: string[];
    queueNodeNotifications?: RuleSaveNodeError[];
    nodeJumpToHandler: any; // TODO
}

export const RuleEditorNotifications = ({
    integratedView = false,
    queueEditorNotifications = [] as string[],
    queueNodeNotifications = [] as RuleSaveNodeError[],
    nodeJumpToHandler
}: RuleEditorNotificationsProps) => {
    const [isOpen, setIsOpen] = useState<boolean>(false);
    const diNotifications = useNotificationsQueue();
    const [t] = useTranslation();

    useEffect(() => {
        setIsOpen(true && integratedView);
    }, [diNotifications.messages.length > 0 ? diNotifications.messages[0] : undefined]);

    useEffect(() => {
        setIsOpen(true);
    }, [queueEditorNotifications.length > 0 ? queueEditorNotifications[0] : undefined]);

    useEffect(() => {
        setIsOpen(true);
    }, [queueNodeNotifications.length > 0 ? queueNodeNotifications[0] : undefined]);

    const toggleNotifications = (forceClose: boolean = false) => {
        if (forceClose) {
            setIsOpen(false);
        } else {
            setIsOpen(!isOpen);
        }
    };

    return (
        queueEditorNotifications.length > 0 ||
        queueNodeNotifications.length > 0 ||
        (integratedView && diNotifications.messages.length > 0)
    ) ? (
        <>
            <Spacing vertical size="tiny" />
            <ContextOverlay
                data-test-id={"ruleEditorToolbar-saveError-Btn"}
                isOpen={isOpen}
                onClose={() => toggleNotifications(true)}
                rootBoundary="viewport"
                content={(
                    <div style={{maxWidth: "39vw", padding: "0.5rem"}}>
                        {integratedView && diNotifications.notifications}
                        {queueEditorNotifications.map((editorNotification) => (
                            <Notification danger={true} key={"errorMessage"} iconName="state-warning">
                                {editorNotification}
                            </Notification>
                        ))}
                        {queueNodeNotifications.map((nodeNotification) => (
                            <div key={nodeNotification.nodeId}>
                                <Spacing size={"tiny"} />
                                <Notification
                                    warning={true}
                                    iconName="state-warning"
                                    actions={
                                        <IconButton
                                            data-test-id={"RuleEditorToolbar-nodeError-btn"}
                                            name="item-viewdetails"
                                            text={t("RuleEditor.toolbar.saveError.nodeError.tooltip")}
                                            onClick={() => {
                                                nodeJumpToHandler(nodeNotification.nodeId);
                                            }}
                                        />
                                    }
                                >
                                    <p>{nodeNotification.message}</p>
                                </Notification>
                            </div>
                        ))}
                    </div>
                )}
            >
                <Icon name="application-warning" onClick={() => toggleNotifications()} />
            </ContextOverlay>
            <Spacing vertical size="tiny" />
        </>
    ) : (
        null
    )
}
