import React, { ReactElement, useState } from "react";
import { Classes, Intent } from "@wrappers/bluprint/constants";
import Button from "@wrappers/bluprint/button";
import Dialog from "@wrappers/bluprint/dialog";
import Label from "@wrappers/bluprint/label";
import Checkbox from "@wrappers/bluprint/checkbox";

export interface IDeleteModalOptions {
    isOpen: boolean;
    confirmationRequired?: boolean;

    onDiscard(): void;
    onConfirm(): void;

    render?(): ReactElement;
    children?: ReactElement;
}

export default function DeleteModal({isOpen, confirmationRequired, onDiscard, render, onConfirm, children}: IDeleteModalOptions) {
    const [isConfirmed, setIsConfirmed] = useState(false);

    const toggleConfirmChange = () => {
        setIsConfirmed(!isConfirmed);
    };

    return (
        <Dialog
            icon="info-sign"
            onClose={onDiscard}
            title="Confirm Deletion"
            isOpen={isOpen}
        >

            <div className={Classes.DIALOG_BODY}>
                {render && render()}
                {children && children}
            </div>

            <div className={Classes.DIALOG_FOOTER}>
                <div className={Classes.DIALOG_FOOTER_ACTIONS}>
                    {
                        confirmationRequired && (
                            <>
                                <Label>Confirm</Label>
                                <Checkbox onChange={toggleConfirmChange}/>
                            </>
                        )
                    }
                    <Button
                        intent={Intent.PRIMARY}
                        onClick={onConfirm}
                        disabled={confirmationRequired && !isConfirmed}
                    >
                        Remove
                    </Button>
                    <Button onClick={onDiscard}>Cancel</Button>
                </div>
            </div>

        </Dialog>
    )
}
