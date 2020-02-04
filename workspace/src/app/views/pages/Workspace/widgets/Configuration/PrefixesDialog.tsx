import React, { useEffect, useState } from 'react';
import { Classes, Intent } from "@wrappers/constants";
import Dialog from "@wrappers/dialog";

import Button from '@wrappers/button';
import PrefixDialogRow from "./PrefixDialogRow";
import DeleteModal from "../../../../components/modals/DeleteModal";

export interface IFormattedPrefix {
    prefixName: string;
    prefixUri: string;
    _isNew?: boolean;
}

const PrefixesDialog = ({prefixList, onCloseModal, onRemovePrefix}) => {
    const [newPrefix, setNewPrefix] = useState<IFormattedPrefix>(null);

    const [formattedPrefixes, setFormattedPrefixes] = useState<IFormattedPrefix[]>([]);
    const [isOpenRemove, setIsOpenRemove] = useState<boolean>(false);
    const [selectedPrefix, setSelectedPrefix] = useState<IFormattedPrefix>(null);

    useEffect(() => {
        const prefixesArr = Object.keys(prefixList)
            .map(key => ({
                prefixName: key,
                prefixUri: prefixList[key]
            }));

        setFormattedPrefixes(prefixesArr);
    }, [prefixList]);

    const handleRemove = (prefix: IFormattedPrefix) => {
        setIsOpenRemove(true);
        setSelectedPrefix(prefix);
    };

    const handleConfirmRemove = () => {
        if (selectedPrefix) {
            onRemovePrefix(selectedPrefix.prefixName);

            const arr = [...formattedPrefixes];
            const i = arr.findIndex(item => item.prefixUri === selectedPrefix.prefixUri);
            arr.splice(i, 1);

            setFormattedPrefixes(arr);
        }

        setNewPrefix(null);
        setSelectedPrefix(null);
        setIsOpenRemove(false);
    };

    const handleChange = (index: number, field: string, value: string) => {
        const arr = [...formattedPrefixes];
        // This is new prefix
        if (index < 0) {
            arr.unshift(newPrefix);
            index = 0;
        }

        arr[index] = {
            ...arr[index],
            [field]: value
        };

        setFormattedPrefixes(arr);
        setNewPrefix(null);
    };

    const handleAdd = () => {
        setNewPrefix({
            prefixUri: '',
            prefixName: '',
            _isNew: true
        });
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
                    newPrefix && <PrefixDialogRow
                        key={'NEW_PREFIX'}
                        prefix={newPrefix}
                        onChange={(field, value) => handleChange(-1, field, value)}
                        onRemove={() => handleConfirmRemove()}
                    />
                }
                {
                    formattedPrefixes.map((prefix, i) =>
                        <PrefixDialogRow
                            key={i}
                            prefix={prefix}
                            onChange={(field, value) => handleChange(i, field, value)}
                            onRemove={() => handleRemove(prefix)}
                        />
                    )
                }
            </div>
            <div className={Classes.DIALOG_FOOTER}>
                <div className={Classes.DIALOG_FOOTER_ACTIONS}>
                    <Button
                        onClick={handleAdd}
                        intent={Intent.PRIMARY}
                        disabled={newPrefix}
                    >Add Prefix</Button>
                </div>
            </div>
            <DeleteModal
                isOpen={isOpenRemove}
                onDiscard={() => setIsOpenRemove(false)}
                onConfirm={handleConfirmRemove}
            >
                <div>
                    <p>Are you sure you want to delete prefix?</p>
                </div>
            </DeleteModal>
        </Dialog>
    )
};

export default PrefixesDialog;
