import React from "react";
import { AlertDialog, Button } from "@gui-elements/index";

interface IProps {
    fileName: string;
    onConfirm: (e) => void;
    onCancel: (e) => void;
    isOpen: boolean;
}

/** Alert to warn against overwriting an existing file */
const OverrideAlert = ({ fileName, isOpen, onCancel, onConfirm }: IProps) => {
    return (
        <AlertDialog
            warning
            isOpen={isOpen}
            actions={[
                <Button key="replace" onClick={onConfirm}>
                    Replace
                </Button>,
                <Button key="cancel" onClick={onCancel}>
                    Cancel
                </Button>,
            ]}
        >
            <p>File '{fileName}' already exists. Do you want to overwrite it?</p>
        </AlertDialog>
    );
};

export default OverrideAlert;
