import React, { useState, useEffect } from "react";
import { ContextOverlay, Icon, IconButton, Spacing, Notification, NotificationProps } from "@eccenca/gui-elements";
import { useNotificationsQueue } from "../../ApplicationNotifications/NotificationsMenu";
import { RULE_EDITOR_NOTIFICATION_INSTANCE, RuleSaveNodeError } from "../RuleEditor.typings";
import { useTranslation } from "react-i18next";
import { RuleEditorEvaluationNotification } from "../contexts/RuleEditorEvaluationContext";

interface RuleEditorNotificationsProps {
    queueEditorNotifications?: string[];
    queueNodeNotifications?: RuleSaveNodeError[];
    nodeJumpToHandler: any; // TODO
    /** Notifications from the rule evaluation. */
    evaluationNotifications?: RuleEditorEvaluationNotification[];
    /** Only notifications more current than the given date time value are shown. */
    generalNotificationMinDateTime: number;
}

export const RuleEditorNotifications = ({
    queueEditorNotifications = [] as string[],
    queueNodeNotifications = [] as RuleSaveNodeError[],
    nodeJumpToHandler,
    evaluationNotifications,
    generalNotificationMinDateTime,
}: RuleEditorNotificationsProps) => {
    const [isOpen, setIsOpen] = useState<boolean>(false);
    const { messages, notifications } = useNotificationsQueue(RULE_EDITOR_NOTIFICATION_INSTANCE);
    const [t] = useTranslation();
    const ruleEditorErrorMessages = messages.filter((diError) => diError.timestamp > generalNotificationMinDateTime);

    useEffect(() => {
        if (ruleEditorErrorMessages.length && !ruleEditorErrorMessages[0]?.notAutoOpen) {
            setIsOpen(true);
        }
    }, [ruleEditorErrorMessages.length > 0 ? ruleEditorErrorMessages[0] : undefined]);

    useEffect(() => {
        if (queueEditorNotifications.length) {
            setIsOpen(true);
        }
    }, [queueEditorNotifications.length > 0 ? queueEditorNotifications[0] : undefined]);

    useEffect(() => {
        if (queueNodeNotifications.length) {
            setIsOpen(true);
        }
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
        ruleEditorErrorMessages.length > 0 ||
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
                        {ruleEditorErrorMessages.length > 0 ? notifications : null}
                        {queueEditorNotifications.map((editorNotification) => (
                            <Notification danger={true} key={"errorMessage"} icon={<Icon name="state-warning" />}>
                                {editorNotification}
                            </Notification>
                        ))}
                        {evaluationNotifications && evaluationNotifications.length > 0
                            ? evaluationNotifications.map((notification) => {
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
                              })
                            : null}
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
