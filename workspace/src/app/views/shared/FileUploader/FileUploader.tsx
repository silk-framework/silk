import React from "react";
import XHR from "@uppy/xhr-upload";
import Uppy, { UppyFile } from "@uppy/core";
import "@uppy/core/dist/style.css";
import "@uppy/drag-drop/dist/style.css";
import "@uppy/progress-bar/dist/style.css";
import { Button, Divider, FieldItem, Icon, Notification, TextField } from "@gui-elements/index";
import { IAutocompleteProps } from "../Autocomplete/Autocomplete";
import OverrideAlert from "../modals/FileUploadModal/OverrideAlert";
import { UploadNewFile } from "./cases/UploadNewFile";
import { FileMenu, FileMenuItems } from "./FileMenu";
import { SelectFileFromExisting } from "./cases/SelectFileFromExisting";
import { CreateNewFile } from "./cases/CreateNewFile";
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
     * The indicator show simple file input or drop zone
     */
    simpleInput?: boolean;

    /**
     * @default false
     * if advanced is true, then show file uploader with multiple options
     */
    advanced?: boolean;

    /**
     * autocomplete option useful when advanced is true
     */
    autocomplete?: IAutocompleteProps;

    /**
     * Called when:
     * - New file added
     * - Select resource from autocomplete
     * - Write new file name
     */
    onChange?(value: File | string);
}

interface IState {
    // Selected File menu item
    selectedFileMenu: FileMenuItems;

    // Override dialog
    onlyOverrides: UppyFile[];

    //Show upload process
    isUploading: boolean;

    //Update default value in case that file is already given
    showActionsMenu: boolean;

    //Filename which shows in input for update action
    inputFilename: string;

    // If an error occurred this will contain the error message
    error: string | null;
}

const noop = () => {
    // @see https://gph.is/1Lddqze
};

/**
 * File Uploader widget
 * with advanced = true, provides full FileUploader with 2 extra options
 * otherwise provides simple drang and drop uploader
 */
export class FileUploader extends React.Component<IUploaderOptions, IState> {
    private uppy = Uppy({
        // @ts-ignore
        logger: Uppy.debugLogger,
    });

    private checkedFilesQueue = 0;

    constructor(props) {
        super(props);

        this.state = {
            selectedFileMenu: props.advanced ? "SELECT" : "NEW",
            isUploading: false,
            onlyOverrides: [],
            showActionsMenu: false,
            inputFilename: props.defaultValue || "",
            error: null,
        };

        this.uppy.use(XHR, {
            method: "PUT",
            fieldName: "file",
            metaFields: [],
            limit: 0,
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

    /**
     * @see Uppy.upload
     */
    upload = this.uppy.upload;

    /**
     * @see Uppy.cancelAll
     */
    cancelAll = this.uppy.cancelAll;

    handleUploadSuccess = (file: File) => {
        if (this.props.onUploadSuccess) {
            this.props.onUploadSuccess(file);
        }
        this.setState({
            inputFilename: file.name,
        });
        this.toggleFileResourceChange();
        console.log("Uploaded!");
        this.reset();
    };

    handleUploadError = (fileData, error) => {
        let errorDetails = error?.message ? error.message : "-";
        const idx = errorDetails.indexOf("Source error");
        if (idx > 0) {
            errorDetails = errorDetails.substring(0, idx);
        }
        const errorMessage = `An upload error has occurred for file '${fileData.name}'. Details: ${errorDetails}`;
        this.setState({ error: errorMessage });
    };

    handleFileMenuChange = (value: FileMenuItems) => {
        this.setState({
            selectedFileMenu: value,
        });
        this.reset();
    };

    handleFileAdded = async (file: UppyFile) => {
        try {
            const { projectId } = this.props;

            this.uppy.setFileState(file.id, {
                isOverride: await requestIfResourceExists(projectId, file.name),
            });

            this.checkedFilesQueue++;
            // if all files checked and on final line
            const isCompleteAllChecks = this.checkedFilesQueue === this.uppy.getFiles().length;
            if (isCompleteAllChecks) {
                const files = this.uppy.getFiles();
                const onlyOverrides = files.filter((file: any) => file.isOverride);
                this.setState({
                    onlyOverrides,
                });

                onlyOverrides.forEach((f) => this.uppy.removeFile(f.id));

                await this.handleUpload(this.uppy.getFiles());

                this.checkedFilesQueue = 0;
            }
        } finally {
        }
    };

    handleUpload = async (files) => {
        const { projectId } = this.props;

        this.setState({
            isUploading: true,
            error: null,
        });

        const endpoint = `${legacyApiEndpoint(`/projects/${projectId}/resources`)}`;
        files.forEach((file) => {
            this.uppy.setFileState(file.id, {
                xhrUpload: {
                    endpoint: `${endpoint}/${encodeURIComponent(file.name)}`,
                },
            });
        });

        try {
            const result = await this.uppy.upload();
            if (this.props.onChange && result?.successful) {
                // this.props.onChange(file.name);
            }
        } catch (e) {
            console.log(e);
        } finally {
            this.setState({ isUploading: false });
        }
    };

    handleOverride = async () => {
        this.state.onlyOverrides.forEach((file) => {
            this.uppy.addFile(file);
        });

        await this.handleUpload(this.state.onlyOverrides);
        this.handleOverrideCancel();
    };

    reset = () => {
        this.uppy.cancelAll();
        this.uppy.reset();
    };

    handleOverrideCancel = () => {
        this.reset();
        this.setState({
            onlyOverrides: [],
        });
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

    render() {
        const { selectedFileMenu, showActionsMenu, inputFilename, onlyOverrides } = this.state;
        const { simpleInput, allowMultiple, advanced, autocomplete, defaultValue } = this.props;

        return (
            <>
                {defaultValue && !showActionsMenu && (
                    <FieldItem>
                        <TextField
                            readOnly
                            value={inputFilename}
                            onChange={noop}
                            rightElement={
                                <Button
                                    minimal
                                    text={"Change file"}
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
                            text={"Abort and keep file"}
                            icon={<Icon name="operation-undo" />}
                            onClick={this.handleDiscardChanges}
                        />
                        <Divider addSpacing="large" />
                    </>
                )}
                {(!defaultValue || showActionsMenu) && (
                    <>
                        {advanced && (
                            <FileMenu onChange={this.handleFileMenuChange} selectedFileMenu={selectedFileMenu} />
                        )}

                        <div>
                            {selectedFileMenu === "SELECT" && (
                                <SelectFileFromExisting
                                    autocomplete={autocomplete}
                                    onChange={this.handleFileNameChange}
                                />
                            )}
                            {selectedFileMenu === "NEW" && (
                                <>
                                    <UploadNewFile
                                        uppy={this.uppy}
                                        simpleInput={simpleInput}
                                        allowMultiple={allowMultiple}
                                        onProgress={this.props.onProgress}
                                        onAdded={this.handleFileAdded}
                                        onUploadSuccess={this.handleUploadSuccess}
                                        onUploadError={this.handleUploadError}
                                    />
                                    {this.state.error && <Notification message={this.state.error} danger />}
                                </>
                            )}
                            {selectedFileMenu === "EMPTY" && (
                                <CreateNewFile onChange={this.props.onChange} confirmationButton={!!defaultValue} />
                            )}
                        </div>

                        <OverrideAlert
                            files={onlyOverrides}
                            isOpen={!!onlyOverrides.length}
                            onCancel={this.handleOverrideCancel}
                            onConfirm={this.handleOverride}
                        />
                    </>
                )}
            </>
        );
    }
}
