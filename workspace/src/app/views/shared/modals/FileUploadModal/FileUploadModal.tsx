import React, { useDebugValue, useState } from "react";
import { Button, SimpleDialog } from "@wrappers/index";
import FileUploader from "../../FileUploader";
import { useSelector } from "react-redux";
import { commonSel } from "@ducks/common";
import { IUploaderOptions } from "../../FileUploader/FileUploader";

export interface IFileUploadModalProps {
    isOpen: boolean;

    onDiscard(): void;

    uploaderOptions?: Partial<IUploaderOptions>;
}

export function FileUploadModal({ isOpen, onDiscard, uploaderOptions = {} }: IFileUploadModalProps) {
    const [fileUploaderInstance, setFileUploaderInstance] = useState<any>(null);

    const projectId = useSelector(commonSel.currentProjectIdSelector);

    useDebugValue(!projectId ? "Project ID not provided and upload url is not valid" : "");

    if (!projectId) {
        return null;
    }

    const getUploaderInstance = (instance) => {
        setFileUploaderInstance(instance);
    };

    const handleDiscard = () => {
        fileUploaderInstance.reset();
        onDiscard();
    };

    /**
     * @override uploader change function and handle it
     */
    const overriddenUploaderOptions = {
        ...uploaderOptions,
        projectId,
    };

    return (
        <>
            <SimpleDialog
                title="Upload file"
                size="small"
                isOpen={isOpen}
                onClose={handleDiscard}
                actions={
                    <Button key="close" onClick={onDiscard}>
                        Close
                    </Button>
                }
            >
                <FileUploader getInstance={getUploaderInstance} projectId={projectId} {...overriddenUploaderOptions} />
            </SimpleDialog>
        </>
    );
}
