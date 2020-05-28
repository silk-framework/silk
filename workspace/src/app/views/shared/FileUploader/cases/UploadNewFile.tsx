import { DragDrop } from "@uppy/react";
import React, { useEffect } from "react";
import Uppy from "@uppy/core";

interface IProps {
    // Uppy instance
    uppy: Uppy.Uppy<any>;

    // Recognize component view
    simpleInput?: boolean;

    // Allow multiple file upload
    allowMultiple?: boolean;

    onAdded(file: File);

    onProgress?(file: File, data: any);

    onUploadSuccess?();

    onUploadError?(e, f);
}

/**
 * The Widget for "Upload new file" option
 */
export function UploadNewFile(props: IProps) {
    const { uppy, simpleInput, allowMultiple, onAdded, onProgress, onUploadSuccess, onUploadError } = props;

    useEffect(() => {
        registerEvents();
        return unregisterEvents;
    }, []);

    const registerEvents = () => {
        uppy.on("file-added", onAdded);
        uppy.on("upload-progress", onProgress);
        uppy.on("upload-success", onUploadSuccess);
        uppy.on("upload-error", onUploadError);
    };

    const unregisterEvents = () => {
        uppy.off("file-added", onAdded);
        uppy.off("upload-progress", onProgress);
        uppy.off("upload-success", onUploadSuccess);
        uppy.off("upload-error", onUploadError);
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
            {simpleInput ? (
                <input type="file" id="fileInput" onChange={handleFileInputChange} />
            ) : (
                <DragDrop uppy={uppy} />
            )}
        </div>
    );
}
