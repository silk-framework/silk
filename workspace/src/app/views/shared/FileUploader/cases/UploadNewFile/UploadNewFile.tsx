import { DragDrop } from "@uppy/react";
import React, { useEffect, useState } from "react";
import Uppy, { UppyFile } from "@uppy/core";
import { Button, Notification, Spacing } from "@gui-elements/index";
import { useTranslation } from "react-i18next";
import { NewFileItem } from "./NewFileItem";
import { ReplacementFileItem } from "./ReplacementFileItem";
import { useForceUpdate } from "../../../../../hooks/useForceUpdate";
import { RetryFileItem } from "./RetryFileItem";
import { UploadedFileItem } from "./UploadedFileItem";
import { FileRemoveModal } from "../../../modals/FileRemoveModal";

interface IProps {
    // Uppy instance
    uppy: Uppy.Uppy<any>;

    // Allow multiple file upload
    allowMultiple?: boolean;

    uploadEndpoint: string;

    projectId?: string;

    // If true, the file name will be added to the uploadEndpoint for each file to be uploaded
    attachFileNameToEndpoint: boolean;

    validateBeforeAdd?(fileName: string);

    onAdded?(file: UppyFile);

    onProgress?(progress: number);

    onUploadSuccess?(file: UppyFile, response: { status: number; body: any });

    onUploadError?(e, f);
}

/**
 * The Widget for "Upload new file" option
 */
