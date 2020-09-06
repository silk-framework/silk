import { DragDrop } from "@uppy/react";
import React, { useEffect, useState } from "react";
import Uppy, { UppyFile } from "@uppy/core";
import ProgressBar from "@gui-elements/blueprint/progressbar";
import { AlertDialog, Button, Notification, Spacing } from "@gui-elements/index";
import { Intent } from "@gui-elements/blueprint/constants";
import { useTranslation } from "react-i18next";
import i18next from "../../../../../../language";
import { NewFileItem } from "./NewFileItem";
import { ReplacementFileItem } from "./ReplacementFileItem";

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

    const [onlyReplacements, setOnlyReplacements] = useState([]);
    const [error, setError] = useState(null);
    const [t] = useTranslation();

    useEffect(() => {
        registerEvents();
        return unregisterEvents;
    }, [onlyReplacements]);

    const registerEvents = () => {
        uppy.on("file-added", handleFileAdded);
        uppy.on("upload-progress", handleProgress);
        uppy.on("upload-success", handleUploadSuccess);
        uppy.on("upload-error", handleUploadError);
    };

    const unregisterEvents = () => {
        uppy.off("file-added", handleFileAdded);
        uppy.off("upload-progress", handleProgress);
        uppy.off("upload-success", handleUploadSuccess);
        uppy.off("upload-error", handleUploadError);
    };

    const uploadReplacementFile = async (replacementFile) => {
        // then upload and remove from replacements array
        try {
            await upload([replacementFile]);
            setOnlyReplacements(onlyReplacements.filter((f) => f.id !== replacementFile.id));
        } finally {
        }
    };

    const uploadNewFile = async (file: UppyFile, forceUpload: boolean = false) => {
        try {
            const replacement = await validateBeforeAdd(file.name);
            uppy.setFileState(file.id, {
                replacement,
            });
            checkedFilesQueue++;

            // if all files added then run uploader
            const isCompleteAllChecks = checkedFilesQueue === uppy.getFiles().length;
            if (isCompleteAllChecks || forceUpload) {
                const files = uppy.getFiles();
                const replacements = files.filter((file: any) => file.replacement);

                replacements.forEach((f) => {
                    uppy.removeFile(f.id);
                });

                setOnlyReplacements(replacements);

                try {
                    await upload(uppy.getFiles());
                    checkedFilesQueue = 0;
                } finally {
                }
            }

            if (onAdded) {
                onAdded(file);
            }
        } catch (e) {
            // when some error happened, e.g. connection issue
            setError(e.errorResponse?.detail);
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
        setOnlyReplacements([...onlyReplacements.filter((f) => f.id !== fileId)]);
    };

    // @TODO: broken functionality
    // const handleRetry = async () => {
    //     setError(null);
    //
    //     const files = uppy.getFiles();
    //
    //     files.map(async (file) =>  {
    //         // find if file already checked
    //         const replacementFile = onlyReplacements.find((f) => f.id === file.id);
    //         if (replacementFile) {
    //             uploadReplacementFile(replacementFile);
    //         } else {
    //             uploadNewFile(file, true);
    //         }
    //     });
    // };

    return (
        <>
            <DragDrop
                uppy={uppy}
                locale={{ strings: { dropHereOr: t("FileUploader.dropzone", "Drop files here or browse") } }}
            />
            <Spacing />
            {uppy.getFiles().map((file) => (
                <NewFileItem
                    key={file.id}
                    error={error}
                    file={file}
                    onRemoveFile={onRemoveFile}
                    onAbort={handleAbort}
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
            {error && <Notification message={error} danger />}
        </>
    );
}
