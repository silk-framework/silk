import React from "react";
import { AlertDialog, Button } from "@gui-elements/index";

const AbortAlert = ({ isOpen, onCancel, onConfirm }) => {
    return (
        <AlertDialog
            danger
            isOpen={isOpen}
            actions={[
                <Button key="abort" onClick={onConfirm}>
                    Abort
                </Button>,
                <Button key="cancel" onClick={onCancel}>
                    Cancel
                </Button>,
            ]}
        >
            <p>Abort upload process?</p>
        </AlertDialog>
    );
};

export default AbortAlert;
