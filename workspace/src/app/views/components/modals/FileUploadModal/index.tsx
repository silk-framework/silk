import React, { useEffect, useState } from "react";
import { Classes, Intent } from "../../../../wrappers/blueprint/constants";
import Button from "../../../../wrappers/blueprint/button";
import Dialog from "../../../../wrappers/blueprint/dialog";
import Uppy from '@uppy/core';
import { DragDrop } from '@uppy/react';
import XHR from '@uppy/xhr-upload';
import ProgressBar from "../../../../wrappers/blueprint/progressbar";
import Loading from "../../Loading";
import AbortAlert from "./AbortAlert";
import OverrideAlert from "./OverrideAlert";

import '@uppy/core/dist/style.css';
import '@uppy/drag-drop/dist/style.css'
import '@uppy/progress-bar/dist/style.css';

export interface IFileUploadModalProps {
    isOpen: boolean;

    uploadUrl: string;

    onDiscard(): void;

    onAbortUploading?(): void;

    onCheckFileExists?(fileName: string);

    onUpload?(file: File): void;
}

const uppy = Uppy({
    autoProceed: false,
});

export default function FileUploadModal({isOpen, onDiscard, onAbortUploading, onCheckFileExists, onUpload, uploadUrl}: IFileUploadModalProps) {
    const [isCheckingFile, setIsCheckingFile] = useState<boolean>(false);
    const [fileProgress, setFileProgress] = useState<number>(0);
    const [isUploading, setIsUploading] = useState<boolean>(false);
    const [openAbortDialog, setOpenAbortDialog] = useState<boolean>(false);
    const [overrideDialog, setOverrideDialog] = useState<File>(null);

    useEffect(() => {
        uppy.use(XHR, {
            method: 'PUT',
            fieldName: 'file',
            metaFields: [],
        });
        uppy.on('file-added', onFileAdded);
        uppy.on('upload-progress', onProgress);
        uppy.on('upload-success', resetFileDialog);
    }, []);

    const resetFileDialog = () => {
        setIsCheckingFile(false);
        setIsUploading(false);
        setFileProgress(0);
        setOpenAbortDialog(false);
        setOverrideDialog(null);
        uppy.reset();
    };

    const handleAbort = () => {
        uppy.cancelAll();
        resetFileDialog();
        if (onAbortUploading) {
            onAbortUploading();
        }
    };

    const upload = async (file) => {
        // @ts-ignore
        uppy.getPlugin('XHRUpload').setOptions({
            endpoint: `${uploadUrl}/${file.name}`,
        });
        setIsUploading(true);
        await uppy.upload();
        resetFileDialog();
        if (onUpload) {
            onUpload(file);
        }

    };

    const onProgress = (file, {bytesUploaded, bytesTotal}) => {
        const progress = 100.0 * (bytesUploaded / bytesTotal);
        setFileProgress(progress);
    };

    const onFileAdded = async (result) => {
        if (onCheckFileExists) {
            setIsCheckingFile(true);
            const isExists = await onCheckFileExists(result.name);
            setIsCheckingFile(false);

            if (isExists) {
                setOverrideDialog(result);
            } else {
                upload(result);
            }
        }
    };

    const handleDiscard = () => {
        if (isUploading) {
            setOpenAbortDialog(true);
            return false;
        }
        resetFileDialog();
        onDiscard();
    };

    const handleOverrideCancel = () => {
        uppy.reset();
        setOverrideDialog(null);
    };

    return <>
        <Dialog
            onClose={handleDiscard}
            title="Upload New File"
            isOpen={isOpen}
        >
            <div className={Classes.DIALOG_BODY}>
                {
                    isCheckingFile ? <Loading/> : <>
                        <DragDrop uppy={uppy} allowMultipleFiles={false}/>
                        {!!fileProgress &&
                        <div>
                            <p>
                                Waiting for finished file upload to show data preview.
                                You can also create the dataset now and configure it later.
                            </p>
                            <ProgressBar value={fileProgress}/>
                        </div>
                        }
                    </>
                }

            </div>
            <div className={Classes.DIALOG_FOOTER}>
                <div className={Classes.DIALOG_FOOTER_ACTIONS}>
                    {
                        isUploading && <Button
                            intent={Intent.PRIMARY}
                            onClick={handleDiscard}
                        >
                            Abort Upload
                        </Button>
                    }
                    {
                        !isUploading && <Button onClick={onDiscard}>Close</Button>
                    }
                </div>
            </div>

        </Dialog>
        <AbortAlert
            isOpen={openAbortDialog}
            onCancel={() => setOpenAbortDialog(false)}
            onConfirm={handleAbort}
        />
        <OverrideAlert
            isOpen={overrideDialog}
            onCancel={handleOverrideCancel}
            onConfirm={() => upload(overrideDialog)}
        />
    </>
}
