import React from "react";
import { Classes, Intent } from "@wrappers/bluprint/constants";
import Button from "@wrappers/bluprint/button";
import Dialog from "@wrappers/bluprint/dialog";

export interface IFileUploadModalProps {
    isOpen: boolean;

    onDiscard(): void;

    onConfirm(): void;
}

export default function FileUploadModal({isOpen, onDiscard, onConfirm}: IFileUploadModalProps) {
    return (
        <Dialog
            icon="info-sign"
            onClose={onDiscard}
            title="Confirm Deletion"
            isOpen={isOpen}
        >

            <div className={Classes.DIALOG_BODY}>
            </div>
            <div className={Classes.DIALOG_FOOTER}>
                <div className={Classes.DIALOG_FOOTER_ACTIONS}>
                    <Button
                        intent={Intent.PRIMARY}
                        onClick={onConfirm}
                    >
                        Confirm
                    </Button>
                    <Button onClick={onDiscard}>Cancel</Button>
                </div>
            </div>

        </Dialog>
    )
}
