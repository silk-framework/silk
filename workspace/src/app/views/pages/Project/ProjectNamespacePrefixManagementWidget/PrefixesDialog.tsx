import React, { useState } from "react";
import { batch, useDispatch, useSelector } from "react-redux";
import { IPrefixDefinition } from "@ducks/workspace/typings";
import { workspaceOp, workspaceSel } from "@ducks/workspace";
import { Button, Notification, SimpleDialog } from "@eccenca/gui-elements";
import PrefixRow from "./PrefixRow";
import DeleteModal from "../../../shared/modals/DeleteModal";
import PrefixNew from "./PrefixNew";
import DataList from "../../../shared/Datalist";
import { useTranslation } from "react-i18next";
import { updatePrefixList } from "@ducks/workspace/widgets/configuration.thunk";
import { requestChangePrefixes, requestRemoveProjectPrefix } from "@ducks/workspace/requests";
import { widgetsSlice } from "@ducks/workspace/widgetsSlice";
import { ErrorResponse } from "../../../../services/fetch/responseInterceptor";
import { useModalError } from "../../../../hooks/useModalError";

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
    const [loading, setLoading] = React.useState<boolean>(false);
    const [error, setError] = React.useState<ErrorResponse | undefined>();
    const checkAndDisplayPrefixError = useModalError({ setError });

    const [isOpenRemove, setIsOpenRemove] = useState<boolean>(false);
    const [selectedPrefix, setSelectedPrefix] = useState<IPrefixDefinition | undefined>(undefined);

    const [t] = useTranslation();

    const toggleRemoveDialog = (prefix?: IPrefixDefinition) => {
        if (!prefix || isOpenRemove) {
            setIsOpenRemove(false);
            setSelectedPrefix(undefined);
        } else {
            setIsOpenRemove(true);
            setSelectedPrefix(prefix);
        }
        setError(undefined);
    };

    React.useEffect(() => {
        setError(undefined);
    }, [isOpen]);

    const handleConfirmRemove = React.useCallback(async () => {
        try {
            setLoading(true);
            if (selectedPrefix) {
                setError(undefined);
                const data = await requestRemoveProjectPrefix(selectedPrefix.prefixName, projectId);
                dispatch(updatePrefixList(data));

                if (data) {
                    toggleRemoveDialog();
                }
            }
        } catch (err) {
            checkAndDisplayPrefixError(
                err,
                t("widget.ConfigWidget.modal.errors.prefixDeletionFailure", "Prefix deletion failed")
            );
        } finally {
            setLoading(false);
        }
    }, [projectId, selectedPrefix, error]);

    const handleAddOrUpdatePrefix = React.useCallback(async (prefix: IPrefixDefinition) => {
        try {
            setLoading(true);
            setError(undefined);
            const { prefixName, prefixUri } = prefix;
            const data = await requestChangePrefixes(prefixName, JSON.stringify(prefixUri), projectId);
            if (data) {
                batch(() => {
                    dispatch(widgetsSlice.actions.resetNewPrefix());
                    dispatch(updatePrefixList(data));
                });
            }
        } catch (err) {
            checkAndDisplayPrefixError(
                err,
                t("widget.ConfigWidget.modal.errors.prefixChangeFailure", "Prefix change failed")
            );
        } finally {
            setLoading(false);
        }
    }, []);

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
            notifications={error ? <Notification intent="danger">{error.detail}</Notification> : null}
        >
            <PrefixNew
                onAdd={(newPrefix: IPrefixDefinition) => handleAddOrUpdatePrefix(newPrefix)}
                existingPrefixes={existingPrefixes}
            />
            <DataList isEmpty={!prefixList.length} isLoading={loading} hasSpacing hasDivider>
                {prefixList.map((prefix, i) => (
                    <PrefixRow key={i} prefix={prefix} onRemove={() => toggleRemoveDialog(prefix)} />
                ))}
            </DataList>
            <DeleteModal
                isOpen={isOpenRemove}
                data-test-id={"update-prefix-dialog"}
                onDiscard={() => toggleRemoveDialog()}
                onConfirm={handleConfirmRemove}
                title={t("common.action.DeleteSmth", { smth: t("widget.ConfigWidget.prefix") })}
                errorMessage={error ? error.detail : undefined}
            >
                <p>{t("PrefixDialog.deletePrefix", { prefixName: selectedPrefix ? selectedPrefix.prefixName : "" })}</p>
            </DeleteModal>
        </SimpleDialog>
    );
};

export default PrefixesDialog;