export function UploadNewFile(props: IProps) {
    const {
        projectId,
        uppy,
        onAdded,
        onUploadSuccess,
        validateBeforeAdd,
        uploadEndpoint,
        attachFileNameToEndpoint,
    } = props;

    // contains files, which need in replacements
    const [onlyReplacements, setOnlyReplacements] = useState<UppyFile[]>([]);

    // contains files, which need in retry action
    const [filesForRetry, setFilesForRetry] = useState<UppyFile[]>([]);

    // contains already uploaded files
    const [uploadedFiles, setUploadedFiles] = useState<UppyFile[]>([]);

    // contains fatal error messages, if it's exists then show the Retry button
    // @FIXME: perhaps we don't need it
    const [error, setError] = useState(null);

    // contains file for delete dialog
    const [showDeleteDialog, setShowDeleteDialog] = useState<UppyFile>(null);

    // there we put the file ids with progress statuses
    const [progresses, setProgresses] = useState({});

    const [t] = useTranslation();

    const forceUpdate = useForceUpdate();

    // register/unregister uppy events
    useEffect(() => {
        const unregisterEvents = () => {
            uppy.off("file-added", handleFileAdded);
            uppy.off("upload-progress", handleProgress);
            uppy.off("upload-success", handleUploadSuccess);
            uppy.off("upload-error", handleUploadError);
        };

        // reset events, because of "file-added" store prev state values
        unregisterEvents();

        uppy.on("file-added", handleFileAdded);
        uppy.on("upload-progress", handleProgress);
        uppy.on("upload-success", handleUploadSuccess);
        uppy.on("upload-error", handleUploadError);

        return unregisterEvents;
    }, [onlyReplacements, uploadedFiles, filesForRetry]);

    /**
     * Run for every each file added
     * @param file
     */
    const handleFileAdded = async (file: UppyFile) => {
        setError(null);
        // find if file already checked
        const inReplacements = onlyReplacements.find((f) => f.id === file.id);
        if (file.source === "REPLACE_ACTION") {
            await uploadReplacementFile(file);
        } else if (inReplacements) {
            // file added in Uppy queue, so we should remove it
            removeFromUppyQueue(file.id);
        } else {
            await uploadAsNewFile(file);
        }
    };

    const validateBeforeUploadAsync = async (file: UppyFile) => {
        try {
            const replacement = validateBeforeAdd ? await validateBeforeAdd(file.name) : false;
            if (replacement) {
                // also remove from completed uploads
                removeFromUploaded(file.id);

                // in case when retry action fired and aborted before checking response received, that will duplicate the files
                const inRetryList = filesForRetry.findIndex((f) => f.id === file.id);
                if (inRetryList === -1) {
                    addInReplacementQueue(file);
                }
            }
        } catch (e) {
            // when file is corrupted or something wrong with the file
            if (e.isHttpError && e.httpStatus === 400) {
                setFileError(file.id, e.errorDetails.response?.data);
            }

            // when network offline
            if (e.isNetworkError || e.httpStatus >= 500) {
                setFileError(file.id, t("http.error.networkFileUpload", { fileName: file.name }));

                addInRetryQueue(file);

                return;
            }
        }
    };

    const uploadReplacementFile = async (replacementFile: UppyFile) => {
        try {
            removeFromReplacement(replacementFile.id);
            await upload([replacementFile]);
            // catch is implemented in handleUploadError
        } finally {
        }
    };

    const uploadAsNewFile = async (file: UppyFile) => {
        await validateBeforeUploadAsync(file);

        const notCompletedUploads = uppy.getFiles().filter((f) => !f.progress.uploadComplete);

        try {
            if (onAdded) {
                onAdded(file);
            }
            await upload(notCompletedUploads);
            // catch is implemented in handleUploadError
        } finally {
        }
    };

    const upload = async (files): Promise<void | never> => {
        try {
            files.forEach((file) => {
                uppy.setFileState(file.id, {
                    xhrUpload: {
                        endpoint: attachFileNameToEndpoint
                            ? `${uploadEndpoint}/${encodeURIComponent(file.name)}`
                            : uploadEndpoint,
                    },
                });
            });

            await uppy.upload();
        } catch (e) {
            throw new Error(e);
        }
    };

    const handleProgress = (file: UppyFile, { bytesUploaded, bytesTotal }) => {
        const progressAmount = bytesUploaded / bytesTotal;

        if (props.onProgress) {
            props.onProgress(progressAmount);
        }
        setProgresses((prevState) => ({
            ...prevState,
            [file.id]: progressAmount,
        }));
    };

    const handleUploadSuccess = (file: UppyFile, response: { status: number; body: any }) => {
        setError(null);

        setUploadedFiles((prevState) => [...prevState, file]);

        removeFromUppyQueue(file.id);

        onUploadSuccess(file, response);
    };

    const handleUploadError = (fileData: UppyFile, error: any) => {
        let errorDetails = error?.message ? error.message : "-";
        const idx = errorDetails.indexOf("Source error");
        if (idx > 0) {
            errorDetails = errorDetails.substring(0, idx);
        }

        try {
            errorDetails += ` - ${JSON.parse(error.request.response).detail}`;
        } catch {}

        const errorMessage = t("FileUploader.uploadError", {
            fileName: fileData.name,
            errorDetails: errorDetails,
        });

        setFileError(fileData.id, errorMessage);

        // when project not found
        if (error.request.status !== 404) {
            addInRetryQueue(fileData);
        }
    };

    const addInRetryQueue = (file: UppyFile) => {
        setFilesForRetry((prevState) => [...prevState, file]);
        removeFromUppyQueue(file.id);
    };

    const addInReplacementQueue = (file: UppyFile) => {
        setOnlyReplacements((prevState) => [...prevState, file]);
        removeFromUppyQueue(file.id);
    };

    const handleAbort = (fileId: string) => {
        addInRetryQueue(uppy.getFile(fileId));
    };

    const handleReplace = (file: UppyFile) => {
        uppy.addFile({
            ...file,
            source: "REPLACE_ACTION",
        });
    };

    const handleRetry = (fileId: string) => {
        const files = filesForRetry.filter((f) => f.id === fileId);

        removeFromRetry(fileId);

        files.forEach(uppy.addFile);
    };

    const handleConfirmDelete = (fileId: string) => {
        removeFromUploaded(fileId);
        setShowDeleteDialog(null);
    };

    const handleRetryAll = () => {
        const files = uppy.getFiles();

        // reset uppy if all files should retry
        uppy.reset();

        files.forEach(uppy.addFile);
    };

    const removeFromUppyQueue = (fileId: string) => {
        uppy.removeFile(fileId);

        // call force on uppy change
        forceUpdate();
    };

    const removeFromUploaded = (fileId: string) => {
        const isUploaded = uploadedFiles.find((f) => f.id === fileId);
        if (fileId && isUploaded) {
            setUploadedFiles((prevState) => prevState.filter((f) => f.id !== fileId));
        }
    };

    const removeFromRetry = (fileId: string) => {
        setFilesForRetry((prevState) => prevState.filter((f) => f.id !== fileId));
    };

    const removeFromReplacement = (fileId: string) => {
        if (fileId) {
            setOnlyReplacements((prevState) => prevState.filter((f) => f.id !== fileId));
        }
    };

    const setFileError = (fileId: string, error: string) => {
        uppy.setFileState(fileId, {
            error,
        });
        // after every file state update forceUpdate required
        forceUpdate();
    };

    return (
        <>
            {projectId && (
                <FileRemoveModal projectId={projectId} onConfirm={handleConfirmDelete} file={showDeleteDialog} />
            )}
            <DragDrop
                uppy={uppy}
                locale={{ strings: { dropHereOr: t("FileUploader.dropzone", "Drop files here or browse") } }}
            />
            <Spacing />
            {!error ? (
                <>
                    {filesForRetry.map((file) => (
                        <RetryFileItem
                            key={file.id}
                            file={file}
                            onRetry={handleRetry}
                            onCancelRetry={removeFromRetry}
                        />
                    ))}
                    {uppy.getFiles().map((file) => (
                        <NewFileItem
                            key={file.id}
                            file={file}
                            onAbort={handleAbort}
                            onRemove={removeFromUppyQueue}
                            progress={progresses[file.id] || 0}
                        />
                    ))}
                    {onlyReplacements.map((file) => (
                        <ReplacementFileItem
                            key={file.id}
                            file={file}
                            onCancelReplacement={removeFromReplacement}
                            onReplace={handleReplace}
                        />
                    ))}
                    {uploadedFiles.map((file) => (
                        <UploadedFileItem key={file.id} file={file} onRemoveFile={setShowDeleteDialog} />
                    ))}
                </>
            ) : (
                <Notification
                    actions={
                        <Button minimal outlined text={t("FileUploader.retry", "Retry")} onClick={handleRetryAll} />
                    }
                    message={error}
                    danger
                />
            )}
        </>
    );
}
