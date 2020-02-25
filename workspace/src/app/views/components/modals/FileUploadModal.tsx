import React, { useState } from "react";
import { Classes, Intent } from "@wrappers/blueprint/constants";
import Button from "@wrappers/blueprint/button";
import Dialog from "@wrappers/blueprint/dialog";
import Uppy from '@uppy/core';
import { DragDrop } from '@uppy/react';
import XHR from '@uppy/xhr-upload';

export interface IFileUploadModalProps {
    isOpen: boolean;

    uploadUrl: string;

    onDiscard(): void;

    onAbortUploading(): void;

    onUpload(file: File[]): void;
}

export default function FileUploadModal({isOpen, onDiscard, onAbortUploading, onUpload, uploadUrl}: IFileUploadModalProps) {
    const [addedFiles, setAddedFiles] = useState<File[]>([]);

    const uppy = Uppy({
        autoProceed: true,
    });



    uppy.on('file-added', (result) => {
        uppy.use(XHR, {
            endpoint: uploadUrl + '/resources/' + result.name,
            method: 'PUT',
            fieldName: 'file',
            metaFields: []
        });
    });

    const handleFile = (evt, {addedFiles}) => {
        setAddedFiles(addedFiles);
        onUpload(addedFiles);
    };

    return (
        <Dialog
            onClose={onDiscard}
            title="Confirm Deletion"
            isOpen={isOpen}
        >
            <div className={Classes.DIALOG_BODY}>
                <DragDrop
                    uppy={uppy}
                    allowMultipleFiles={false}

                />
            </div>
            <div className={Classes.DIALOG_FOOTER}>
                <div className={Classes.DIALOG_FOOTER_ACTIONS}>
                    {
                        !!addedFiles.length && <Button
                            intent={Intent.PRIMARY}
                            onClick={onAbortUploading}
                        >
                            Abort Upload
                        </Button>
                    }
                    <Button onClick={onDiscard}>Cancel</Button>
                </div>
            </div>

        </Dialog>
    )
}
