import React, { useState } from "react";
import { batch, useDispatch, useSelector } from "react-redux";
import { IPrefixDefinition } from "@ducks/workspace/typings";
import { workspaceOp, workspaceSel } from "@ducks/workspace";
import { Button, Notification, SimpleDialog } from "@eccenca/gui-elements";
import PrefixRow from "./PrefixRow";
import DeleteModal from "../../../shared/modals/DeleteModal";
import PrefixNew from "./PrefixNew";
import DataList from "../../../shared/Datalist";
import Loading from "../../../shared/Loading";
import { useTranslation } from "react-i18next";
import { setError, updatePrefixList } from "@ducks/workspace/widgets/configuration.thunk";
import { requestChangePrefixes, requestRemoveProjectPrefix } from "@ducks/workspace/requests";
import { widgetsSlice } from "@ducks/workspace/widgetsSlice";

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
    const [loading, setLoading] = React.useState<boolean>(false);

    const [isOpenRemove, setIsOpenRemove] = useState<boolean>(false);
    const [selectedPrefix, setSelectedPrefix] = useState<IPrefixDefinition | undefined>(undefined);
    const widgetError = useSelector(workspaceSel.widgetErrorSelector);
    const widgetHasErrors = !!Object.keys(widgetError ?? {}).length;

    const [t] = useTranslation();

    const toggleRemoveDialog = (prefix?: IPrefixDefinition) => {
        if (!prefix || isOpenRemove) {
            setIsOpenRemove(false);
            setSelectedPrefix(undefined);
        } else {
            dispatch(setError(undefined));
            setIsOpenRemove(true);
            setSelectedPrefix(prefix);
        }
    };

    const handleConfirmRemove = React.useCallback(async () => {
        try {
            setLoading(true);
            if (selectedPrefix) {
                const data = await requestRemoveProjectPrefix(selectedPrefix.prefixName, projectId);
                dispatch(updatePrefixList(data));
            }

            if (!widgetHasErrors) {
                toggleRemoveDialog();
            }
        } catch (err) {
            dispatch(setError(err));
        } finally {
            setLoading(false);
        }
    }, [projectId, selectedPrefix, widgetError]);

    const handleAddOrUpdatePrefix = React.useCallback(async (prefix: IPrefixDefinition) => {
        try {
            setLoading(true);
            const { prefixName, prefixUri } = prefix;
            const data = await requestChangePrefixes(prefixName, JSON.stringify(prefixUri), projectId);

            batch(() => {
                dispatch(widgetsSlice.actions.resetNewPrefix());
                dispatch(updatePrefixList(data));
            });
        } catch (err) {
            dispatch(setError(err));
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
            notifications={widgetHasErrors ? <Notification danger>{widgetError.asString()}</Notification> : null}
        >
            {loading ? (
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
                    <DataList isEmpty={!prefixList.length} isLoading={loading} hasSpacing hasDivider>
                        {prefixList.map((prefix, i) => (
                            <PrefixRow key={i} prefix={prefix} onRemove={() => toggleRemoveDialog(prefix)} />
                        ))}
                    </DataList>
                </>
            )}
            <DeleteModal
                isOpen={isOpenRemove}
                data-test-id={"update-prefix-dialog"}
                onDiscard={() => toggleRemoveDialog()}
                onConfirm={handleConfirmRemove}
                title={t("common.action.DeleteSmth", { smth: t("widget.ConfigWidget.prefix") })}
                errorMessage={widgetHasErrors ? `Deletion failed: ${widgetError.asString()}` : undefined}
            >
                <p>{t("PrefixDialog.deletePrefix", { prefixName: selectedPrefix ? selectedPrefix.prefixName : "" })}</p>
            </DeleteModal>
        </SimpleDialog>
    );
};

export default PrefixesDialog;
