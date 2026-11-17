import React, { useState, useEffect, useRef } from "react";
import { ContextOverlay, Icon, IconButton, Spacing, Notification, NotificationProps } from "@eccenca/gui-elements";
import { useNotificationsQueue } from "../../ApplicationNotifications/NotificationsMenu";
import { RULE_EDITOR_NOTIFICATION_INSTANCE, RuleSaveNodeError } from "../RuleEditor.typings";
import { useTranslation } from "react-i18next";
import { RuleEditorEvaluationNotification } from "../contexts/RuleEditorEvaluationContext";

interface RuleEditorNotificationsProps {
    queueEditorNotifications?: string[];
    saveWarningMessages?: string[];
    queueNodeNotifications?: RuleSaveNodeError[];
    nodeJumpToHandler: any; // TODO
    /** Notifications from the rule evaluation. */
    evaluationNotifications?: RuleEditorEvaluationNotification[];
    /** Only notifications more current than the given date time value are shown. */
    generalNotificationMinDateTime: number;
}

export const RuleEditorNotifications = ({
    queueEditorNotifications = [] as string[],
    saveWarningMessages = [] as string[],
    queueNodeNotifications = [] as RuleSaveNodeError[],
    nodeJumpToHandler,
    evaluationNotifications,
    generalNotificationMinDateTime,
}: RuleEditorNotificationsProps) => {
    const [isOpen, setIsOpen] = useState<boolean>(false);
    const warningAutoCloseTimeoutRef = useRef<number | undefined>(undefined);
    const { messages, notifications } = useNotificationsQueue(RULE_EDITOR_NOTIFICATION_INSTANCE);
    const [t] = useTranslation();
    const ruleEditorErrorMessages = messages.filter((diError) => diError.timestamp > generalNotificationMinDateTime);
    const hasBlockingErrors =
        ruleEditorErrorMessages.length > 0 || queueEditorNotifications.length > 0 || queueNodeNotifications.length > 0;

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

    useEffect(() => {
        if (saveWarningMessages.length) {
            setIsOpen(true);
        }
    }, [saveWarningMessages.join("\n")]);

    useEffect(() => {
        window.clearTimeout(warningAutoCloseTimeoutRef.current);
        warningAutoCloseTimeoutRef.current = undefined;
        if (!saveWarningMessages.length || hasBlockingErrors) {
            return;
        }
        warningAutoCloseTimeoutRef.current = window.setTimeout(() => {
            setIsOpen(false);
        }, 5000);
        return () => {
            window.clearTimeout(warningAutoCloseTimeoutRef.current);
            warningAutoCloseTimeoutRef.current = undefined;
        };
    }, [hasBlockingErrors, saveWarningMessages]);

    const toggleNotifications = (forceClose: boolean = false) => {
        if (forceClose) {
            setIsOpen(false);
        } else {
            setIsOpen(!isOpen);
        }
    };

    return queueEditorNotifications.length > 0 ||
        saveWarningMessages.length > 0 ||
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
                        {/* Safari needs the explicit role when list markers are hidden. */}
                        {/* eslint-disable-next-line jsx-a11y/no-redundant-roles */}
                        <ul role="list" style={{ listStyle: "none", margin: 0, padding: 0 }}>
                            {ruleEditorErrorMessages.length > 0 ? notifications : null}
                            {queueEditorNotifications.map((editorNotification, index) => (
                                <li key={`editor-${index}`}>
                                    <Notification intent="danger" icon={<Icon name="state-warning" />}>
                                        {editorNotification}
                                    </Notification>
                                </li>
                            ))}
                            {saveWarningMessages.map((warningMessage, index) => (
                                <li key={`saveWarning-${index}`}>
                                    <Notification intent="warning" icon={<Icon name="state-warning" />}>
                                        {warningMessage}
                                    </Notification>
                                </li>
                            ))}
                            {evaluationNotifications && evaluationNotifications.length > 0
                                ? evaluationNotifications.map((notification, index) => {
                                      const intentObject: Pick<NotificationProps, "intent"> = Object.create(null);
                                      if (notification.intent !== "none") {
                                          intentObject[notification.intent] = notification.intent;
                                      }
                                      return (
                                          <li key={`evaluation-${index}`}>
                                              <Notification
                                                  {...intentObject}
                                                  onDismiss={(didTimeoutExpire) =>
                                                      !didTimeoutExpire && notification.onDiscard?.()
                                                  }
                                              >
                                                  {notification.message}
                                              </Notification>
                                          </li>
                                      );
                                  })
                                : null}
                            {queueNodeNotifications.map((nodeNotification) => (
                                <li key={nodeNotification.nodeId}>
                                    <Spacing size={"tiny"} />
                                    <Notification
                                        intent="warning"
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
                                </li>
                            ))}
                        </ul>
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
