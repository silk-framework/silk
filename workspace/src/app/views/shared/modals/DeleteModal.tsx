import React, { ReactElement, useState } from "react";
import { AlertDialog, Button, Checkbox, HtmlContentBlock, FieldItem, Spacing } from "@wrappers/index";

export interface IDeleteModalOptions {
    isOpen: boolean;
    confirmationRequired?: boolean;

    onDiscard(): void;
    onConfirm(): void;

    render?(): ReactElement;
    children?: ReactElement;
}

export default function DeleteModal({
    isOpen,
    confirmationRequired,
    onDiscard,
    render,
    onConfirm,
    children,
}: IDeleteModalOptions) {
    const [isConfirmed, setIsConfirmed] = useState(false);

    const toggleConfirmChange = () => {
        setIsConfirmed(!isConfirmed);
    };

    const otherContent = !!render ? render() : null;

    return (
        <AlertDialog
            danger
            title="Confirm Deletion"
            isOpen={isOpen}
            onClose={onDiscard}
            actions={[
                <Button key="remove" disruptive onClick={onConfirm} disabled={confirmationRequired && !isConfirmed}>
                    Remove
                </Button>,
                <Button key="cancel" onClick={onDiscard}>
                    Cancel
                </Button>,
            ]}
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
                    <Checkbox onChange={toggleConfirmChange} label={"Confirm"} />
                </FieldItem>
            )}
        </AlertDialog>
    );
}
