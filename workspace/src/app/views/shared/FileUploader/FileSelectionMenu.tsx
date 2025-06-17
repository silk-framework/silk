import React from "react";

import { Button, Divider, FieldItem, Icon, TextField, Uppy, UppyFile } from "@eccenca/gui-elements";
import { IAutoCompleteFieldProps } from "@eccenca/gui-elements/src/components/AutocompleteField/AutoCompleteField";
import { UploadNewFile } from "./cases/UploadNewFile/UploadNewFile";
import { FileSelectionOptions, FileMenuItems } from "./FileSelectionOptions";
import { SelectFileFromExisting } from "./cases/SelectFileFromExisting";
import { CreateNewFile } from "./cases/CreateNewFile";
import i18next from "../../../../language";
import { requestIfResourceExists } from "@ducks/workspace/requests";
import { legacyApiEndpoint } from "../../../utils/getApiEndpoint";

interface IUploaderInstance {
    /**
     * Reset file uploader
     * @see uppy.reset
     */
    reset();

    upload();

    cancelAll();
}

export interface IUploaderOptions {
    /**
     * @required
     */
    projectId: string;

    /**
     * @default undefined
     * holds the currently set file name
     */
    defaultValue?: string;

    /** Indicator that there needs to be a value set/selected, else the file selection (from existing files) can e.g. be reset. */
    required?: boolean;

    /**
     * return uploader API
     * @see IUploaderInstance
     * @param instance
     */
    getInstance?(instance: IUploaderInstance);

    /**
     * Fired when file added
     * @see this.uppy.on('file-added', this.onFileAdded);
     * @param file
     */
    onFileAdded?(file: File);

    /**
     * Fired when upload successfully completed
     * @see this.uppy.on('upload-success', this.onUploadSuccess);
     */
    onUploadSuccess?(file: File);

    /**
     * Fired file uploading progress
     * @param progress
     * @see this.uppy.on('upload-progress', this.onProgress)
     */
    onProgress?(progress: number);

    allowMultiple?: boolean;

    /**
     * @default false
     * if advanced is true, then show file uploader with multiple options
     * this option used in FileWidget for Task creation
     */
    advanced?: {
        // auto-completion of existing files
        autocomplete: IAutoCompleteFieldProps<any, any>;
    };

    /**
     * Called when:
     * - New file added
     * - Select resource from autocomplete
     * - Write new file name
     */
    onChange(value: File | string);

    /** The max. file upload size in bytes. */
    maxFileUploadSizeBytes?: number;

    t(key: string, options?: object | string): string;

    /** When used inside a modal, the behavior of some components will be optimized. */
    insideModal?: boolean;

    /** Callback that is called when the state of all uploads being successfully done has changed.
     * Reasons for non-success are: uploads are in progress, user interaction is needed, errors have occurred.*/
    allFilesSuccessfullyUploadedHandler?: (allSuccessful: boolean) => any;

    listenToUploadedFiles?: (files: UppyFile[]) => void;

    id?: string;
}

const noop = () => {
    // @see https://gph.is/1Lddqze
};

/**
 * File Uploader widget
 * with advanced = true, provides full FileUploader with 2 extra options
 * otherwise provides simple drag and drop uploader
 */
