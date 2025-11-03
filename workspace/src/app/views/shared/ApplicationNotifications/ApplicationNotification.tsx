import React from "react";
import { Accordion, AccordionItem, Notification, Spacing, TitleSubsection } from "@eccenca/gui-elements";
import { ApplicationError, DIErrorFormat } from "@ducks/error/typings";
import { parseErrorCauseMsg } from "./NotificationsMenu";

interface Props {
    /** The application warning/error. */
    errorItem: ApplicationError;
    /** Removes the error from the error queue and the user interface. */
    removeError: (error: DIErrorFormat) => any;
    /** If the user interacts with the notification, e.g. expands it details or clicks it, this function will be triggered. */
    interactionCallback?: () => any;
    /** Time in milliseconds, the time should be updated. Won't update when undefined. */
    updateTimeDelay?: number;
}

/** Notification about the application, usually warnings or errors that are not displayed locally in a widget. */
export const ApplicationNotification = ({ errorItem, removeError, interactionCallback, updateTimeDelay }: Props) => {
    const errorDetails = parseErrorCauseMsg(errorItem.cause);
    const [now, setNow] = React.useState(new Date());

    React.useEffect(() => {
        if (updateTimeDelay && updateTimeDelay > 0) {
            setTimeout(() => setNow(new Date()), updateTimeDelay);
        }
    }, [updateTimeDelay, now]);

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

    const onDismiss = React.useCallback(
        (didTimeoutExpire: boolean) => {
            if (!didTimeoutExpire) {
                removeError(errorItem);
            }
        },
        [removeError],
    );

    return (
        <Notification
            intent={
                !errorItem.alternativeIntent
                    ? "danger"
                    : errorItem.alternativeIntent === "warning"
                      ? "warning"
                      : undefined
            }
            flexWidth={true}
            onDismiss={onDismiss}
        >
            <div onMouseDownCapture={() => interactionCallback?.()}>
                {`${errorItem.message} (${formatDuration(now.getTime() - errorItem.timestamp)} ago)`}
                <Spacing size="small" />
                {errorDetails ? (
                    <Accordion>
                        <AccordionItem
                            label={<TitleSubsection>More details</TitleSubsection>}
                            elevated
                            whitespaceSize="none"
                            open={false}
                        >
                            {errorDetails}
                        </AccordionItem>
                    </Accordion>
                ) : null}
            </div>
        </Notification>
    );
};
