import React from "react";
import Uppy, { UppyFile } from "@uppy/core";
import "@uppy/core/dist/style.css";
import "@uppy/drag-drop/dist/style.css";
import "@uppy/progress-bar/dist/style.css";

import { Button, Divider, FieldItem, Icon, TextField } from "@eccenca/gui-elements";
import { SuggestFieldProps } from "@eccenca/gui-elements/src/components/AutocompleteField/AutoCompleteField";
import { UploadNewFile } from "./cases/UploadNewFile/UploadNewFile";
import { FileSelectionOptions, FileMenuItems } from "./FileSelectionOptions";
import { SelectFileFromExisting } from "./cases/SelectFileFromExisting";
import { CreateNewFile } from "./cases/CreateNewFile";
import i18next from "../../../../language";
import { requestIfResourceExists } from "@ducks/workspace/requests";
import { legacyApiEndpoint } from "../../../utils/getApiEndpoint";
import { withTranslation } from "react-i18next";
import XHR from "@uppy/xhr-upload";

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
    required: boolean;

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
        autocomplete: SuggestFieldProps<any, any>;
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
    insideModal: boolean;

    /** Callback that is called when the state of all uploads being successfully done has changed.
     * Reasons for non-success are: uploads are in progress, user interaction is needed, errors have occurred.*/
    allFilesSuccessfullyUploadedHandler?: (allSuccessful: boolean) => any;

    listenToUploadedFiles: (files: UppyFile[]) => void;
}

interface IState {
    // Selected File menu item
    selectedFileMenu: FileMenuItems;

    //Show upload process
    isUploading: boolean;

    //Update default value in case that file is already given
    showActionsMenu: boolean;

    //Filename which shows in input for update action
    inputFilename: string;

    //Toggle File delete dialog, contains filename or empty string
    visibleFileDelete: string;

    // The ID of the file selection menu
    id: string;
}

const noop = () => {
    // @see https://gph.is/1Lddqze
};

/**
 * File Uploader widget
 * with advanced = true, provides full FileUploader with 2 extra options
 * otherwise provides simple drag and drop uploader
 */
class FileSelectionMenu extends React.Component<IUploaderOptions, IState> {
    private uppy = Uppy({
        // @ts-ignore
        logger: Uppy.debugLogger,
    });

    /**
     * @see Uppy.upload
     */
    public upload = this.uppy.upload;

    /**
     * @see Uppy.reset
     */
    public reset = this.uppy.reset;

    /**
     * @see Uppy.cancelAll
     */
    public cancelAll = this.uppy.cancelAll;

    constructor(props) {
        super(props);

        this.state = {
            selectedFileMenu: props.advanced ? "SELECT" : "NEW",
            isUploading: false,
            showActionsMenu: false,
            inputFilename: props.defaultValue || "",
            visibleFileDelete: "",
            id: props.id,
        };

        if (props.maxFileUploadSizeBytes) {
            this.uppy.setOptions({
                restrictions: {
                    maxFileSize: props.maxFileUploadSizeBytes,
                    // Restrict to 1 file if allowMultiple == false
                    maxNumberOfFiles: props.allowMultiple ? undefined : 1,
                },
            });
        }
        this.uppy.use(XHR, {
            method: "PUT",
            fieldName: "file",
            allowMultipleUploads: props.allowMultiple,
            // Only upload one file at the same time
            limit: 1,
        });
    }

    componentDidMount(): void {
        if (this.props.getInstance) {
            this.props.getInstance({
                reset: this.reset,
                upload: this.upload,
                cancelAll: this.cancelAll,
            });
        }
    }

    handleUploadSuccess = (file: any) => {
        if (this.props.onUploadSuccess) {
            this.props.onUploadSuccess(file);
        }
        this.setState({
            inputFilename: file.name,
        });
        this.toggleFileResourceChange();
    };

    handleFileMenuChange = (value: FileMenuItems) => {
        this.setState({
            selectedFileMenu: value,
        });
        this.reset();
    };

    /**
     * "Abort and Keep File" Handler
     * revert value back
     */
    handleDiscardChanges = () => {
        const isVisible = !this.state.showActionsMenu;
        if (!isVisible) {
            this.handleFileNameChange(this.state.inputFilename);
        } else {
            // just open
            this.toggleFileResourceChange();
        }
    };

    /**
     * Open/close file uploader options
     */
    toggleFileResourceChange = () => {
        this.setState({
            showActionsMenu: !this.state.showActionsMenu,
        });
    };

    /**
     * Change readonly input value
     * @param value
     */
    handleFileNameChange = (value: string) => {
        this.setState({
            inputFilename: value,
        });
        this.props.onChange(value);
        this.toggleFileResourceChange();
    };

    validateBeforeFileAdded = async (fileName: string): Promise<boolean> => {
        return await requestIfResourceExists(this.props.projectId, fileName);
    };

    render() {
        const { selectedFileMenu, showActionsMenu, inputFilename } = this.state;
        const { allowMultiple, advanced, defaultValue, onProgress, projectId, onChange } = this.props;

        return (
            <div id={this.state.id}>
                {defaultValue && !showActionsMenu && (
                    <FieldItem>
                        <TextField
                            readOnly
                            value={inputFilename}
                            onChange={noop}
                            rightElement={
                                <Button
                                    data-test-id="file-selection-change-file-btn"
                                    minimal
                                    text={i18next.t("FileUploader.changeFile", "Change file")}
                                    icon={<Icon name="item-edit" />}
                                    onClick={this.toggleFileResourceChange}
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
                            onClick={this.handleDiscardChanges}
                        />
                        <Divider addSpacing="large" />
                    </>
                )}
                {(!defaultValue || showActionsMenu) && (
                    <>
                        {advanced && (
                            <FileSelectionOptions
                                onChange={this.handleFileMenuChange}
                                selectedFileMenu={selectedFileMenu}
                            />
                        )}

                        <div>
                            {advanced && selectedFileMenu === "SELECT" && (
                                <SelectFileFromExisting
                                    autocomplete={advanced.autocomplete}
                                    onChange={this.handleFileNameChange}
                                    labelAttributes={{
                                        text: this.props.t(
                                            "FileUploader.selectFromProject",
                                            "Select file from projects",
                                        ),
                                        info: this.props.t("common.words.required"),
                                        htmlFor: "autocompleteInput",
                                    }}
                                    required={this.props.required}
                                    insideModal={this.props.insideModal}
                                />
                            )}
                            {selectedFileMenu === "NEW" && (
                                <>
                                    <UploadNewFile
                                        uppy={this.uppy}
                                        projectId={projectId}
                                        allowMultiple={allowMultiple}
                                        onProgress={onProgress}
                                        onUploadSuccess={this.handleUploadSuccess}
                                        validateBeforeAdd={this.validateBeforeFileAdded}
                                        uploadEndpoint={`${legacyApiEndpoint(`/projects/${projectId}/files`)}`}
                                        attachFileNameToEndpoint={true}
                                        listenToUploadedFiles={this.props.listenToUploadedFiles}
                                        allFilesSuccessfullyUploadedHandler={
                                            this.props.allFilesSuccessfullyUploadedHandler
                                        }
                                    />
                                </>
                            )}
                            {advanced && selectedFileMenu === "EMPTY" && (
                                <CreateNewFile onChange={onChange} confirmationButton={!!defaultValue} />
                            )}
                        </div>
                    </>
                )}
            </div>
        );
    }
}

export default withTranslation()(FileSelectionMenu);
