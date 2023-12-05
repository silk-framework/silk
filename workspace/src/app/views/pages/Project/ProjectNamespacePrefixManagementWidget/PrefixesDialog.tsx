import React, { useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { IPrefixDefinition } from "@ducks/workspace/typings";
import { workspaceOp, workspaceSel } from "@ducks/workspace";
import { Button, SimpleDialog } from "@eccenca/gui-elements";
import PrefixRow from "./PrefixRow";
import DeleteModal from "../../../shared/modals/DeleteModal";
import PrefixNew from "./PrefixNew";
import DataList from "../../../shared/Datalist";
import Loading from "../../../shared/Loading";
import { useTranslation } from "react-i18next";

interface IProps {
    projectId: string;
    onCloseModal: () => any;
    isOpen: boolean;
    existingPrefixes: Set<string>;
}

/** Manages project prefix definitions. */
const PrefixesDialog = ({ onCloseModal, isOpen, existingPrefixes, projectId }: IProps) => {
    const dispatch = useDispatch();
    const prefixList = useSelector(workspaceSel.prefixListSelector);
    const configWidget = useSelector(workspaceSel.widgetsSelector).configuration;
    const { isLoading } = configWidget;

    const [isOpenRemove, setIsOpenRemove] = useState<boolean>(false);
    const [selectedPrefix, setSelectedPrefix] = useState<IPrefixDefinition | undefined>(undefined);
    const widgetHasErrors = !!Object.keys(configWidget?.error ?? {}).length;

    console.log({ widgetHasErrors });

    const [t] = useTranslation();

    const toggleRemoveDialog = (prefix?: IPrefixDefinition) => {
        if (!prefix || isOpenRemove) {
            setIsOpenRemove(false);
            setSelectedPrefix(undefined);
        } else {
            setIsOpenRemove(true);
            setSelectedPrefix(prefix);
        }
    };

    const handleConfirmRemove = () => {
        if (selectedPrefix) {
            dispatch(workspaceOp.fetchRemoveProjectPrefixAsync(selectedPrefix.prefixName, projectId));
        }
        // if (!widgetHasErrors) {
        //     toggleRemoveDialog();
        // }
    };

    const handleAddOrUpdatePrefix = (prefix: IPrefixDefinition) => {
        const { prefixName, prefixUri } = prefix;
        dispatch(workspaceOp.fetchAddOrUpdatePrefixAsync(prefixName, prefixUri, projectId));
    };

    return (
        <SimpleDialog
            title={t("widget.ConfigWidget.prefixTitle", "Manage Prefixes")}
            data-test-id={"prefix-dialog"}
            isOpen={isOpen}
            onClose={onCloseModal}
            actions={
                <Button data-test-id={"close-prefix-dialog-btn"} onClick={() => onCloseModal()}>
                    {t("common.action.close")}
                </Button>
            }
        >
            {isLoading ? (
                <Loading
                    description={t("widget.ConfigWidget.loadingPrefix", "Loading prefix configuration.")}
                    delay={0}
                />
            ) : (
                <>
                    <PrefixNew
                        onAdd={(newPrefix: IPrefixDefinition) => handleAddOrUpdatePrefix(newPrefix)}
                        existingPrefixes={existingPrefixes}
                    />
                    <DataList isEmpty={!prefixList.length} isLoading={isLoading} hasSpacing hasDivider>
                        {prefixList.map((prefix, i) => (
                            <PrefixRow key={i} prefix={prefix} onRemove={() => toggleRemoveDialog(prefix)} />
                        ))}
                    </DataList>
                </>
            )}
            <DeleteModal
                isOpen={isOpenRemove}
                data-test-id={"update-prefix-dialog"}
                onDiscard={() => {}}
                onConfirm={handleConfirmRemove}
                title={t("common.action.DeleteSmth", { smth: t("widget.ConfigWidget.prefix") })}
                errorMessage={widgetHasErrors ? `Deletion failed: ${configWidget.error.asString()}` : undefined}
            >
                <p>{t("PrefixDialog.deletePrefix", { prefixName: selectedPrefix ? selectedPrefix.prefixName : "" })}</p>
            </DeleteModal>
        </SimpleDialog>
    );
};

export default PrefixesDialog;
