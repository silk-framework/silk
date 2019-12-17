import React, { ReactElement, useState } from "react";
import { Button, Classes, Intent } from '@blueprintjs/core';
import Dialog from "../wrappers/dialog/Dialog";
import Checkbox from "../wrappers/checkbox";
import Label from "../wrappers/label/Label";

export interface IDeleteModalOptions {
    isOpen: boolean;
    confirmationRequired?: boolean;

    onDiscard(): void;
    onConfirm(): void;

    render?(): ReactElement;
}

export default function DeleteModal({isOpen, confirmationRequired, onDiscard, render, onConfirm}: IDeleteModalOptions) {
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
                    <Button onClick={() => onDiscard()}>Cancel</Button>
                </div>
            </div>

        </Dialog>
    )
}
