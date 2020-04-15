import React from 'react';
import {
    AlertDialog,
    Button,
} from "@wrappers/index";

const OverrideAlert = ({ isOpen, onCancel, onConfirm }) => {
    return <AlertDialog
        warning
        isOpen={isOpen}
        actions={
            [
                <Button key='replace' onClick={onConfirm}>Replace</Button>,
                <Button key='cancel' onClick={onCancel}>Cancel</Button>
            ]
        }
    >
        <p>File already exists. Are you sure you want to replace it?</p>
    </AlertDialog>
};

export default OverrideAlert;
