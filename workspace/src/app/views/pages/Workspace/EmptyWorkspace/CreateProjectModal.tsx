import React, { useState } from "react";
import { Classes } from "@wrappers/blueprint/constants";
import Dialog from "../../../../wrappers/blueprint/dialog";
import InputGroup from "../../../../wrappers/blueprint/input-group";
import { FormGroup } from "@blueprintjs/core";
import TextArea from "../../../../wrappers/blueprint/textarea";
import Loading from "../../../components/Loading";
import FileUploader from "../../../components/FileUploader";
import { workspaceOp } from "@ducks/workspace";
import { useDispatch } from "react-redux";
import { Button } from '@wrappers/index';

interface IFormData {
    label: string;
    description: string;
}

export interface ICloneOptions {
    isOpen: boolean;

    onDiscard(): void;

    onConfirm(formData: IFormData): void;
}

export default function CreateProjectModal({isOpen, onDiscard, onConfirm}: ICloneOptions) {
    const dispatch = useDispatch();

    const [loading, setLoading] = useState<boolean>(false);
    const [formData, setFormData] = useState<IFormData>({
        label: '',
        description: ''
    });

    const handleInputChange = (key: string, value: string) => {
        setFormData({
            ...formData,
            [key]: value
        });
    };

    const handleCreate = async () => {
        setLoading(true);
        try {
            dispatch(workspaceOp.fetchCreateProjectAsync(formData.label, formData.description));
            onConfirm(formData);
        } finally {
            setLoading(false);
        }
    };

    return (
        <Dialog
            icon="info-sign"
            onClose={onDiscard}
            title={`Create a new artefact: Project`}
            isOpen={isOpen}
        >
            {
                loading ? <Loading/> : <>
                    <div className={Classes.DIALOG_BODY}>
                        <FormGroup
                            inline={false}
                            label={"Project Title"}
                            labelFor="title-input"
                            labelInfo={"(required)"}
                        >
                            <InputGroup
                                id="title-input"
                                placeholder="Project Title"
                                onChange={e => handleInputChange('label', e.target.value)}
                                value={formData.label}
                            />
                        </FormGroup>
                        <FormGroup
                            inline={false}
                            label={"Project Description"}
                            labelFor="desc-input"
                        >
                            <TextArea
                                style={{'width': '100%'}}
                                id="desc-input"
                                growVertically={true}
                                placeholder="Project Description"
                                onChange={e => handleInputChange('description', e.target.value)}
                                value={formData.description}
                            />
                        </FormGroup>
                        <FormGroup
                            inline={false}
                            label={"Import Project"}
                        >
                            <FileUploader/>
                        </FormGroup>
                    </div>
                    <div className={Classes.DIALOG_FOOTER}>
                        <div className={Classes.DIALOG_FOOTER_ACTIONS}>
                            <Button onClick={onDiscard}>Cancel</Button>
                            <Button affirmative onClick={handleCreate}>Create</Button>
                        </div>
                    </div>
                </>
            }
        </Dialog>
    )
}
