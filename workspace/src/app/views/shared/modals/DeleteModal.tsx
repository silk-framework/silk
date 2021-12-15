import React, { ReactElement, useState } from "react";
import { AlertDialog, Button, Checkbox, FieldItem, HtmlContentBlock, Notification, Spacing } from "@gui-elements/index";
import { Loading } from "../Loading/Loading";
import { useTranslation } from "react-i18next";
import { TestableComponent } from "@gui-elements/src/components/interfaces";

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
    ...otherProps
}: IDeleteModalOptions) {
    const [isConfirmed, setIsConfirmed] = useState(false);

    const toggleConfirmChange = () => {
        setIsConfirmed(!isConfirmed);
    };

    // Only render content when modal is open
    const otherContent = !!render && isOpen ? render() : null;
    const [t] = useTranslation();

    return (
        <AlertDialog
            danger
            title={title}
            isOpen={isOpen}
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
                    <Checkbox onChange={toggleConfirmChange} label={t("common.action.confirm", "Confirm")} />
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
