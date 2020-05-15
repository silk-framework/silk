import React from "react";
import ProgressBar from "@wrappers/blueprint/progressbar";
import XHR from "@uppy/xhr-upload";
import Uppy from "@uppy/core";
import "@uppy/core/dist/style.css";
import "@uppy/drag-drop/dist/style.css";
import "@uppy/progress-bar/dist/style.css";

import Loading from "../Loading";
import { UploadNewFile } from "./UploadNewFile";
import { IAutocompleteProps } from "../Autocomplete/Autocomplete";
import { FileMenu, FileMenuItems } from "./FileMenu";
import { SelectFileFromExisting } from "./SelectFileFromExisting";
import { CreateNewFile } from "./CreateNewFile";

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
        const { loading, simpleInput, allowMultiple, advanced, autocomplete, onChange } = this.props;

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
                            onChange={onChange}
                            onProgress={this.handleProgress}
                            onUploadSuccess={this.handleUploadSuccess}
                        />
                    )}
                    {selectedFileMenu === "EMPTY" && <CreateNewFile onChange={onChange} />}
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
