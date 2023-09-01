import React, { ReactElement, useState } from "react";
import {
    AlertDialog,
    Button,
    Checkbox,
    FieldItem,
    HtmlContentBlock,
    Notification,
    Spacing,
} from "@eccenca/gui-elements";
import { Loading } from "../Loading/Loading";
import { useTranslation } from "react-i18next";
import { TestableComponent } from "@eccenca/gui-elements/src/components/interfaces";
import useHotKey from "../HotKeyHandler/HotKeyHandler";

export interface IDeleteModalOptions extends TestableComponent {
    isOpen: boolean;
    confirmationRequired?: boolean;

    onDiscard(): void;
    onConfirm(): void;

    render?(): ReactElement;
    children?: ReactElement;
    title?: string;
    // Loading status during the remove request
    removeLoading?: boolean;
    errorMessage?: string;
    /** Called when the Enter key is pressed and the optional confirmation is checked. */
    submitOnEnter?: boolean;
}

export default function DeleteModal({
    isOpen,
    confirmationRequired,
    onDiscard,
    render,
    onConfirm,
    children,
    title = "Delete",
    removeLoading = false,
    errorMessage,
    submitOnEnter = true,
    ...otherProps
}: IDeleteModalOptions) {
    const [isConfirmed, setIsConfirmed] = useState(false);
    const confirmCheckbox = React.useRef<HTMLInputElement | null>(null);

    const toggleConfirmChange = () => {
        setIsConfirmed(!isConfirmed);
        // Needs to be blurred after changing its state so the ENTER hotkey can be triggered
        confirmCheckbox.current?.blur();
    };

    // Only render content when modal is open
    const otherContent = !!render && isOpen ? render() : null;
    const [t] = useTranslation();
    const enterHandler = React.useCallback(() => {
        if (submitOnEnter && (!confirmationRequired || isConfirmed)) {
            onConfirm();
        }
    }, [submitOnEnter, isConfirmed, confirmationRequired, onConfirm]);

    useHotKey({ hotkey: "enter", handler: enterHandler, enabled: submitOnEnter });

    return (
        <AlertDialog
            danger
            title={title}
            isOpen={isOpen}
            canEscapeKeyClose={true}
            onClose={onDiscard}
            data-test-id={otherProps["data-test-id"]}
            actions={
                removeLoading ? (
                    <Loading delay={0} />
                ) : (
                    [
                        <Button
                            key="remove"
                            disruptive
                            onClick={onConfirm}
                            disabled={confirmationRequired && !isConfirmed}
                            data-test-id={"remove-item-button"}
                        >
                            {t("common.action.delete", "Delete")}
                        </Button>,
                        <Button key="cancel" onClick={onDiscard}>
                            {t("common.action.cancel", "Cancel")}
                        </Button>,
                    ]
                )
            }
        >
            {otherContent && (
                <>
                    <HtmlContentBlock>{otherContent}</HtmlContentBlock>
                    <Spacing />
                </>
            )}
            {children && (
                <>
                    {children}
                    <Spacing />
                </>
            )}
            {confirmationRequired && (
                <FieldItem>
                    <Checkbox
                        inputRef={confirmCheckbox}
                        onChange={toggleConfirmChange}
                        label={t("common.action.confirm", "Confirm")}
                    />
                </FieldItem>
            )}
            {errorMessage && (
                <>
                    <Spacing />
                    <Notification message={errorMessage} danger />
                    <Spacing />
                </>
            )}
        </AlertDialog>
    );
}
