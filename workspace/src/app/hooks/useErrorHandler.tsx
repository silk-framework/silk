import React from "react";
import { DIErrorFormat, diErrorMessage, DIErrorTypes } from "@ducks/error/typings";
import { Accordion, AccordionItem, Notification, Spacing, TitleSubsection } from "@eccenca/gui-elements";
import { useDispatch, useSelector } from "react-redux";
import errorSelector from "@ducks/error/selectors";
import { registerNewError, clearOneOrMoreErrors } from "@ducks/error/errorSlice";
import { ErrorResponse, FetchError } from "../services/fetch/responseInterceptor";
import { useTranslation } from "react-i18next";

/**
 * @param errorId      An application wide unique error ID. This will be uniquely represented in the error widget.
 * @param errorMessage A human-readable error message that should be shown in the UI.
 * @param cause        The actual error that has happened.
 * @param errorNotificationInstanceId The notification instance where the error should be displayed. If this is set, the
 *                                    error will NOT be displayed in the global notification widget.
 */
export type ErrorHandlerRegisterFuncType = (
    errorId: string,
    errorMessage: string,
    cause: DIErrorTypes | null,
    options?: ErrorHandlerOptions,
) => React.JSX.Element | null;

interface ErrorHandlerOptions {
    /** The notification instance where the error should be displayed. If this is set, the
     error will NOT be displayed in the global notification widget. */
    errorNotificationInstanceId?: string;
    /** Optional function that is called when a notification will be dismissed. Usually needed when an error notification instance ID is supplied
     * and the return notification element is used. */
    onDismiss?: () => any;
    /** The intent of the notification. Default: "danger" */
    intent?: "danger" | "warning";
    /** If this is true, the error will not trigger opening the notification queue. */
    notAutoOpen?: boolean;
}

type ErrorHandlerRegisterShortFuncType = (
    /** A valid language key from the language files. This key will be used as error ID. */
    langKey: string,
    /** The error cause. */
    cause: DIErrorTypes | null,
    options?: ErrorHandlerOptions,
) => React.JSX.Element | null;

interface ErrorHandlerDict {
    registerError: ErrorHandlerRegisterFuncType;
    getAllErrors: () => Array<DIErrorFormat>;
    clearErrors: (errorIds?: Array<string> | undefined) => void;
    registerErrorI18N: ErrorHandlerRegisterShortFuncType;
}

export type RegisterErrorType = Pick<DIErrorFormat, "id" | "message" | "cause">;

const isTemporarilyUnavailableError = (error?: DIErrorTypes | null): boolean => {
    return (
        !!error &&
        (((error as FetchError).isFetchError && (error as FetchError).httpStatus === 503) ||
            (error as ErrorResponse).status === 503)
    );
};

const canBeIgnored = (error?: DIErrorTypes | null): boolean => {
    return !!error && (error as FetchError).isFetchError && (error as FetchError).errorResponse.canBeIgnored;
};

const isNotFoundError = (error?: DIErrorTypes | null): boolean => {
    return (
        !!error &&
        (((error as FetchError).isFetchError && (error as FetchError).httpStatus === 404) ||
            (error as ErrorResponse).status === 404)
    );
};

/** Hook for registering errors in the centralized error handling component. */
const useErrorHandler = (): ErrorHandlerDict => {
    const error = useSelector(errorSelector);
    const dispatch = useDispatch();
    const [t] = useTranslation();
    const currentErrors = error?.errors ?? [];
    const dispatchRef = React.useRef(dispatch);
    const errorRef = React.useRef(currentErrors);
    const translateRef = React.useRef(t);

    dispatchRef.current = dispatch;
    errorRef.current = currentErrors;
    translateRef.current = t;

    /** register a new error to the error stack
     *
     * @param errorId A globally unique error ID that has to be set by the developer. Convention: "<ComponentName>_<ActionName>", e.g. "WorkflowEditor_LoadWorkflow".
     * @param errorMessage Human readable error message.
     * @param cause Optional Error object explaining the exception.
     * @param options Additional options.
     */
    const registerError = React.useCallback<ErrorHandlerRegisterFuncType>(
        (errorId: string, errorMessage: string, cause: DIErrorTypes | null, options?: ErrorHandlerOptions) => {
            const { errorNotificationInstanceId, onDismiss, intent, notAutoOpen } = options ?? {};
            const error: RegisterErrorType = {
                id: errorId,
                message: errorMessage,
                cause,
            };
            if (canBeIgnored(cause)) {
                // Just ignore this error
                return null;
            } else if (isTemporarilyUnavailableError(cause)) {
                // Handle 503 errors differently
                const tempUnavailableMessage = translateRef.current("common.messages.temporarilyUnavailableMessage", {
                    detailMessage: diErrorMessage(cause),
                });
                dispatchRef.current(
                    registerNewError({
                        newError: {
                            id: "temporarily-unavailable",
                            message: tempUnavailableMessage,
                            cause: null,
                            alternativeIntent: "warning",
                        },
                        errorNotificationInstanceId,
                        notAutoOpen,
                    }),
                );
                return <Notification message={tempUnavailableMessage} />;
            } else if (isNotFoundError(cause)) {
                // Do not log 404 errors at all. These are usually due to page 404. Only log to console.
                console.warn("Received 404 error.", cause);
                return null;
            } else {
                dispatchRef.current(
                    registerNewError({
                        newError: {
                            ...error,
                            alternativeIntent: intent === "warning" ? intent : undefined,
                        },
                        errorNotificationInstanceId,
                        notAutoOpen,
                    }),
                );
                const detailMessage = diErrorMessage(cause);
                return (
                    <Notification intent="warning" onDismiss={onDismiss}>
                        {errorMessage}
                        <Spacing size="small" />
                        {detailMessage ? (
                            <Accordion>
                                <AccordionItem
                                    label={
                                        <TitleSubsection>
                                            {translateRef.current("common.action.showMoreDetails")}
                                        </TitleSubsection>
                                    }
                                    open={false}
                                    whitespaceSize={"none"}
                                    noBorder={true}
                                >
                                    {detailMessage}
                                </AccordionItem>
                            </Accordion>
                        ) : null}
                    </Notification>
                );
            }
        },
        [],
    );

    /** Shorter version that uses the language file key as error ID. */
    const registerErrorI18N = React.useCallback<ErrorHandlerRegisterShortFuncType>(
        (langKey: string, cause: DIErrorTypes | null, options?: ErrorHandlerOptions) => {
            return registerError(langKey, translateRef.current(langKey), cause, options);
        },
        [registerError],
    );

    // get a list of all errors
    const getAllErrors = React.useCallback(() => {
        return errorRef.current;
    }, []);

    /***
     * deletes all errors corresponding to the ids passed in the parameter,
     * if no parameter is passed it clears all errors.
     *  ***/
    const clearErrors = React.useCallback((errorIds?: Array<string> | undefined) => {
        dispatchRef.current(clearOneOrMoreErrors({ errorIds }));
    }, []);

    return React.useMemo(
        () => ({
            registerError,
            getAllErrors,
            clearErrors,
            registerErrorI18N,
        }),
        [clearErrors, getAllErrors, registerError, registerErrorI18N],
    );
};

export default useErrorHandler;
