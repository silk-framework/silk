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
} from "@gui-elements/index";
import useErrorHandler from "../../../hooks/useErrorHandler";
import { useSelector } from "react-redux";
import errorSelector from "@ducks/error/selectors";
import { DIErrorFormat, DIErrorTypes } from "@ducks/error/typings";
import { ErrorResponse, FetchError } from "../../../services/fetch/responseInterceptor";

export function NotificationsMenu() {
    // condition: first message in array is handled as latest message, otherwise reverse it first
    const { clearErrors } = useErrorHandler();
    const [displayNotifications, setDisplayNotifications] = useState<boolean>(false);
    const [displayLastNotification, setDisplayLastNotification] = useState<boolean>(false);
    const { errors } = useSelector(errorSelector);
    //first message is the latest entry based on the timestamp
    const messages = [...errors].sort((a, b) => b.timestamp - a.timestamp); //https://stackoverflow.com/questions/53420055/

    useEffect(() => {
        if (messages.length) {
            setDisplayLastNotification(true);
            const timeout: number = window.setTimeout(async () => {
                setDisplayLastNotification(false);
            }, 6000);
            return () => {
                clearTimeout(timeout);
            };
        } else {
            setDisplayLastNotification(false);
            setDisplayNotifications(false);
        }
    }, [messages.length > 0 ? messages[0] : undefined]);

    /***** remove one or all messages *****/
    const removeMessages = (error?: DIErrorFormat) => {
        if (error) {
            clearErrors([error.id]);
        } else {
            clearErrors();
            setDisplayNotifications(false);
        }
    };

    const toggleNotifications = () => {
        setDisplayLastNotification(false);
        setDisplayNotifications(!displayNotifications);
    };

    const parseErrorCauseMsg = (cause?: DIErrorTypes | null): string | undefined => {
        //show cause if network error only
        return cause && cause instanceof FetchError && (cause.isNetworkError || cause.isHttpError)
            ? cause.message
            : undefined;
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

    const notificationIndicator =
        displayLastNotification && messages.length > 0 ? (
            <ContextOverlay
                isOpen={true}
                minimal={true}
                position="bottom-right"
                autoFocus={false}
                enforceFocus={false}
                openOnTargetFocus={false}
                content={
                    <Notification danger onDismiss={() => setDisplayLastNotification(false)}>
                        {messages[0].message}
                    </Notification>
                }
                target={notificationIndicatorButton}
            />
        ) : (
            notificationIndicatorButton
        );

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
                    {messages.map((item, id) => {
                        const errorDetails = parseErrorCauseMsg(item.cause);
                        return (
                            <div key={"message" + id}>
                                <Notification danger fullWidth onDismiss={() => removeMessages(item)}>
                                    {`${item.message} (${formatDuration(now.getTime() - item.timestamp)} ago)`}
                                    <Spacing size="small" />
                                    {errorDetails ? (
                                        <Accordion>
                                            <AccordionItem
                                                title={<TitleSubsection>More details</TitleSubsection>}
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
                    })}
                </ApplicationToolbarPanel>
            )}
        </>
    ) : (
        <></>
    );
}