export const FileSelectionMenu: React.FC<IUploaderOptions> = (props) => {
    const [inputFileName, setInputFileName] = React.useState<string>("");
    const [selectedFileMenu, setSelectedFileMenu] = React.useState<FileMenuItems>(props.advanced ? "SELECT" : "NEW");
    const [showActionsMenu, setShowActionMenu] = React.useState<boolean>(false);
    const [uppy, setUppy] = React.useState<Uppy>();

    const upload = React.useCallback(() => uppy?.upload, [uppy]);
    const reset = React.useCallback(() => uppy?.reset, [uppy]);
    const cancelAll = React.useCallback(() => uppy?.cancelAll, [uppy]);

    React.useEffect(() => {
        props.getInstance &&
            props.getInstance({
                reset: reset,
                upload: upload,
                cancelAll: cancelAll,
            });
    }, []);

    const handleUploadSuccess = React.useCallback(
        (file: any) => {
            props.onUploadSuccess && props.onUploadSuccess(file);
            setInputFileName(file.name);
            toggleFileResourceChange();
        },
        [props.onUploadSuccess],
    );

    const handleFileMenuChange = React.useCallback((value: FileMenuItems) => {
        setSelectedFileMenu(value);
        reset();
    }, []);

    /**
     * "Abort and Keep File" Handler
     * revert value back
     */
    const handleDiscardChanges = React.useCallback(() => {
        !showActionsMenu ? handleFileNameChange(inputFileName) : toggleFileResourceChange();
    }, [showActionsMenu, inputFileName]);

    /**
     * Open/close file uploader options
     */
    const toggleFileResourceChange = React.useCallback(() => setShowActionMenu((s) => !s), []);

    /**
     * Change readonly input value
     * @param value
     */
    const handleFileNameChange = React.useCallback(
        (value: string) => {
            setInputFileName(value);
            props.onChange(value);
            toggleFileResourceChange();
        },
        [props.onChange],
    );

    const validateBeforeFileAdded = React.useCallback(
        async (fileName: string): Promise<boolean> => {
            return await requestIfResourceExists(props.projectId, fileName);
        },
        [props.projectId],
    );

    const { allowMultiple, advanced, defaultValue, onProgress, projectId, onChange } = props;

    const fileRestrictions = props.maxFileUploadSizeBytes
        ? {
              restriction: {
                  maxFileSize: props.maxFileUploadSizeBytes,
                  // Restrict to 1 file if allowMultiple == false
                  maxNumberOfFiles: props.allowMultiple ? undefined : 1,
              },
          }
        : {};

    return (
        <div id={props.id}>
            {defaultValue && !showActionsMenu && (
                <FieldItem>
                    <TextField
                        readOnly
                        value={inputFileName}
                        onChange={noop}
                        rightElement={
                            <Button
                                data-test-id="file-selection-change-file-btn"
                                minimal
                                text={i18next.t("FileUploader.changeFile", "Change file")}
                                icon={<Icon name="item-edit" />}
                                onClick={toggleFileResourceChange}
                            />
                        }
                    />
                </FieldItem>
            )}
            {defaultValue && showActionsMenu && (
                <>
                    <Button
                        outlined
                        small
                        text={i18next.t("FileUploader.abort", "Abort and keep file")}
                        icon={<Icon name="operation-undo" />}
                        onClick={handleDiscardChanges}
                    />
                    <Divider addSpacing="large" />
                </>
            )}
            {(!defaultValue || showActionsMenu) && (
                <>
                    {advanced && (
                        <FileSelectionOptions onChange={handleFileMenuChange} selectedFileMenu={selectedFileMenu} />
                    )}

                    <div>
                        {advanced && selectedFileMenu === "SELECT" && (
                            <SelectFileFromExisting
                                autocomplete={advanced.autocomplete}
                                onChange={handleFileNameChange}
                                labelAttributes={{
                                    text: props.t("FileUploader.selectFromProject", "Select file from projects"),
                                    info: props.t("common.words.required"),
                                    htmlFor: "autocompleteInput",
                                }}
                                required={!!props.required}
                                insideModal={!!props.insideModal}
                            />
                        )}
                        {selectedFileMenu === "NEW" && (
                            <UploadNewFile
                                xhrUploadOptions={{
                                    method: "PUT",
                                    fieldName: "file",
                                    // Only upload one file at the same time
                                    limit: 1,
                                    endpoint: `${legacyApiEndpoint(`/projects/${projectId}/files`)}`,
                                }}
                                {...fileRestrictions}
                                getUppyInstance={setUppy}
                                projectId={projectId}
                                allowMultiple={allowMultiple}
                                onProgress={onProgress}
                                onUploadSuccess={handleUploadSuccess}
                                validateBeforeAdd={validateBeforeFileAdded}
                                uploadEndpoint={`${legacyApiEndpoint(`/projects/${projectId}/files`)}`}
                                attachFileNameToEndpoint={true}
                                listenToUploadedFiles={props.listenToUploadedFiles}
                                allFilesSuccessfullyUploadedHandler={props.allFilesSuccessfullyUploadedHandler}
                            />
                        )}
                        {advanced && selectedFileMenu === "EMPTY" && (
                            <CreateNewFile onChange={onChange} confirmationButton={!!defaultValue} />
                        )}
                    </div>
                </>
            )}
        </div>
    );
};
