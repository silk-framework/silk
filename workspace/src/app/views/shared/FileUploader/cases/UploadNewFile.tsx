import { DragDrop } from "@uppy/react";
import React, { useEffect, useState } from "react";
import Uppy from "@uppy/core";
import ProgressBar from "@wrappers/blueprint/progressbar";
import { Button } from "@wrappers/index";
import AbortAlert from "../../modals/FileUploadModal/AbortAlert";
import { Intent } from "@wrappers/blueprint/constants";

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
}

/**
 * The Widget for "Upload new file" option
 */
export function UploadNewFile(props: IProps) {
    const { uppy, simpleInput, allowMultiple, onAdded, onUploadSuccess } = props;

    const [progress, setProgress] = useState<number>(0);
    const [uploaded, setUploaded] = useState<File>(null);

    const [openAbortDialog, setOpenAbortDialog] = useState<boolean>(false);

    useEffect(() => {
        registerEvents();
        return unregisterEvents;
    }, []);

    const registerEvents = () => {
        uppy.on("file-added", onAdded);
        uppy.on("upload-progress", handleProgress);
        uppy.on("upload-success", handleUploadSuccess);
    };

    const unregisterEvents = () => {
        uppy.off("file-added", onAdded);
        uppy.off("upload-progress", handleProgress);
        uppy.off("upload-success", handleUploadSuccess);
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
        setProgress(0);
        setOpenAbortDialog(false);
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
        <div>
            {progress ? (
                <div>
                    <p>
                        {!uploaded
                            ? "Waiting for finished file upload to show data preview."
                            : `${uploaded.name} successfully uploaded`}
                    </p>
                    <ProgressBar
                        value={progress}
                        stripes={!uploaded}
                        intent={uploaded ? Intent.SUCCESS : Intent.PRIMARY}
                    />
                    {!uploaded && <Button onClick={() => setOpenAbortDialog(true)}>Abort Upload</Button>}
                </div>
            ) : simpleInput ? (
                <input type="file" id="fileInput" onChange={handleFileInputChange} />
            ) : (
                <DragDrop uppy={uppy} />
            )}
            <AbortAlert isOpen={openAbortDialog} onCancel={() => setOpenAbortDialog(false)} onConfirm={handleAbort} />
        </div>
    );
}
