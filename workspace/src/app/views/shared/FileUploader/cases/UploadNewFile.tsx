import { DragDrop } from "@uppy/react";
import React, { useEffect, useState } from "react";
import Uppy from "@uppy/core";
import ProgressBar from "@wrappers/blueprint/progressbar";
import { Button, Notification, Spacing } from "@wrappers/index";
import { Intent } from "@wrappers/blueprint/constants";
import { useTranslation } from "react-i18next";

interface IProps {
    // Uppy instance
    uppy: Uppy.Uppy<any>;

    // Recognize component view
    simpleInput?: boolean;

    // Allow multiple file upload
    allowMultiple?: boolean;

    onAdded(file: File);

    onProgress?(progress: number);

    onUploadSuccess?(file: File);

    onUploadError?(e, f);
}

/**
 * The Widget for "Upload new file" option
 */
export function UploadNewFile(props: IProps) {
    const { uppy, simpleInput, allowMultiple, onAdded, onUploadSuccess, onUploadError } = props;

    const [progress, setProgress] = useState<number>(-1);
    const [uploaded, setUploaded] = useState<File>(null);
    const [t] = useTranslation();

    useEffect(() => {
        registerEvents();
        return unregisterEvents;
    }, []);

    const registerEvents = () => {
        uppy.on("file-added", onAdded);
        uppy.on("upload-progress", handleProgress);
        uppy.on("upload-success", handleUploadSuccess);
        uppy.on("upload-error", onUploadError);
    };

    const unregisterEvents = () => {
        uppy.off("file-added", onAdded);
        uppy.off("upload-progress", handleProgress);
        uppy.off("upload-success", handleUploadSuccess);
        uppy.off("upload-error", onUploadError);
    };

    const handleProgress = (file, { bytesUploaded, bytesTotal }) => {
        const progress = bytesUploaded / bytesTotal;
        setProgress(progress);

        if (props.onProgress) {
            props.onProgress(progress);
        }
    };

    const handleUploadSuccess = (file: File) => {
        setUploaded(file);
        onUploadSuccess(file);
    };

    const handleAbort = () => {
        uppy.reset();
        uppy.cancelAll();
        setProgress(-1);
        setUploaded(null);
    };

    const handleFileInputChange = (event) => {
        const files = Array.from(event.target.files);
        files.forEach((file: File) => {
            try {
                uppy.addFile({
                    source: "file input",
                    name: file.name,
                    type: file.type,
                    data: file,
                });
            } catch (err) {
                if (err.isRestriction) {
                    // handle restrictions
                    console.log("Restriction error:", err);
                } else {
                    // handle other errors
                    console.error(err);
                }
            }
        });
    };

    if (!allowMultiple) {
        // Workaround because 'allowMultipleFiles' property on DragDrop does not work
        uppy.setOptions({ allowMultipleUploads: false, restrictions: { maxNumberOfFiles: 1 } });
    }

    return (
        <>
            {progress >= 0 ? (
                <Notification
                    success={uploaded ? true : false}
                    actions={
                        !uploaded && (
                            <Button outlined onClick={handleAbort}>
                                {t("common.fileUploader.abortOnly", "Abort Upload")}
                            </Button>
                        )
                    }
                >
                    <p>
                        {!uploaded
                            ? t("common.fileUploader.waitFor", "Wait for finished upload.")
                            : `${uploaded.name} was successfully uploaded`}
                    </p>
                    <Spacing />
                    <ProgressBar
                        value={progress}
                        stripes={!uploaded}
                        intent={uploaded ? Intent.SUCCESS : Intent.PRIMARY}
                    />
                </Notification>
            ) : simpleInput ? (
                <input type="file" id="fileInput" onChange={handleFileInputChange} />
            ) : (
                <DragDrop
                    uppy={uppy}
                    locale={{ strings: { dropHereOr: "Drop file here or %{browse}", browse: "browse" } }}
                />
            )}
        </>
    );
}
