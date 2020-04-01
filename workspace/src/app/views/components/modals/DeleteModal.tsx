import React, { ReactElement, useState } from "react";
import { Classes } from "@wrappers/blueprint/constants";
import Dialog from "@wrappers/blueprint/dialog";
import Label from "@wrappers/blueprint/label";
import {
    Button,
    Checkbox,
} from '@wrappers/index';

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
                        confirmationRequired && <Checkbox onChange={toggleConfirmChange} label={"Confirm"} />
                    }
                    <Button onClick={onDiscard}>Cancel</Button>
                    <Button
                        disruptive
                        onClick={onConfirm}
                        disabled={confirmationRequired && !isConfirmed}
                    >
                        Remove
                    </Button>
                </div>
            </div>

        </Dialog>
    )
}
