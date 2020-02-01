import React, { useState } from 'react';

import { Classes, IconNames, Intent } from "@wrappers/constants";
import Dialog from "@wrappers/dialog";

import Button from '@wrappers/button';
import PrefixDialogRow from "./PrefixDialogRow";

const PrefixesDialog = ({ prefixList, onCloseModal }) => {
    const [removedPrefixes, setRemovedPrefixes] = useState<Set<string>>(new Set());
    const [addedPrefixes, setAddedPrefixes] = useState<Object>({});

    const handleRemove = key => {
        removedPrefixes.add(key);
        setRemovedPrefixes(removedPrefixes);
    };

    const handleAdd = (key: string, value: string) => {
        setAddedPrefixes({
            addedPrefixes,
            [key]: value
        });
    };

    const handleChange = (key: string, value: string) => {
        handleRemove(key);

    };

    const handleSave = () => {

    };

    return (
        <Dialog
            onClose={onCloseModal}
            title="Edit Prefixes"
            isOpen={true}
            style={{width: '800px'}}
        >
            <div className={Classes.DIALOG_BODY} style={{
                maxHeight: '600px',
                overflow: 'auto'
            }}>
                {
                    Object.keys(prefixList).map(key =>
                        <PrefixDialogRow
                            itemKey={key}
                            value={prefixList[key]}
                            onChange={value => handleChange(key, value)}
                            onRemove={() => handleRemove(key)}
                        />
                    )
                }
            </div>
            <div className={Classes.DIALOG_FOOTER}>
                <div className={Classes.DIALOG_FOOTER_ACTIONS}>
                    <Button onClick={onCloseModal}>Cancel</Button>
                    <Button onClick={handleSave}>Save</Button>
                    <Button intent={Intent.PRIMARY}>Add Prefix</Button>
                </div>
            </div>
        </Dialog>
    )
};

export default PrefixesDialog;
