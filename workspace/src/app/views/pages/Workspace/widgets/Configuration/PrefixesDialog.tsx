import React, { useEffect, useState } from 'react';
import { Classes, Intent } from "@wrappers/constants";
import Dialog from "@wrappers/dialog";

import Button from '@wrappers/button';
import PrefixRow from "./PrefixRow";
import DeleteModal from "../../../../components/modals/DeleteModal";
import PrefixNew from "./PrefixNew";

export interface IFormattedPrefix {
    prefixName: string;
    prefixUri: string;
}

const PrefixesDialog = ({prefixList, onCloseModal, onRemove, onAddOrUpdate}) => {
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
            onRemove(selectedPrefix.prefixName);

            const arr = [...formattedPrefixes];
            const i = arr.findIndex(item => item.prefixUri === selectedPrefix.prefixUri);
            arr.splice(i, 1);

            setFormattedPrefixes(arr);
        }

        setSelectedPrefix(null);
        setIsOpenRemove(false);
    };

    const handleAddNew = (prefix: IFormattedPrefix) => {
        onAddOrUpdate(prefix.prefixName, prefix.prefixUri)
    };

    return (
        <Dialog
            onClose={onCloseModal}
            title="Manage Prefixes"
            isOpen={true}
            style={{width: '850px'}}
        >
            <div className={Classes.DIALOG_BODY} style={{
                maxHeight: '600px',
                overflow: 'auto'
            }}>
                <div className="col-12">
                    <PrefixNew onAdd={handleAddNew}/>
                </div>
                <div className={'row'}>
                    <div className="col-5"><b>Prefix</b></div>
                    <div className="col-6"><b>Uri</b></div>
                </div>
                {
                    formattedPrefixes.map((prefix, i) =>
                        <PrefixRow
                            key={i}
                            prefix={prefix}
                            onRemove={() => handleRemove(prefix)}
                        />
                    )
                }
            </div>
            <div className={Classes.DIALOG_FOOTER}>
                <div className={Classes.DIALOG_FOOTER_ACTIONS}>
                    <Button
                        onClick={() => setIsOpenRemove(false)}
                        intent={Intent.NONE}
                    >Cancel</Button>
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
