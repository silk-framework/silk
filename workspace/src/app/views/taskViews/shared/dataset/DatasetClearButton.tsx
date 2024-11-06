import React, { useState } from "react";

import { IconButton } from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";
import DeleteModal from "../../../shared/modals/DeleteModal";
import { clearDataset } from "./dataset.requests";
import { ErrorResponse, FetchError } from "../../../../services/fetch/responseInterceptor";
import { IMetaData } from "../../../pages/MappingEditor/HierarchicalMapping/store";
import { requestTaskData } from "@ducks/shared/requests";
import useErrorHandler from "../../../../hooks/useErrorHandler";

interface Props {
    projectId: string;
    datasetId: string;
}

/** Clears the given dataset. Displays a disabled button if the dataset is read-only.
 * Displays no button if the dataset does not exist. */
export const DatasetClearButton = ({ projectId, datasetId }: Props) => {
    const [t] = useTranslation();
    const [showModal, setShowModal] = React.useState(false);
    const [datasetMetaData, setDatasetMetaData] = React.useState<IMetaData | undefined>(undefined);
    const [outputIsReadOnly, setOutputIsReadOnly] = React.useState(false);
    const { registerError } = useErrorHandler();

    React.useEffect(() => {
        fetchDatasetMetaData();
    }, [projectId, datasetId]);

    const fetchDatasetMetaData = React.useCallback(async () => {
        setDatasetMetaData(undefined);
        setOutputIsReadOnly(false);
        try {
            const datasetTask = (await requestTaskData(projectId, datasetId)).data;
            if (datasetTask.data.taskType === "Dataset") {
                if (!datasetTask.data.readOnly) {
                    // Only allow deleting if there is an output dataset
                    setDatasetMetaData(datasetTask.metadata);
                } else {
                    setOutputIsReadOnly(true);
                }
            }
        } catch (error) {
            registerError("DatasetClearButton.fetchDatasetMetaData", "Could not fetch dataset data.", error);
        }
    }, [projectId, datasetId]);

    return (
        <>
            {datasetMetaData ? (
                <IconButton
                    key={"button"}
                    name={"item-remove"}
                    text={t("DatasetClearButton.tooltip")}
                    disruptive={true}
                    onClick={() => setShowModal(true)}
                />
            ) : null}
            {outputIsReadOnly ? (
                <IconButton
                    key={"button"}
                    name={"item-remove"}
                    disabled={true}
                    text={t("DatasetClearButton.readOnlyTooltip")}
                />
            ) : null}
            {datasetMetaData && showModal ? (
                <DatasetClearButtonModal
                    key={"modal"}
                    projectId={projectId}
                    datasetId={datasetId}
                    onClose={() => setShowModal(false)}
                    datasetMetaData={datasetMetaData}
                />
            ) : null}
        </>
    );
};

interface ModalProps extends Props {
    onClose: () => any;
    datasetMetaData: IMetaData;
}

const DatasetClearButtonModal = ({ projectId, datasetId, onClose, datasetMetaData }: ModalProps) => {
    const [t] = useTranslation();
    const [error, setError] = useState<ErrorResponse | undefined>(undefined);
    const [clearing, setClearing] = React.useState(false);

    const onConfirm = async () => {
        setError(undefined);
        setClearing(true);
        try {
            await clearDataset(projectId, datasetId);
            onClose();
        } catch (error) {
            if (error.isFetchError) {
                setError((error as FetchError).errorResponse);
            }
        } finally {
            setClearing(false);
        }
    };
    const datasetLabel = datasetMetaData.label;
    return (
        <DeleteModal
            title={t("DatasetClearButton.modalTitle", { datasetLabel })}
            isOpen={true}
            onConfirm={onConfirm}
            onDiscard={onClose}
            removeLoading={clearing}
            alternativeDeleteButtonText={t("DatasetClearButton.modalButtonText")}
            errorMessage={
                error ? `Clearing dataset '${datasetMetaData.label}' has failed. ${error.asString()}` : undefined
            }
        >
            {t("DatasetClearButton.modalText", { datasetLabel })}
        </DeleteModal>
    );
};
