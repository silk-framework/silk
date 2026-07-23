import React, { useEffect, useState } from "react";
import {
    ApplicationToolbarPanel,
    Badge,
    Button,
    cn,
    ContextOverlay,
    Divider,
    Icon,
    Spacing,
    useApplicationHeaderOverModals,
} from "@eccenca/gui-elements";
import useErrorHandler from "../../../hooks/useErrorHandler";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";
import errorSelector from "@ducks/error/selectors";
import { ApplicationError, DIErrorFormat, DIErrorTypes } from "@ducks/error/typings";
import { ErrorResponse, FetchError } from "../../../services/fetch/responseInterceptor";
import { ApplicationNotification } from "./ApplicationNotification";
import { commonOp, commonSel } from "@ducks/common";

interface Props {
    /** When true the last notification will be shown for some seconds. */
    autoDisplayNotifications?: boolean;
    /** The unique instance ID of this notification menu. This allows to send specific errors only to this instance. */
    errorNotificationInstanceId?: string;
}

export function NotificationsMenu({ autoDisplayNotifications = true, errorNotificationInstanceId }: Props) {
    const displayNotifications = useSelector(commonSel.notificationMenuSelector);
    const dispatch = useDispatch();
    const [t] = useTranslation();

    const notificationQueue = useNotificationsQueue(errorNotificationInstanceId, autoDisplayNotifications);

    useApplicationHeaderOverModals(
        notificationQueue.messages.length > 0,
        "diapp-applicationnotifications--filledqueue",
    );

    const toggleNotifications = () => {
        dispatch(commonOp.toggleNotificationMenuDisplay(!displayNotifications));
    };

    // The bell is a permanent part of the header chrome, so it renders regardless of whether there
    // are queued notifications. The count badge only appears when something is waiting; opening an
    // empty queue shows an explicit empty state.
    const notificationCount = notificationQueue.messages.length;

    // Ghost icon-button matching the header's other chrome actions (Help): 36px box, 20px icon,
    // stroke 2 — kept in sync with `headerActionButtonClass` in `Header.tsx`. The count badge sits
    // in the top-right corner of the relative button; opening the panel swaps in a close icon.
    const notificationIndicatorButton = (
        <button
            type="button"
            aria-label={displayNotifications ? "Close notifications menu" : "Open notifications menu"}
            onClick={() => {
                toggleNotifications();
            }}
            className={cn(
                "relative flex size-9 items-center justify-center rounded-lg text-foreground transition-colors",
                "hover:bg-muted focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/40",
                displayNotifications && "bg-muted text-foreground",
            )}
        >
            {displayNotifications ? (
                <Icon name="navigation-close" title="Close icon" small />
            ) : (
                <>
                    <Icon name="application-notification" title="Notification menu icon" small />
                    {notificationCount > 0 && (
                        <Badge position={"top-right"} intent="danger" maxLength={2} children={notificationCount} />
                    )}
                </>
            )}
        </button>
    );

    const notificationIndicator =
        notificationQueue.displayLastNotification && notificationQueue.lastNotification ? (
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

    return (
        <>
            {notificationIndicator}
            {displayNotifications && (
                <ApplicationToolbarPanel
                    aria-label="Notification menu"
                    expanded={true}
                    style={{ width: "40rem" }}
                    className="top-15"
                >
                    {notificationCount > 0 ? (
                        <>
                            {notificationQueue.clearAllButton}
                            {notificationQueue.notifications}
                        </>
                    ) : (
                        <div className="text-sm text-muted-foreground">
                            {t("Notifications.empty", "You have no notifications.")}
                        </div>
                    )}
                </ApplicationToolbarPanel>
            )}
        </>
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
    errorNotificationInstanceId?: string,
): boolean => {
    return (
        !!message &&
        (message.errorNotificationInstanceId == null ||
            message.errorNotificationInstanceId === errorNotificationInstanceId)
    );
};

export function useNotificationsQueue(errorNotificationInstanceId?: string, autoDisplayNotifications: boolean = false) {
    // condition: first message in array is handled as latest message, otherwise reverse it first
    const { clearErrors } = useErrorHandler();
    const [displayLastNotification, setDisplayLastNotification] = useState<boolean>(false);
    const { errors } = useSelector(errorSelector);
    //first message is the latest entry based on the timestamp
    const messages = [...errors]
        .filter((error) => showMessage(error, errorNotificationInstanceId))
        .sort((a, b) => b.timestamp - a.timestamp); //https://stackoverflow.com/questions/53420055/
    const initTime = React.useRef(new Date().getTime());
    const displayNotifications = useSelector(commonSel.notificationMenuSelector);
    const dispatch = useDispatch();
    // If set, then the given message will be displayed as "last message" until the user closes it instead of being closed automatically after 6 seconds
    const displayMessageUntilClosed = React.useRef<ApplicationError | undefined>(undefined);

    React.useEffect(() => {
        if (displayNotifications) {
            setDisplayLastNotification(false);
        }
    }, [displayNotifications]);

    useEffect(() => {
        if (messages.length && messages[0].timestamp > initTime.current) {
            if (autoDisplayNotifications && !messages[0].notAutoOpen) {
                setDisplayLastNotification(true);
                const timeout: number = window.setTimeout(async () => {
                    if (displayMessageUntilClosed.current && displayMessageUntilClosed.current === messages[0]) {
                        return;
                    }
                    setDisplayLastNotification(false);
                }, 5000);
                return () => {
                    clearTimeout(timeout);
                };
            }
        } else {
            setDisplayLastNotification(false);
        }
    }, [messages.length > 0 ? messages[0] : undefined]);

    /***** remove one or all messages *****/
    const removeMessages = (error?: DIErrorFormat) => {
        if (error && messages.length > 1) {
            if (error === displayMessageUntilClosed.current) {
                displayMessageUntilClosed.current = undefined;
            }
            clearErrors([error.id]);
        } else {
            clearErrors();
            dispatch(commonOp.toggleNotificationMenuDisplay(false));
        }
    };

    const lastMessage: ApplicationError | undefined = messages[0];
    const lastNotification =
        displayLastNotification && lastMessage ? (
            <ApplicationNotification
                errorItem={lastMessage}
                removeError={() => {
                    //hide last message without removing from the queue
                    setDisplayLastNotification(false);
                    // removeMessages(lastMessage);
                }}
                interactionCallback={() => {
                    displayMessageUntilClosed.current = lastMessage;
                }}
                updateTimeDelay={200}
            />
        ) : null;

    const clearAllButton = (
        <>
            <Button text="Clear all messages" onClick={() => removeMessages()} />
            <Divider addSpacing="medium" />
        </>
    );

    const notifications = messages.map((item, id) => {
        return (
            <div key={"message" + id}>
                <ApplicationNotification errorItem={item} removeError={removeMessages} />
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
