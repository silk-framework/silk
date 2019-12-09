import React, { memo } from "react";
import { Button, Classes, Dialog as B_Dialog, Intent } from '@blueprintjs/core';

interface IProps {
    isOpen: boolean;
    onClose(bool?: boolean): void;
}

const Dialog = memo(({ isOpen, onClose }: IProps) => {
    const handleClose = (confirm: boolean = false) => {
        onClose(confirm);
    };

    return (
        <B_Dialog
            icon="info-sign"
            onClose={() => handleClose()}
            title="Confirm Deletion"
            isOpen={isOpen}
        >
            <div className={Classes.DIALOG_BODY}>
                <p>
                    Are you sure you want to permanently remove this item?
                </p>
            </div>
            <div className={Classes.DIALOG_FOOTER}>
                <div className={Classes.DIALOG_FOOTER_ACTIONS}>
                    <Button intent={Intent.PRIMARY} onClick={() => handleClose(true)}>
                        Remove
                    </Button>
                    <Button onClick={() => handleClose()}>
                        Cancel
                    </Button>
                </div>
            </div>
        </B_Dialog>
    )
});

export default Dialog;
