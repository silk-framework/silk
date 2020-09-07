import { DragDrop } from "@uppy/react";
import React, { useEffect, useState } from "react";
import Uppy, { UppyFile } from "@uppy/core";
import { Button, Notification, Spacing } from "@gui-elements/index";
import { useTranslation } from "react-i18next";
import i18next from "../../../../../../language";
import { NewFileItem } from "./NewFileItem";
import { ReplacementFileItem } from "./ReplacementFileItem";
import { useForceUpdate } from "../../../../../hooks/useForceUpdate";

interface IProps {
    // Uppy instance
    uppy: Uppy.Uppy<any>;

    // Allow multiple file upload
    allowMultiple?: boolean;

    uploadEndpoint: string;

    validateBeforeAdd(fileName: string);

    onAdded?(file: UppyFile);

    onProgress?(progress: number);

    onUploadSuccess?(file: UppyFile);

    onRemoveFile(fileId: string);

    onUploadError?(e, f);
}

let checkedFilesQueue = 0;

/**
 * The Widget for "Upload new file" option
 */
export function UploadNewFile(props: IProps) {
    const { uppy, onRemoveFile, onAdded, onUploadSuccess, validateBeforeAdd, uploadEndpoint } = props;
    // contains files, which need in replacements
    const [onlyReplacements, setOnlyReplacements] = useState([]);

    // contains fatal error messages, if it's exists then show the Retry button
    const [error, setError] = useState(null);

    // forceUpdate need when uppy updated
    const forceUpdate = useForceUpdate();

    const [t] = useTranslation();

    // register uppy events
    useEffect(() => {
        const unregisterEvents = () => {
            uppy.off("file-added", handleFileAdded);
            uppy.off("upload-progress", handleProgress);
            uppy.off("upload-success", handleUploadSuccess);
            uppy.off("upload-error", handleUploadError);

            checkedFilesQueue = 0;
        };

        // reset events, because of "file-added" store old values of onlyReplacements
        unregisterEvents();

        uppy.on("file-added", handleFileAdded);
        uppy.on("upload-progress", handleProgress);
        uppy.on("upload-success", handleUploadSuccess);
        uppy.on("upload-error", handleUploadError);

        return unregisterEvents;
    }, [onlyReplacements]);

    const uploadReplacementFile = async (replacementFile: UppyFile) => {
        try {
            await upload([replacementFile]);
            setOnlyReplacements((prevState) => prevState.filter((f) => f.id !== replacementFile.id));
            // catch is implemented in handleUploadError
        } finally {
        }
    };

    const uploadNewFile = async (file: UppyFile, forceUpload: boolean = false) => {
        try {
            const replacement = await validateBeforeAdd(file.name);
            if (replacement) {
                uppy.removeFile(file.id);
                setOnlyReplacements((prevState) => [...prevState, file]);
            }
        } catch (e) {
            // when some error happened, e.g. connection issue
            setError(e.errorResponse?.detail);
            return;
        }

        // if all files added then run uploader
        checkedFilesQueue++;

        const notCompletedUploads = uppy.getFiles().filter((f) => !f.progress.uploadComplete);
        const isCompleteAllChecks = checkedFilesQueue >= notCompletedUploads.length;

        if (isCompleteAllChecks || forceUpload) {
            try {
                await upload(notCompletedUploads);
            } catch (e) {}
            // await upload([file]);
            // checkedFilesQueue = 0;
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
    };

    const handleUploadSuccess = (file: UppyFile) => {
        setError(null);
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

        const errorMessage = i18next.t("FileUploader.uploadError", {
            fileName: fileData.name,
            errorDetails: errorDetails,
        });
        setError(errorMessage);
    };

    const handleAbort = (id: string) => {
        uppy.removeFile(id);
    };

    const handleReplace = (file) => {
        uppy.addFile(file);
    };

    const handleCancelReplace = (fileId: string) => {
        setOnlyReplacements(onlyReplacements.filter((f) => f.id !== fileId));
    };

    const handleRetry = async () => {
        const files = uppy.getFiles();

        uppy.reset();

        files.map(handleReplace);
    };

    const handleDismissUploadedFile = (fileId: string) => {
        uppy.removeFile(fileId);
        forceUpdate(fileId);
    };

    return (
        <>
            <DragDrop
                uppy={uppy}
                locale={{ strings: { dropHereOr: t("FileUploader.dropzone", "Drop files here or browse") } }}
            />
            <Spacing />
            {!error ? (
                <>
                    {uppy.getFiles().map((file) => (
                        <NewFileItem
                            key={file.id}
                            error={error}
                            file={file}
                            onRemoveFile={onRemoveFile}
                            onAbort={handleAbort}
                            onDismiss={handleDismissUploadedFile}
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
                </>
            ) : (
                <Notification
                    actions={<Button minimal outlined text={t("FileUploader.retry", "Retry")} onClick={handleRetry} />}
                    message={error}
                    danger
                />
            )}
        </>
    );
}
