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

    onChange(file: File);

    onProgress?(file: File, data: any);

    onUploadSuccess?();
}

/**
 * The Widget for "Upload new file" option
 */
export function UploadNewFile(props: IProps) {
    const { uppy, simpleInput, allowMultiple, onChange, onProgress, onUploadSuccess } = props;

    useEffect(() => {
        registerEvents();
    }, []);

    const registerEvents = () => {
        uppy.on("file-added", onChange);
        uppy.on("upload-progress", onProgress);
        uppy.on("upload-success", onUploadSuccess);
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

    // const uploadedFiles = uppy.getFiles();

    return (
        <div>
            {simpleInput ? (
                <input type="file" id="fileInput" onChange={handleFileInputChange} />
            ) : (
                <DragDrop uppy={uppy} allowMultipleFiles={allowMultiple} />
            )}
            {/*{*/}
            {/*    !!uploadedFiles.length && <>*/}
            {/*        <p><strong>Added Files</strong></p>*/}
            {/*        {*/}
            {/*            uploadedFiles.map(item => <div key={item.id}>{item.name}</div>)*/}
            {/*        }*/}
            {/*    </>*/}
            {/*}*/}
        </div>
    );
}
