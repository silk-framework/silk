import React, { ReactElement, useState } from "react";
import Label from "@wrappers/blueprint/label";
import {
    Button,
    Checkbox,
    AlertDialog,
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
        <AlertDialog
            danger
            title="Confirm Deletion"
            isOpen={isOpen}
            onClose={onDiscard}
            actions={
                [
                    <Button
                        disruptive
                        onClick={onConfirm}
                        disabled={confirmationRequired && !isConfirmed}
                    >
                        Remove
                    </Button>,
                    <Button onClick={onDiscard}>Cancel</Button>
                ]
            }
        >
            <div>{render && render()}</div>
            <div>{children && children}</div>
            <div>
                {
                    confirmationRequired && <Checkbox onChange={toggleConfirmChange} label={"Confirm"} />
                }
            </div>
        </AlertDialog>
    )
}
