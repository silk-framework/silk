import React, { useEffect, useState } from "react";
import {
    Button,
    SimpleDialog,
    TextField,
} from '@wrappers/index';

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
        <SimpleDialog
            size="small"
            title="Cloning"
            isOpen={isOpen}
            onClose={onDiscard}
            actions={[
                <Button
                    key='clone'
                    affirmative
                    onClick={() => onConfirm(newId)}
                    disabled={!newId}
                >
                    Clone
                </Button>,
                <Button key='cancel' onClick={onDiscard}>Cancel</Button>
            ]}
        >
            <TextField
                onChange={e => setNewId(e.target.value)}
                value={newId}
            />
        </SimpleDialog>
    )
}
