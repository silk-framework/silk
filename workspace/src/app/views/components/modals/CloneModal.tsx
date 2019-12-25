import React, { useEffect, useState } from "react";
import { Classes, Intent } from "@wrappers/constants";
import Button from "@wrappers/button";
import Dialog from "@wrappers/dialog";
import InputGroup from "@wrappers/input-group";

export interface ICloneOptions {
    isOpen: boolean;
    oldId: string;

    onDiscard(): void;
    onConfirm(newId: string): void;
}

export default function CloneModal({isOpen, oldId, onDiscard, onConfirm}: ICloneOptions) {
    const [newId, setNewId] = useState(oldId);

    useEffect(() => {
        setNewId(oldId);
    }, [oldId]);

    return (
        <Dialog
            icon="info-sign"
            onClose={onDiscard}
            title="Cloning"
            isOpen={isOpen}
        >
            <div className={Classes.DIALOG_BODY}>
                <InputGroup
                    onChange={e => setNewId(e.target.value)}
                    value={newId}
                />
            </div>

            <div className={Classes.DIALOG_FOOTER}>
                <div className={Classes.DIALOG_FOOTER_ACTIONS}>
                    <Button
                        intent={Intent.PRIMARY}
                        onClick={() => onConfirm(newId)}
                        disabled={!newId}
                    >
                        Clone
                    </Button>
                    <Button onClick={onDiscard}>Cancel</Button>
                </div>
            </div>

        </Dialog>
    )
}
