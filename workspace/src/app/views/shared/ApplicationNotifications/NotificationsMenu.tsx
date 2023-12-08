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
    TitleSubsection,
    Accordion,
    AccordionItem,
    Badge,
    Depiction,
} from "@eccenca/gui-elements";
import useErrorHandler from "../../../hooks/useErrorHandler";
import { useSelector } from "react-redux";
import errorSelector from "@ducks/error/selectors";
import { DIErrorFormat, DIErrorTypes } from "@ducks/error/typings";
import { ErrorResponse, FetchError } from "../../../services/fetch/responseInterceptor";

interface Props {
    /** When true the last notification will be shown for some seconds. */
    autoDisplayNotifications?: boolean;
    /** The unique instance ID of this notification menu. This allows to send specific errors only to this instance. */
    errorNotificationInstanceId?: string;
}

export function NotificationsMenu({ autoDisplayNotifications = true, errorNotificationInstanceId }: Props) {
    const [displayNotifications, setDisplayNotifications] = useState<boolean>(false);
    const [displayLastNotification, setDisplayLastNotification] = useState<boolean>(false);

    const notificationQueue = useNotificationsQueue(errorNotificationInstanceId);

    useEffect(() => {
        if (notificationQueue.displayLastNotification) {
            if (autoDisplayNotifications) {
                setDisplayLastNotification(true);
                const timeout: number = window.setTimeout(async () => {
                    setDisplayLastNotification(false);
                }, 6000);
                return () => {
                    clearTimeout(timeout);
                };
            }
        } else {
            setDisplayLastNotification(false);
            setDisplayNotifications(false);
        }
    }, [notificationQueue.displayLastNotification]);

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
            <Depiction
                padding="medium"
                ratio="1:1"
                resizing="contain"
                forceInlineSvg
                image={<Icon name="application-warning" description="Notification menu icon" />}
                badge={<Badge position={"bottom-right"}>{notificationQueue.notifications.length}</Badge>}
            />
        </ApplicationToolbarAction>
    );

    const showLastNotification = displayLastNotification && notificationQueue.lastNotification;

    const notificationIndicator = showLastNotification ? (
        <ContextOverlay
            isOpen={true}
            minimal={true}
            placement="bottom-end"
            autoFocus={false}
            enforceFocus={false}
            openOnTargetFocus={false}
            content={notificationQueue.lastNotification}
        >
            {notificationIndicatorButton}
        </ContextOverlay>
    ) : (
        notificationIndicatorButton
    );

    const filteredMessages = notificationQueue.messages.filter((m) => showMessage(m));

    return filteredMessages.length > 0 ? (
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
                    {notificationQueue.clearAllButton}
                    {notificationQueue.notifications}
                </ApplicationToolbarPanel>
            )}
        </>
    ) : (
        <></>
    );
}

/** Returns the error details of the top-most error */
export const parseErrorCauseMsg = (cause?: DIErrorTypes | null): string | undefined => {
    const asString = (cause as FetchError | ErrorResponse)?.asString;
    return asString ? asString() : (cause as { message?: string })?.message;
};

/** Decide if to show a message based on the instance ID. */
const showMessage = (
    message?: DIErrorFormat & { errorNotificationInstanceId?: string },
    errorNotificationInstanceId?: string
): boolean => {
    return (
        !!message &&
        (message.errorNotificationInstanceId == null ||
            message.errorNotificationInstanceId === errorNotificationInstanceId)
    );
};

export function useNotificationsQueue(errorNotificationInstanceId?: string) {
    // condition: first message in array is handled as latest message, otherwise reverse it first
    const { clearErrors } = useErrorHandler();
    const [displayLastNotification, setDisplayLastNotification] = useState<boolean>(false);
    const { errors } = useSelector(errorSelector);
    //first message is the latest entry based on the timestamp
    const messages = [...errors]
        .filter((error) => showMessage(error, errorNotificationInstanceId))
        .sort((a, b) => b.timestamp - a.timestamp); //https://stackoverflow.com/questions/53420055/
    const initTime = React.useRef(new Date().getTime());

    useEffect(() => {
        if (messages.length && messages[0].timestamp > initTime.current) {
            setDisplayLastNotification(true);
            const timeout: number = window.setTimeout(async () => {
                setDisplayLastNotification(false);
            }, 6000);
            return () => {
                clearTimeout(timeout);
            };
        } else {
            setDisplayLastNotification(false);
        }
    }, [messages.length > 0 ? messages[0] : undefined]);

    /***** remove one or all messages *****/
    const removeMessages = (error?: DIErrorFormat) => {
        if (error) {
            clearErrors([error.id]);
        } else {
            clearErrors();
        }
    };

    const lastNotification =
        displayLastNotification && messages.length > 0 ? (
            <Notification
                danger={!messages[0].alternativeIntent}
                warning={messages[0].alternativeIntent === "warning"}
                onDismiss={() => setDisplayLastNotification(false)}
            >
                {messages[0].message}
            </Notification>
        ) : null;

    const now = new Date();

    // Formats the duration since the error happened. Currently only goes up to minutes, since we don't expect users to keep errors that long.
    const formatDuration = (durationInMs: number): string => {
        if (durationInMs < 1000) {
            return "1s";
        } else if (durationInMs < 60 * 1000) {
            return Math.round(durationInMs / 1000) + "s";
        } else {
            return Math.round(durationInMs / (60 * 1000)) + " minute" + (durationInMs >= 120 * 1000 ? "s" : "");
        }
    };

    const clearAllButton = (
        <>
            <Button text="Clear all messages" onClick={() => removeMessages()} />
            <Divider addSpacing="medium" />
        </>
    );

    const notifications = messages.map((item, id) => {
        const errorDetails = parseErrorCauseMsg(item.cause);
        return (
            <div key={"message" + id}>
                <Notification
                    danger={!item.alternativeIntent}
                    warning={item.alternativeIntent === "warning"}
                    fullWidth
                    onDismiss={() => removeMessages(item)}
                >
                    {`${item.message} (${formatDuration(now.getTime() - item.timestamp)} ago)`}
                    <Spacing size="small" />
                    {errorDetails ? (
                        <Accordion>
                            <AccordionItem
                                label={<TitleSubsection>More details</TitleSubsection>}
                                elevated
                                condensed
                                open={false}
                            >
                                {errorDetails}
                            </AccordionItem>
                        </Accordion>
                    ) : null}
                </Notification>
                <Spacing size="small" />
            </div>
        );
    });

    return {
        displayLastNotification,
        lastNotification,
        notifications,
        clearAllButton,
        messages,
    } as const;
}
