import { DragDrop } from "@uppy/react";
import React, { useEffect, useState } from "react";
import Uppy, { UppyFile } from "@uppy/core";
import ProgressBar from "@gui-elements/blueprint/progressbar";
import { Button, Notification, Spacing } from "@gui-elements/index";
import { Intent } from "@gui-elements/blueprint/constants";

interface IProps {
    // Uppy instance
    uppy: Uppy.Uppy<any>;

    // Recognize component view
    simpleInput?: boolean;

    // Allow multiple file upload
    allowMultiple?: boolean;

    onAdded(file: UppyFile);

    onProgress?(progress: number);

    onUploadSuccess?(file: File);

    onUploadError?(e, f);
}

/**
 * The Widget for "Upload new file" option
 */
export function UploadNewFile(props: IProps) {
    const { uppy, simpleInput, allowMultiple, onAdded, onUploadSuccess, onUploadError } = props;

    useEffect(() => {
        registerEvents();
        return unregisterEvents;
    }, []);

    const registerEvents = () => {
        console.log("register events");
        uppy.on("file-added", handleDropAreaAdded);
        uppy.on("upload-progress", handleProgress);
        uppy.on("upload-success", handleUploadSuccess);
        uppy.on("upload-error", onUploadError);
    };

    const unregisterEvents = () => {
        console.log("unregister events");
        uppy.off("file-added", handleDropAreaAdded);
        uppy.off("upload-progress", handleProgress);
        uppy.off("upload-success", handleUploadSuccess);
        uppy.off("upload-error", onUploadError);
    };

    /**
     * Run for every each file added
     * @param file
     */
    const handleDropAreaAdded = (file: UppyFile) => {
        onAdded(file);
    };

    const handleProgress = (file, { bytesUploaded, bytesTotal }) => {
        const progressAmount = bytesUploaded / bytesTotal;
        if (props.onProgress) {
            props.onProgress(progressAmount);
        }
    };

    const handleUploadSuccess = (file) => {
        console.log("Uploaded successfully", file);
        onUploadSuccess(file);
    };

    const handleAbort = (id: string) => {
        uppy.removeFile(id);
    };

    // Only for input file
    // @TODO Missing multiple uploader functionality
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

    // Workaround because 'allowMultipleFiles' property on DragDrop does not work
    uppy.setOptions({ allowMultipleUploads: allowMultiple, restrictions: { maxNumberOfFiles: 3 } });
    const uppyFiles = uppy.getFiles();

    return (
        <>
            {uppyFiles.length ? (
                uppyFiles.map((file) => {
                    const { progress } = file;
                    const fileProgress = progress.bytesUploaded / progress.bytesTotal;
                    const isUploaded = file.progress.uploadComplete;

                    return (
                        <Notification
                            key={file.id}
                            success={isUploaded}
                            actions={
                                !isUploaded &&
                                fileProgress !== 1 && (
                                    <Button outlined onClick={() => handleAbort(file.id)}>
                                        Abort Upload
                                    </Button>
                                )
                            }
                        >
                            <p>
                                {!isUploaded ? "Wait for finished upload." : `${file.name} was successfully uploaded`}
                            </p>
                            <Spacing />
                            <ProgressBar
                                value={fileProgress}
                                stripes={!isUploaded}
                                intent={isUploaded ? Intent.SUCCESS : Intent.PRIMARY}
                            />
                        </Notification>
                    );
                })
            ) : simpleInput ? (
                <input type="file" id="fileInput" onChange={handleFileInputChange} multiple={allowMultiple} />
            ) : (
                <DragDrop
                    uppy={uppy}
                    locale={{ strings: { dropHereOr: "Drop file here or %{browse}", browse: "browse" } }}
                />
            )}
        </>
    );
}
