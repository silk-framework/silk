import React, { useState, useEffect } from "react";
import { ContextOverlay, Icon, IconButton, Spacing, Notification, NotificationProps } from "@eccenca/gui-elements";
import { useNotificationsQueue } from "../../ApplicationNotifications/NotificationsMenu";
import { RuleSaveNodeError } from "../RuleEditor.typings";
import { useTranslation } from "react-i18next";
import { RuleEditorEvaluationNotification } from "../contexts/RuleEditorEvaluationContext";

interface RuleEditorNotificationsProps {
    integratedView?: boolean;
    queueEditorNotifications?: string[];
    queueNodeNotifications?: RuleSaveNodeError[];
    nodeJumpToHandler: any; // TODO
    /** Notifications from the rule evaluation. */
    evaluationNotifications?: RuleEditorEvaluationNotification[];
}

export const RuleEditorNotifications = ({
    integratedView = false,
    queueEditorNotifications = [] as string[],
    queueNodeNotifications = [] as RuleSaveNodeError[],
    nodeJumpToHandler,
    evaluationNotifications,
}: RuleEditorNotificationsProps) => {
    const [isOpen, setIsOpen] = useState<boolean>(false);
    const initTimestamp = React.useRef(Date.now());
    const { messages, notifications } = useNotificationsQueue();
    const [t] = useTranslation();
    const diErrorMessages = messages.filter((diError) => diError.timestamp > initTimestamp.current);

    useEffect(() => {
        setIsOpen(!!integratedView);
    }, [diErrorMessages.length > 0 ? diErrorMessages[0] : undefined]);

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

    return queueEditorNotifications.length > 0 ||
        queueNodeNotifications.length > 0 ||
        (integratedView && diErrorMessages.length > 0) ||
        (evaluationNotifications && evaluationNotifications.length) ? (
        <>
            <Spacing vertical size="tiny" />
            <ContextOverlay
                isOpen={isOpen}
                onClose={() => toggleNotifications(true)}
                rootBoundary="viewport"
                content={
                    <div
                        data-test-id={"ruleEditorToolbar-saveError-Btn"}
                        style={{ maxWidth: "39vw", padding: "0.5rem" }}
                    >
                        {integratedView && notifications}
                        {queueEditorNotifications.map((editorNotification) => (
                            <Notification danger={true} key={"errorMessage"} icon={<Icon name="state-warning" />}>
                                {editorNotification}
                            </Notification>
                        ))}
                        {evaluationNotifications &&
                            evaluationNotifications.length &&
                            evaluationNotifications.map((notification) => {
                                const intentObject: Pick<
                                    NotificationProps,
                                    "danger" | "warning" | "success" | "neutral"
                                > = Object.create(null);
                                if (notification.intent !== "none") {
                                    intentObject[notification.intent] = true;
                                }
                                return (
                                    <Notification
                                        {...intentObject}
                                        onDismiss={(didTimeoutExpire) =>
                                            !didTimeoutExpire && notification.onDiscard?.()
                                        }
                                    >
                                        {notification.message}
                                    </Notification>
                                );
                            })}
                        {queueNodeNotifications.map((nodeNotification) => (
                            <div key={nodeNotification.nodeId}>
                                <Spacing size={"tiny"} />
                                <Notification
                                    warning={true}
                                    icon={<Icon name="state-warning" />}
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
                }
            >
                <Icon
                    name="application-warning"
                    data-test-id="evaluation-warning-icon"
                    onClick={() => toggleNotifications()}
                />
            </ContextOverlay>
            <Spacing vertical size="tiny" />
        </>
    ) : null;
};
