import { DragDrop } from "@uppy/react";
import React, { useEffect, useState } from "react";
import Uppy, { UppyFile } from "@uppy/core";
import ProgressBar from "@gui-elements/blueprint/progressbar";
import { AlertDialog, Button, Notification, Spacing } from "@gui-elements/index";
import { Intent } from "@gui-elements/blueprint/constants";
import { useTranslation } from "react-i18next";
import i18next from "../../../../../language";

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

    /**
     * Run for every each file added
     * @param file
     */
    const handleFileAdded = async (file: UppyFile) => {
        setError(null);

        // find if file already checked
        const replacementFile = onlyReplacements.find((f) => f.id === file.id);
        if (replacementFile) {
            try {
                await upload([replacementFile]);
                // update without upload file
                setOnlyReplacements(onlyReplacements.filter((f) => f.id !== replacementFile.id));
            } catch (e) {
                console.log("Cant replace file", e);
            }
        } else {
            uppy.setFileState(file.id, {
                replacement: await validateBeforeAdd(file.name),
            });

            checkedFilesQueue++;

            // if all files added then run uploader
            const isCompleteAllChecks = checkedFilesQueue === uppy.getFiles().length;
            if (isCompleteAllChecks) {
                const files = uppy.getFiles();
                const replacements = files.filter((file: any) => file.replacement);

                replacements.forEach((f) => {
                    uppy.removeFile(f.id);
                });

                setOnlyReplacements(replacements);

                try {
                    await upload(uppy.getFiles());
                    checkedFilesQueue = 0;
                } catch (e) {
                    console.log("Cant upload files", e);
                }
            }

            if (onAdded) {
                onAdded(file);
            }
        }
    };

    const upload = async (files) => {
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

    const uppyFiles = uppy.getFiles();
    return (
        <>
            <DragDrop
                uppy={uppy}
                locale={{ strings: { dropHereOr: t("FileUploader.dropzone", "Drop file here or browse") } }}
            />
            <Spacing />
            {uppyFiles.map((file) => {
                const { progress } = file;
                const fileProgress = progress.bytesUploaded / progress.bytesTotal;
                const isUploaded = file.progress.uploadComplete;

                const fileActionBtn = isUploaded ? (
                    <Button outlined onClick={() => onRemoveFile(file.name)}>
                        Remove
                    </Button>
                ) : fileProgress !== 1 ? (
                    <Button outlined onClick={() => handleAbort(file.id)}>
                        {t("FileUploader.abortOnly", "Abort Upload")}
                    </Button>
                ) : null;

                return (
                    <div key={file.id}>
                        <Notification success={isUploaded} actions={fileActionBtn}>
                            <p>
                                {!isUploaded
                                    ? t("FileUploader.waitFor", "Wait for finished upload.")
                                    : t("FileUploader.successfullyUploaded", { uploadedName: file.name })}
                            </p>
                            <Spacing />
                            <ProgressBar
                                value={fileProgress}
                                stripes={!isUploaded}
                                intent={isUploaded ? Intent.SUCCESS : Intent.PRIMARY}
                            />
                        </Notification>
                        <Spacing />
                    </div>
                );
            })}
            {onlyReplacements.map((file) => {
                return (
                    <div key={file.id}>
                        <Notification
                            warning={true}
                            actions={
                                <Button outlined onClick={() => handleReplace(file)}>
                                    {t("common.action.replace", "Replace")}
                                </Button>
                            }
                        >
                            <p>{t("OverwriteModal.overwriteFile", { fileName: file.name })}</p>
                            <Spacing />
                        </Notification>
                        <Spacing />
                    </div>
                );
            })}
            {error && <Notification message={error} danger />}
        </>
    );
}
