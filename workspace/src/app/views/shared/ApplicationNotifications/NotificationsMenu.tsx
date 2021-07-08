import React, { useState, useEffect } from "react";
import {
    ApplicationToolbarAction,
    ApplicationToolbarPanel,
    Icon,
    Notification,
    Spacing,
    Divider,
    Button,
    ContextOverlay,
} from "@gui-elements/index";
import { DIErrorFormat } from "@ducks/error/typings";
import useErrorHandler from "../../../hooks/useErrorHandler";
import { useSelector } from "react-redux";
import errorSelector from "@ducks/error/selectors";

export function NotificationsMenu() {
    // condition: first message in array is handled as latest message, otherwise reverse it first
    const { clearErrors } = useErrorHandler();
    const [startTime, updateStartTime] = useState<number>(Date.now());
    const [displayNotifications, setDisplayNotifications] = useState<boolean>(false);
    const [displayLastNotification, setDisplayLastNotification] = useState<boolean>(false);
    const { errors: messages } = useSelector(errorSelector);

    useEffect(() => {
        if (messages.length > 0 && startTime <= messages[0].timestamp) {
            setDisplayLastNotification(true);
        } else {
            setDisplayLastNotification(false);
        }
    }, [messages]);

    const removeMessages = (errorId?: string) => {
        errorId ? clearErrors([errorId]) : clearErrors();
        setDisplayNotifications(false);
    };

    // const removeMessage = (item) => {
    //     const id = messages.indexOf(item);
    //     if (id !== -1) {
    //         messages.splice(id, 1);
    //     }
    //     if (messages.length < 1) {
    //         setDisplayNotifications(false);
    //     }
    //     setMessages(messages);
    // };

    // const removeAllMessages = () => {
    //     messages.splice(0);
    //     setMessages(messages);
    //     setDisplayNotifications(false);
    // };

    const toggleNotifications = () => {
        setDisplayLastNotification(false);
        setDisplayNotifications(!displayNotifications);
    };

    const notificationIndicatorButton = (
        <ApplicationToolbarAction
            aria-label="Open notifications menu"
            isActive={false}
            onClick={() => {
                toggleNotifications();
            }}
        >
            <Icon name="application-warning" description="Notification menu icon" large />
        </ApplicationToolbarAction>
    );

    const notificationIndicator = displayLastNotification ? (
        <ContextOverlay
            isOpen={true}
            minimal={true}
            position="bottom-right"
            autoFocus={false}
            enforceFocus={false}
            openOnTargetFocus={false}
            content={
                <Notification danger timeout={6000} onDismiss={() => setDisplayLastNotification(false)}>
                    {messages[0].message}
                </Notification>
            }
            target={notificationIndicatorButton}
        />
    ) : (
        notificationIndicatorButton
    );

    return messages.length > 0 ? (
        <>
            {!displayNotifications && notificationIndicator}
            {displayNotifications && (
                <ApplicationToolbarAction
                    aria-label="Close notifications menu"
                    isActive={true}
                    onClick={() => {
                        toggleNotifications();
                    }}
                >
                    <Icon name="navigation-close" description="Close icon" large />
                </ApplicationToolbarAction>
            )}
            {displayNotifications && (
                <ApplicationToolbarPanel aria-label="Notification menu" expanded={true} style={{ width: "40rem" }}>
                    <Button text="Clear all messages" onClick={() => removeMessages()} />
                    <Divider addSpacing="medium" />
                    {messages.map((item, id) => (
                        <div key={"message" + id}>
                            <Notification danger fullWidth onDismiss={() => removeMessages(item.id)}>
                                {item.message}
                            </Notification>
                            <Spacing size="small" />
                        </div>
                    ))}
                </ApplicationToolbarPanel>
            )}
        </>
    ) : (
        <></>
    );
}
