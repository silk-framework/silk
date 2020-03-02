import React from 'react';
import { Intent } from "@wrappers/blueprint/constants";
import Alert from "@wrappers/blueprint/alert";

const AbortAlert = ({ isOpen, onCancel, onConfirm }) => {
    return <Alert
        cancelButtonText="Cancel"
        confirmButtonText="Abort"
        icon="trash"
        intent={Intent.DANGER}
        isOpen={isOpen}
        onCancel={onCancel}
        onConfirm={onConfirm}
    >
        <p>Are you sure you want to <b>abort</b> upload process?</p>
    </Alert>
};

export default AbortAlert;
