import React from "react";
import {
    Button,
    FileUpload,
    FileUploadFile,
    FileUploadHandle,
    FileUploadState,
    IconButton,
    Notification,
    Spacing,
} from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";

import { requestIfResourceExists } from "@ducks/workspace/requests";
import { legacyApiEndpoint } from "../../../utils/getApiEndpoint";
import { useFileUploadLabels } from "./useFileUploadLabels";

export interface ProjectResourceUploadHandle {
    upload(): Promise<void>;
    reset(): void;
    cancelAll(): void;
}

interface ProjectResourceUploadProps {
    projectId: string;
    allowMultiple?: boolean;
    maxFileUploadSizeBytes?: number;
    onFileAdded?: (file: FileUploadFile) => void;
    onProgress?: (progress: number) => void;
    onUploadSuccess?: (file: FileUploadFile) => void;
    onUploadStateChange?: (uploading: boolean) => void;
    allFilesSuccessfullyUploadedHandler?: (allSuccessful: boolean) => void;
    listenToUploadedFiles?: (files: FileUploadFile[]) => void;
}

interface ReplacementPrompt {
    file: FileUploadFile;
    resolve: (replace: boolean) => void;
}

const ProjectResourceUpload = React.forwardRef<ProjectResourceUploadHandle, ProjectResourceUploadProps>(
    function ProjectResourceUpload(
        {
            projectId,
            allowMultiple = false,
            maxFileUploadSizeBytes,
            onFileAdded,
            onProgress,
            onUploadSuccess,
            onUploadStateChange,
            allFilesSuccessfullyUploadedHandler,
            listenToUploadedFiles,
        },
        ref,
    ) {
        const [t] = useTranslation();
        const labels = useFileUploadLabels();
        const uploadRef = React.useRef<FileUploadHandle>(null);
        const mounted = React.useRef(true);
        const [pendingReplacements, setPendingReplacements] = React.useState<ReplacementPrompt[]>([]);
        const [uploadedFiles, setUploadedFiles] = React.useState<FileUploadFile[]>([]);
        const [uploadState, setUploadState] = React.useState<Readonly<FileUploadState>>();
        React.useEffect(() => {
            mounted.current = true;
            return () => {
                mounted.current = false;
            };
        }, []);
        React.useEffect(() => {
            listenToUploadedFiles?.(uploadedFiles);
        }, [listenToUploadedFiles, uploadedFiles]);

        const beforeUpload = React.useCallback(
            async (file: FileUploadFile, signal: AbortSignal): Promise<boolean> => {
                const exists = await requestIfResourceExists(projectId, file.name);
                if (signal.aborted) return false;
                if (!exists) return true;
                return new Promise<boolean>((resolve) => {
                    const finish = (replace: boolean) => {
                        signal.removeEventListener("abort", cancel);
                        if (mounted.current)
                            setPendingReplacements((current) => current.filter((prompt) => prompt.file.id !== file.id));
                        resolve(replace);
                    };
                    const cancel = () => finish(false);
                    signal.addEventListener("abort", cancel, { once: true });
                    if (signal.aborted) cancel();
                    else setPendingReplacements((current) => [...current, { file, resolve: finish }]);
                });
            },
            [projectId],
        );

        React.useImperativeHandle(
            ref,
            () => ({
                upload: async () => {
                    await uploadRef.current?.upload();
                },
                reset: () => {
                    uploadRef.current?.reset();
                    setUploadedFiles([]);
                },
                cancelAll: () => {
                    uploadRef.current?.cancel();
                    setUploadedFiles([]);
                },
            }),
            [],
        );

        const handleStateChange = (state: Readonly<FileUploadState>) => {
            setUploadState(state);
            onProgress?.(state.progress / 100);
            onUploadStateChange?.(state.uploading > 0);
            allFilesSuccessfullyUploadedHandler?.(state.allSuccessful);
        };

        const cancelReplacementLabel = t("FileUploader.cancelReplacement");
        return (
            <>
                <FileUpload
                    ref={uploadRef}
                    beforeUpload={beforeUpload}
                    selectionDisabled={(uploadState?.uploading ?? 0) > 0}
                    endpoint={(file) =>
                        `${legacyApiEndpoint(`/projects/${projectId}/files`)}?path=${encodeURIComponent(file.name)}`
                    }
                    labels={labels}
                    maxFileSize={maxFileUploadSizeBytes}
                    maxNumberOfFiles={allowMultiple ? null : 1}
                    method="PUT"
                    name={t("FileUploader.uploadWidgetName")}
                    onFilesAdded={(files) => files.forEach((file) => onFileAdded?.(file))}
                    onStateChange={handleStateChange}
                    onUploadSuccess={({ file }) => {
                        setUploadedFiles((current) => [...current, file]);
                        onUploadSuccess?.(file);
                    }}
                />
                {pendingReplacements.length > 0 && <Spacing size="small" />}
                {pendingReplacements.map(({ file, resolve }, index) => (
                    <React.Fragment key={file.id}>
                        <Notification
                            actions={
                                <>
                                    <Button outlined text={t("common.action.replace")} onClick={() => resolve(true)} />
                                    <IconButton
                                        aria-label={cancelReplacementLabel}
                                        name="navigation-close"
                                        text={cancelReplacementLabel}
                                        tooltipAsTitle
                                        onClick={() => resolve(false)}
                                    />
                                </>
                            }
                            intent="warning"
                            isCloseButtonShown={false}
                            message={t("OverwriteModal.overwriteFile", { fileName: file.name })}
                        />
                        {index < pendingReplacements.length - 1 && (
                            <Spacing data-testid="replacement-notification-spacing" size="small" />
                        )}
                    </React.Fragment>
                ))}
            </>
        );
    },
);

export default ProjectResourceUpload;
