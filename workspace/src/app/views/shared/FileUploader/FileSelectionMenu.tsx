import React from "react";

import { Button, Divider, FieldItem, FileUploadFile, Icon, TextField } from "@eccenca/gui-elements";
import { SuggestFieldProps } from "@eccenca/gui-elements/src/components/AutocompleteField/AutoCompleteField";
import ProjectResourceUpload, { ProjectResourceUploadHandle } from "./ProjectResourceUpload";
import { FileSelectionOptions, FileMenuItems } from "./FileSelectionOptions";
import { SelectFileFromExisting } from "./cases/SelectFileFromExisting";
import { CreateNewFile } from "./cases/CreateNewFile";
import i18next from "../../../../language";
import { withTranslation } from "react-i18next";

export interface IUploaderInstance {
    /** Reset the file uploader. */
    reset(): void;

    upload(): Promise<void>;

    cancelAll(): void;
}

export interface IUploaderOptions {
    id?: string;
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
    getInstance?(instance: IUploaderInstance): void;

    /**
     * Fired when file added
     * @param file
     */
    onFileAdded?(file: FileUploadFile): void;

    /**
     * Fired when upload successfully completed
     */
    onUploadSuccess?(file: FileUploadFile): void;

    /**
     * Fired file uploading progress
     * @param progress
     */
    onProgress?(progress: number): void;

    onUploadStateChange?(uploading: boolean): void;

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
    onChange(value: FileUploadFile | string);

    /** The max. file upload size in bytes. */
    maxFileUploadSizeBytes?: number;

    t(key: string, options?: object | string): string;

    /** When used inside a modal, the behavior of some components will be optimized. */
    insideModal: boolean;

    /** Callback that is called when the state of all uploads being successfully done has changed.
     * Reasons for non-success are: uploads are in progress, user interaction is needed, errors have occurred.*/
    allFilesSuccessfullyUploadedHandler?: (allSuccessful: boolean) => void;

    listenToUploadedFiles?: (files: FileUploadFile[]) => void;
}

interface IState {
    // Selected File menu item
    selectedFileMenu: FileMenuItems;

    //Update default value in case that file is already given
    showActionsMenu: boolean;

    //Filename which shows in input for update action
    inputFilename: string;

    // The ID of the file selection menu
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
export class FileSelectionMenu extends React.Component<IUploaderOptions, IState> {
    private uploader?: ProjectResourceUploadHandle;

    public upload = (): Promise<void> => this.uploader?.upload() ?? Promise.resolve();

    public reset = (): void => this.uploader?.reset();

    public cancelAll = (): void => this.uploader?.cancelAll();

    constructor(props) {
        super(props);

        this.state = {
            selectedFileMenu: props.advanced ? "SELECT" : "NEW",
            showActionsMenu: false,
            inputFilename: props.defaultValue || "",
            id: props.id,
        };
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

    componentWillUnmount(): void {
        this.cancelAll();
    }

    handleUploadSuccess = (file: FileUploadFile) => {
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
     * "Cancel upload and keep file" handler
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
                            text={i18next.t("FileUploader.abort", "Cancel upload and keep file")}
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
                                <ProjectResourceUpload
                                    ref={(uploader) => {
                                        this.uploader = uploader ?? undefined;
                                    }}
                                    projectId={projectId}
                                    allowMultiple={allowMultiple}
                                    maxFileUploadSizeBytes={this.props.maxFileUploadSizeBytes}
                                    onFileAdded={this.props.onFileAdded}
                                    onProgress={onProgress}
                                    onUploadStateChange={this.props.onUploadStateChange}
                                    onUploadSuccess={this.handleUploadSuccess}
                                    listenToUploadedFiles={this.props.listenToUploadedFiles}
                                    allFilesSuccessfullyUploadedHandler={this.props.allFilesSuccessfullyUploadedHandler}
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
    }
}

export default withTranslation()(FileSelectionMenu);
