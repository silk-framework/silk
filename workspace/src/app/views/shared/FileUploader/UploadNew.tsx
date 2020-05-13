import { DragDrop } from "@uppy/react";
import React from "react";
import Uppy from "@uppy/core";

interface IProps {
    // Uppy instance
    uppy: Uppy.Uppy<any>;

    // Recognize component view
    simpleInput?: boolean;

    // Allow multiple file upload
    allowMultiple?: boolean;
}

/**
 *
 */
export function UploadNew({ uppy, simpleInput, allowMultiple }: IProps) {
    const handleInputChange = (event) => {
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

    return simpleInput ? (
        <input type="file" id="fileInput" onChange={handleInputChange} />
    ) : (
        <DragDrop uppy={uppy} allowMultipleFiles={allowMultiple} />
    );
}
