import React from "react";
import ProgressBar from "@wrappers/blueprint/progressbar";
import XHR from "@uppy/xhr-upload";
import Uppy from "@uppy/core";
import "@uppy/core/dist/style.css";
import "@uppy/drag-drop/dist/style.css";
import "@uppy/progress-bar/dist/style.css";

import Loading from "../Loading";
import { UploadNewFile } from "./cases/UploadNewFile";
import { IAutocompleteProps } from "../Autocomplete/Autocomplete";
import { FileMenu, FileMenuItems } from "./FileMenu";
import { SelectFileFromExisting } from "./cases/SelectFileFromExisting";
import { CreateNewFile } from "./cases/CreateNewFile";
import { requestIfResourceExists } from "@ducks/workspace/requests";
import AbortAlert from "../modals/FileUploadModal/AbortAlert";
import OverrideAlert from "../modals/FileUploadModal/OverrideAlert";
import { legacyApiEndpoint } from "../../../utils/getApiEndpoint";
import { Button } from "@wrappers/index";

interface IUploaderInstance {
    /**
     * Reset file uploader
     * @see uppy.reset
     */
    reset();

    upload();

    cancelAll();

    /**
     * Update upload endpoint
     * @param endpoint
     */
    setEndpoint(endpoint: string);
}

export interface IUploaderOptions {
    /**
     * @required
     */
    projectId: string;

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
    onUploadSuccess?();

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
    // Uploader progress
    progress: number;

    // Selected File menu item
    selectedFileMenu: FileMenuItems;

    // Loading state
    loading: boolean;

    // Override dialog
    overrideDialog: File | null;

    // abort dialog
    abortDialog: boolean;

    //Show upload process
    isUploading: boolean;
}

/**
 * File Uploader widget
 * with advanced = true, provides full FileUploader with 2 extra options
 * otherwise provides simple drang and drop uploader
 */
export class FileUploader extends React.Component<IUploaderOptions, IState> {
    private uppy = Uppy();

    constructor(props) {
        super(props);

        this.state = {
            progress: 0,
            loading: false,
            selectedFileMenu: props.advanced ? "SELECT" : "NEW",
            isUploading: false,
            abortDialog: false,
            overrideDialog: null,
        };

        this.uppy.use(XHR, {
            method: "PUT",
            fieldName: "file",
            metaFields: [],
        });
    }

    componentDidMount(): void {
        if (this.props.getInstance) {
            this.props.getInstance({
                reset: this.reset,
                upload: this.upload,
                cancelAll: this.cancelAll,
                setEndpoint: this.setEndpoint,
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

    /**
     * Set upload endpoint
     * @param endpoint
     */
    setEndpoint = (endpoint: string) => {
        this.uppy.getPlugin("XHRUpload").setOptions({
            endpoint,
        });
    };

    handleProgress = (file, { bytesUploaded, bytesTotal }) => {
        const progress = 100.0 * (bytesUploaded / bytesTotal);
        this.setState({
            progress,
        });
        if (this.props.onProgress) {
            this.props.onProgress(progress);
        }
    };

    handleUploadSuccess = () => {
        this.setState({
            progress: 0,
        });
        this.reset();
        if (this.props.onUploadSuccess) {
            this.props.onUploadSuccess();
        }
    };

    handleFileMenuChange = (value: FileMenuItems) => {
        this.setState({
            selectedFileMenu: value,
        });
        this.reset();
    };

    isResourceExists = async (fileName: string) => {
        try {
            const res = await requestIfResourceExists(this.props.projectId, fileName);
            return !!res.size;
        } catch {
            return false;
        }
    };

    handleFileAdded = async (result: any) => {
        try {
            this.setState({ loading: true });
            const isExists = await this.isResourceExists(result.name);

            isExists ? this.setState({ overrideDialog: result }) : this.handleUpload(result);
        } finally {
            this.setState({ loading: false });
        }
    };

    handleUpload = async (file: any) => {
        const { projectId } = this.props;

        const uploadUrl = legacyApiEndpoint(`/projects/${projectId}/resources`);
        this.setEndpoint(`${uploadUrl}/${file.name}`);

        this.setState({ isUploading: true });
        try {
            await this.uppy.upload();
            this.props.onChange(file.name);
            this.reset();
        } catch (e) {
            console.log(e);
        } finally {
            this.setState({
                isUploading: false,
            });
        }
    };

    handleAbort = () => {
        this.setState({ abortDialog: true });
        this.reset();
    };

    handleOverride = () => {
        this.handleUpload(this.state.overrideDialog);
        this.setState({
            overrideDialog: null,
        });
    };

    reset = () => {
        this.setState({
            progress: 0,
            overrideDialog: null,
            abortDialog: false,
            loading: false,
        });
        this.uppy.cancelAll();
        this.uppy.reset();
    };

    render() {
        const { progress, selectedFileMenu, loading, abortDialog, overrideDialog } = this.state;
        const { simpleInput, allowMultiple, advanced, autocomplete, onChange } = this.props;

        return loading ? (
            <Loading />
        ) : (
            <>
                {advanced && <FileMenu onChange={this.handleFileMenuChange} selectedFileMenu={selectedFileMenu} />}

                <div>
                    {selectedFileMenu === "SELECT" && (
                        <SelectFileFromExisting autocomplete={autocomplete} onChange={onChange} />
                    )}
                    {selectedFileMenu === "NEW" && (
                        <UploadNewFile
                            uppy={this.uppy}
                            simpleInput={simpleInput}
                            allowMultiple={allowMultiple}
                            onAdded={this.handleFileAdded}
                            onProgress={this.handleProgress}
                            onUploadSuccess={this.handleUploadSuccess}
                        />
                    )}
                    {selectedFileMenu === "EMPTY" && <CreateNewFile onChange={onChange} />}
                </div>
                {!!progress && (
                    <div>
                        <p>Waiting for finished file upload to show data preview.</p>
                        <ProgressBar value={progress} />

                        <Button onClick={this.handleAbort}>Abort Upload</Button>
                    </div>
                )}

                <AbortAlert
                    isOpen={abortDialog}
                    onCancel={() => this.setState({ abortDialog: false })}
                    onConfirm={this.reset}
                />
                <OverrideAlert isOpen={overrideDialog} onCancel={this.reset} onConfirm={this.handleOverride} />
            </>
        );
    }
}
