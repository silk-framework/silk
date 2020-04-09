import React from 'react';
import { Intent } from "@wrappers/blueprint/constants";
import Alert from "@wrappers/blueprint/alert";

const OverrideAlert = ({ isOpen, onCancel, onConfirm }) => {
    return <Alert
        cancelButtonText="Cancel"
        confirmButtonText="Replace"
        icon="issue"
        intent={Intent.WARNING}
        isOpen={isOpen}
        onCancel={onCancel}
        onConfirm={onConfirm}
    >
        <p>File already exists. Are you sure you want to <b>replace</b> it?</p>
    </Alert>
};

export default OverrideAlert;
