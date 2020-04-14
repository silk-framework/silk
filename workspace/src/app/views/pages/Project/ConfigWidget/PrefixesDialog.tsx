import React, { useState } from 'react';
import { Classes } from "@wrappers/blueprint/constants";
import Dialog from "@wrappers/blueprint/dialog";

import PrefixRow from "./PrefixRow";
import DeleteModal from "../../../shared/modals/DeleteModal";
import PrefixNew from "./PrefixNew";
import { IPrefixState } from "@ducks/workspace/typings";
import { workspaceOp, workspaceSel } from "@ducks/workspace";
import { useDispatch, useSelector } from "react-redux";
import DataList from "../../../shared/Datalist";
import Loading from "../../../shared/Loading";
import {
    Button,
    Spacing,
} from '@wrappers/index';

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
        <Dialog
            onClose={onCloseModal}
            title="Manage Prefixes"
            isOpen={isOpen}
            style={{width: '850px'}}
        >
            {
                isLoading ? <Loading /> :
                    <>
                        <div className={Classes.DIALOG_BODY} style={{
                            maxHeight: '600px',
                            overflow: 'auto'
                        }}>
                            <PrefixNew
                                prefix={newPrefix}
                                onChangePrefix={handleUpdatePrefixFields}
                                onAdd={() => handleAddOrUpdatePrefix(newPrefix)}
                            />
                            <Spacing small />
                            <DataList data={prefixList} densityHigh hasSpacing hasDivider>
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
                        </div>
                        <div className={Classes.DIALOG_FOOTER}>
                            <div className={Classes.DIALOG_FOOTER_ACTIONS}>
                                <Button onClick={() => onCloseModal()}>
                                    Close
                                </Button>
                            </div>
                        </div>
                    </>
            }
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
