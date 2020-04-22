import React, { useState } from 'react';
import { useDispatch, useSelector } from "react-redux";
import { IPrefixState } from "@ducks/workspace/typings";
import { workspaceOp, workspaceSel } from "@ducks/workspace";
import { Button, SimpleDialog, Spacing, } from '@wrappers/index';
import PrefixRow from "./PrefixRow";
import DeleteModal from "../../../shared/modals/DeleteModal";
import PrefixNew from "./PrefixNew";
import DataList from "../../../shared/Datalist";
import Loading from "../../../shared/Loading";

const PrefixesDialog = ({onCloseModal, isOpen}) => {
    const dispatch = useDispatch();

    const prefixList = useSelector(workspaceSel.prefixListSelector);
    const newPrefix = useSelector(workspaceSel.newPrefixSelector);
    const configWidget = useSelector(workspaceSel.widgetsSelector).configuration;
    const {error, isLoading} = configWidget;

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
            dispatch(workspaceOp.fetchRemoveProjectPrefixAsync(selectedPrefix.prefixName));
        }
        toggleRemoveDialog();
    };

    const handleAddOrUpdatePrefix = (prefix: IPrefixState) => {
        const {prefixName, prefixUri} = prefix;
        dispatch(workspaceOp.fetchAddOrUpdatePrefixAsync(prefixName, prefixUri));
    };

    const handleUpdatePrefixFields = (field: string, value: string) => {
        dispatch(workspaceOp.updateNewPrefix({
            field,
            value
        }));
    };

    return (
        <SimpleDialog
            title="Manage Prefixes"
            isOpen={isOpen}
            onClose={onCloseModal}
            actions={
                <Button onClick={() => onCloseModal()}>
                    Close
                </Button>
            }
        >
            {
                isLoading ?
                    <Loading /> :
                    <>
                        <PrefixNew
                            prefix={newPrefix}
                            onChangePrefix={handleUpdatePrefixFields}
                            onAdd={() => handleAddOrUpdatePrefix(newPrefix)}
                        />
                        <Spacing small />
                        <DataList
                            isEmpty={!prefixList.length}
                            isLoading={isLoading}
                            hasSpacing
                            hasDivider
                        >
                            {
                                prefixList.map((prefix, i) =>
                                    <PrefixRow
                                        key={i}
                                        prefix={prefix}
                                        onRemove={() => toggleRemoveDialog(prefix)}
                                    />
                                )
                            }
                        </DataList>
                    </>
            }
            <DeleteModal
                isOpen={isOpenRemove}
                onDiscard={() => toggleRemoveDialog()}
                onConfirm={handleConfirmRemove}
            >
                <p>Are you sure you want to delete prefix?</p>
            </DeleteModal>
        </SimpleDialog>
    )
};

export default PrefixesDialog;
