import React from "react";
import { AlertDialog, Button } from "@gui-elements/index";
import { UppyFile } from "@uppy/core";

interface IProps {
    files: UppyFile[];
    onConfirm: (e) => void;
    onCancel: (e) => void;
    isOpen: boolean;
}

/**
 * @deprecated
 * can be removed
 */
/** Alert to warn against overwriting an existing file */
const OverrideAlert = ({ files, isOpen, onCancel, onConfirm }: IProps) => {
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
            <p>File '{files.map((f) => f.name)}' already exists. Do you want to overwrite it?</p>
        </AlertDialog>
    );
};

export default OverrideAlert;
