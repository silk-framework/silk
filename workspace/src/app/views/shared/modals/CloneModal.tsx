import React, { useEffect, useState } from "react";
import { Classes } from "@wrappers/blueprint/constants";
import Dialog from "@wrappers/blueprint/dialog";
import InputGroup from "@wrappers/blueprint/input-group";
import { Button } from '@wrappers/index';

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
                    <Button onClick={onDiscard}>Cancel</Button>
                    <Button
                        affirmative
                        onClick={() => onConfirm(newId)}
                        disabled={!newId}
                    >
                        Clone
                    </Button>
                </div>
            </div>

        </Dialog>
    )
}
