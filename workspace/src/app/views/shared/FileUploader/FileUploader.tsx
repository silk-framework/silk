import React from "react";
import ProgressBar from "@wrappers/blueprint/progressbar";
import XHR from "@uppy/xhr-upload";
import Uppy from "@uppy/core";
import "@uppy/core/dist/style.css";
import "@uppy/drag-drop/dist/style.css";
import "@uppy/progress-bar/dist/style.css";

import Loading from "../Loading";
import { UploadNew } from "./UploadNew";
import { Autocomplete, IAutocompleteProps } from "../Autocomplete/Autocomplete";
import { FileMenu, FileMenuItems } from "./FileMenu";

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

    loading?: boolean;

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
}

interface IState {
    // Uploader progress
    progress: number;

    // Selected File menu item
    selectedFileMenu: FileMenuItems;
}

export class FileUploader extends React.Component<IUploaderOptions, IState> {
    private uppy = Uppy();

    constructor(props) {
        super(props);

        this.state = {
            progress: 0,
            selectedFileMenu: props.advanced ? "SELECT" : "NEW",
        };

        this.uppy.use(XHR, {
            method: "PUT",
            fieldName: "file",
            metaFields: [],
        });

        this.uppy.on("file-added", this.onFileAdded);
        this.uppy.on("upload-progress", this.onProgress);
        this.uppy.on("upload-success", this.onUploadSuccess);
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

    upload = this.uppy.upload;
    cancelAll = this.uppy.cancelAll;

    setEndpoint = (endpoint: string) => {
        // @ts-ignore
        this.uppy.getPlugin("XHRUpload").setOptions({
            endpoint,
        });
    };

    onFileAdded = (result: File) => {
        if (this.props.onFileAdded) {
            this.props.onFileAdded(result);
        }
    };

    onProgress = (file, { bytesUploaded, bytesTotal }) => {
        const progress = 100.0 * (bytesUploaded / bytesTotal);
        this.setState({
            progress,
        });
        if (this.props.onProgress) {
            this.props.onProgress(progress);
        }
    };

    onUploadSuccess = () => {
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
    };

    reset = () => {
        this.setState({
            progress: 0,
        });
        this.uppy.cancelAll();
        this.uppy.reset();
    };

    render() {
        const { progress, selectedFileMenu } = this.state;
        const { loading, simpleInput, allowMultiple, advanced, autocomplete } = this.props;

        return loading ? (
            <Loading />
        ) : (
            <>
                {advanced ? (
                    <FileMenu onChange={this.handleFileMenuChange} selectedFileMenu={selectedFileMenu} />
                ) : null}
                <div>
                    {selectedFileMenu === "SELECT" && <Autocomplete {...autocomplete} />}
                    {selectedFileMenu === "NEW" && (
                        <UploadNew uppy={this.uppy} simpleInput={simpleInput} allowMultiple={allowMultiple} />
                    )}
                    {selectedFileMenu === "EMPTY" && null}
                </div>
                {!!progress && (
                    <div>
                        <p>
                            Waiting for finished file upload to show data preview. You can also create the dataset now
                            and configure it later.
                        </p>
                        <ProgressBar value={progress} />
                    </div>
                )}
            </>
        );
    }
}
