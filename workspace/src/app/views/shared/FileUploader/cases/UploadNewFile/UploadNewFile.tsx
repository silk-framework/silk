import { DragDrop } from "@uppy/react";
import React, { useEffect, useState } from "react";
import Uppy, { UppyFile } from "@uppy/core";
import { Button, Notification, Spacing } from "@gui-elements/index";
import { useTranslation } from "react-i18next";
import { NewFileItem } from "./NewFileItem";
import { ReplacementFileItem } from "./ReplacementFileItem";
import { useForceUpdate } from "../../../../../hooks/useForceUpdate";
import { RetryFileItem } from "./RetryFileItem";
import { CompletedFileItem } from "./CompletedFileItem";
import { FileRemoveModal } from "../../../modals/FileRemoveModal";

interface IProps {
    // Uppy instance
    uppy: Uppy.Uppy<any>;

    // Allow multiple file upload
    allowMultiple?: boolean;

    uploadEndpoint: string;

    projectId: string;

    validateBeforeAdd(fileName: string);

    onAdded?(file: UppyFile);

    onProgress?(progress: number);

    onUploadSuccess?(file: UppyFile);

    onUploadError?(e, f);
}

let checkedFilesQueue = 0;

/**
 * The Widget for "Upload new file" option
 */
export function UploadNewFile(props: IProps) {
    const { projectId, uppy, onAdded, onUploadSuccess, validateBeforeAdd, uploadEndpoint } = props;

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

    // register uppy events
    useEffect(() => {
        const unregisterEvents = () => {
            checkedFilesQueue = 0;

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

    const uploadReplacementFile = async (replacementFile: UppyFile) => {
        try {
            setOnlyReplacements((prevState) => prevState.filter((f) => f.id !== replacementFile.id));
            await upload([replacementFile]);
            // catch is implemented in handleUploadError
        } finally {
        }
    };

    const uploadNewFile = async (file: UppyFile, forceUpload: boolean = false) => {
        try {
            const replacement = await validateBeforeAdd(file.name);
            if (replacement) {
                removeFromQueue(file.id);
                setOnlyReplacements((prevState) => [...prevState, file]);
            }
        } catch (e) {
            // when file is corrupted or something wrong with the file
            if (e.isHttpError && e.httpStatus === 400) {
                setFileError(file.id, e.errorDetails.response?.data);
            }

            // when network offline
            if (e.isNetworkError) {
                setFileError(file.id, t("http.error.networkFileUpload", { fileName: file.name }));

                updateRetryFiles(file);

                return;
            }
        }

        // if all files added then run uploader
        checkedFilesQueue++;

        const notCompletedUploads = uppy.getFiles().filter((f) => !f.progress.uploadComplete);
        const isCompleteAllChecks = checkedFilesQueue >= notCompletedUploads.length;

        if (isCompleteAllChecks || forceUpload) {
            try {
                await upload(notCompletedUploads);
            } catch (e) {}
        }

        if (onAdded) {
            onAdded(file);
        }
    };

    /**
     * Run for every each file added
     * @param file
     */
    const handleFileAdded = async (file: UppyFile) => {
        setError(null);

        // find if file already checked
        const replacementFile = onlyReplacements.find((f) => f.id === file.id);
        if (replacementFile) {
            await uploadReplacementFile(replacementFile);
        } else {
            await uploadNewFile(file);
        }
    };

    const upload = async (files): Promise<void | never> => {
        try {
            files.forEach((file) => {
                uppy.setFileState(file.id, {
                    xhrUpload: {
                        endpoint: `${uploadEndpoint}/${encodeURIComponent(file.name)}`,
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
        setProgresses({
            ...progresses,
            [file.id]: progressAmount,
        });
    };

    const handleUploadSuccess = (file: UppyFile) => {
        setError(null);
        setUploadedFiles([...uploadedFiles, file]);

        removeFromQueue(file.id);

        onUploadSuccess(file);
    };

    const handleUploadError = (fileData, error) => {
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

        updateRetryFiles(fileData);
    };

    const updateRetryFiles = (file: UppyFile) => {
        setFilesForRetry((files) => [...files, uppy.getFile(file.id)]);

        removeFromQueue(file.id);
    };

    const handleAbort = (fileId: string) => {
        setFilesForRetry([...filesForRetry, uppy.getFile(fileId)]);
        removeFromQueue(fileId);
    };

    const handleReplace = (file: UppyFile) => {
        uppy.addFile(file);
    };

    const handleCancelReplace = (fileId: string) => {
        setOnlyReplacements(onlyReplacements.filter((f) => f.id !== fileId));
    };

    const handleRetry = (fileId: string) => {
        const files = filesForRetry.filter((f) => f.id === fileId);
        deleteFromRetry(fileId);

        files.forEach(uppy.addFile);
    };

    const handleRetryAll = () => {
        const files = uppy.getFiles();

        // reset uppy if all files should retry
        uppy.reset();

        files.forEach(uppy.addFile);
    };

    const deleteFromRetry = (fileId: string) => {
        setFilesForRetry(filesForRetry.filter((f) => f.id !== fileId));
    };

    const handleConfirmDelete = (fileId: string) => {
        if (fileId) {
            setUploadedFiles(uploadedFiles.filter((f) => f.id !== fileId));
        }

        setShowDeleteDialog(null);
    };

    const removeFromQueue = (fileId: string) => {
        uppy.removeFile(fileId);
        forceUpdate();
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
            <FileRemoveModal projectId={projectId} onConfirm={handleConfirmDelete} file={showDeleteDialog} />
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
                            onCancelRetry={deleteFromRetry}
                        />
                    ))}
                    {uppy.getFiles().map((file) => (
                        <NewFileItem
                            key={file.id}
                            file={file}
                            onAbort={handleAbort}
                            onRemove={removeFromQueue}
                            progress={progresses[file.id] || 0}
                        />
                    ))}
                    {onlyReplacements.map((file) => (
                        <ReplacementFileItem
                            key={file.id}
                            file={file}
                            onCancelReplacement={handleCancelReplace}
                            onReplace={handleReplace}
                        />
                    ))}
                    {uploadedFiles.map((file) => (
                        <CompletedFileItem key={file.id} file={file} onRemoveFile={setShowDeleteDialog} />
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
