import React, { useState } from 'react';
import { Classes, Intent } from "@wrappers/constants";
import Dialog from "@wrappers/dialog";

import Button from '@wrappers/button';
import PrefixRow from "./PrefixRow";
import DeleteModal from "../../../../components/modals/DeleteModal";
import PrefixNew from "./PrefixNew";
import { IPrefixState } from "@ducks/workspace/typings";
import { workspaceOp, workspaceSel } from "@ducks/workspace";
import { useDispatch, useSelector } from "react-redux";

const PrefixesDialog = ({onCloseModal, isOpen}) => {
    const dispatch = useDispatch();

    const prefixList = useSelector(workspaceSel.prefixListSelector);
    const newPrefix = useSelector(workspaceSel.newPrefixSelector);

    const [isOpenRemove, setIsOpenRemove] = useState<boolean>(false);
    const [selectedPrefix, setSelectedPrefix] = useState<IPrefixState>(null);

    const toggleRemoveDialog = (prefix?: IPrefixState) => {
        if (!prefix || isOpenRemove) {
            setIsOpenRemove(false);
            setSelectedPrefix(null);
        } else {
            setIsOpenRemove(true);
            setSelectedPrefix(prefix);
        }
    };

    const handleConfirmRemove = () => {
        if (selectedPrefix) {
            dispatch(workspaceOp.removeProjectPrefixAsync(selectedPrefix.prefixName));
        }
        toggleRemoveDialog();
    };

    const handleAddOrUpdatePrefix = (prefix: IPrefixState) => {
        const {prefixName, prefixUri} = prefix;
        dispatch(workspaceOp.addOrUpdatePrefixAsync(prefixName, prefixUri));
    };

    const handleUpdatePrefixFields = (field: string, value: string) => {
        dispatch(workspaceOp.updateNewPrefix({
            field,
            value
        }));
    };

    return (
        <Dialog
            onClose={onCloseModal}
            title="Manage Prefixes"
            isOpen={isOpen}
            style={{width: '850px'}}
        >
            <div className={Classes.DIALOG_BODY} style={{
                maxHeight: '600px',
                overflow: 'auto'
            }}>
                <div className="col-12">
                    <PrefixNew
                        prefix={newPrefix}
                        onChangePrefix={handleUpdatePrefixFields}
                        onAdd={() => handleAddOrUpdatePrefix(newPrefix)}
                    />
                </div>
                <div className={'row'}>
                    <div className="col-5"><b>Prefix</b></div>
                    <div className="col-6"><b>Uri</b></div>
                </div>
                {
                    prefixList.map((prefix, i) =>
                        <PrefixRow
                            key={i}
                            prefix={prefix}
                            onRemove={() => toggleRemoveDialog(prefix)}
                        />
                    )
                }
            </div>
            <div className={Classes.DIALOG_FOOTER}>
                <div className={Classes.DIALOG_FOOTER_ACTIONS}>
                    <Button
                        onClick={() => onCloseModal()}
                        intent={Intent.NONE}>
                        Close
                    </Button>
                </div>
            </div>
            <DeleteModal
                isOpen={isOpenRemove}
                onDiscard={() => toggleRemoveDialog()}
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
