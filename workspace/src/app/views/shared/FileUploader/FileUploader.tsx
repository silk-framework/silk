import React from "react";
import { DragDrop } from "@uppy/react";
import ProgressBar from "@wrappers/blueprint/progressbar";
import XHR from '@uppy/xhr-upload';
import Uppy from '@uppy/core';
import '@uppy/core/dist/style.css';
import '@uppy/drag-drop/dist/style.css'
import '@uppy/progress-bar/dist/style.css';
import Loading from "../Loading";

interface IUploaderInstance {
    reset();

    upload();

    cancelAll();

    setEndpoint(endpoint: string)
}

interface IProps {
    getInstance?(instance: IUploaderInstance);

    onFileAdded?(file: File);

    onUploadSuccess?();

    onProgress?(progress: number);

    allowMultiple?: boolean;

    disabled?: boolean;
}

interface IState {
    progress: number
}

export class FileUploader extends React.Component<IProps, IState> {
    private uppy = Uppy();

    constructor(props) {
        super(props);

        this.state = {
            progress: 0
        };

        this.uppy.use(XHR, {
            method: 'PUT',
            fieldName: 'file',
            metaFields: [],
        });

        this.uppy.on('file-added', this.onFileAdded);
        this.uppy.on('upload-progress', this.onProgress);
        this.uppy.on('upload-success', this.onUploadSuccess);
    }

    componentDidMount(): void {
        if (this.props.getInstance) {
            this.props.getInstance({
                reset: this.reset,
                upload: this.upload,
                cancelAll: this.cancelAll,
                setEndpoint: this.setEndpoint
            });
        }
    }

    upload = this.uppy.upload;
    cancelAll = this.uppy.cancelAll;

    setEndpoint = (endpoint: string) => {
        // @ts-ignore
        this.uppy.getPlugin('XHRUpload').setOptions({
            endpoint
        });
    };

    onFileAdded = (result: File) => {
        if (this.props.onFileAdded) {
            this.props.onFileAdded(result)
        }
    };

    onProgress = (file, {bytesUploaded, bytesTotal}) => {
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

    reset = () => {
        this.setState({
            progress: 0
        });
        this.uppy.cancelAll();
        this.uppy.reset();
    };

    render() {
        const {progress} = this.state;
        const {disabled, allowMultiple} = this.props;

        return (
            disabled
                ? <Loading/>
                : <>
                    <DragDrop uppy={this.uppy} allowMultipleFiles={allowMultiple}/>
                    {
                        !!progress && <div>
                            <p>Waiting for finished file upload to show data preview.
                                You can also create the dataset now and configure it later.</p>
                            <ProgressBar value={progress}/>
                        </div>
                    }
                </>
        );
    }
}
