import React, { useEffect, useState } from "react";
import { Classes, Intent } from "@wrappers/blueprint/constants";
import Button from "@wrappers/blueprint/button";
import Dialog from "@wrappers/blueprint/dialog";
import InputGroup from "@wrappers/blueprint/input-group";
import { FormGroup } from "@blueprintjs/core";
import FileUploader from "../FileUploader";

export interface ICloneOptions {
    isOpen: boolean;

    onDiscard(): void;

    onConfirm(formData: any): void;
}

export default function CreateProjectModal({isOpen, onDiscard, onConfirm}: ICloneOptions) {
    const [formData, setFormData] = useState({
        title: '',
        description: ''
    });

    const handleInputChange = (key: string, value: string) => {
        setFormData({
            ...formData,
            [key]: value
        });
    };

    const handleCreate = () => {
        onConfirm(formData);
    }

    return (
        <Dialog
            icon="info-sign"
            onClose={onDiscard}
            title={`Create a new artefact: Project`}
            isOpen={isOpen}
        >
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
                        onChange={e => handleInputChange('title', e.target.value)}
                    />
                </FormGroup>
                <FormGroup
                    inline={false}
                    label={"Project Description"}
                    labelFor="desc-input"
                    labelInfo={"(required)"}
                >
                    <InputGroup
                        id="desc-input"
                        placeholder="Project Description"
                        onChange={e => handleInputChange('description', e.target.value)}
                    />
                </FormGroup>
                <FormGroup
                    inline={false}
                    label={"Project Description"}
                    labelFor="desc-input"
                    labelInfo={"(required)"}
                >
                    <FileUploader />
                </FormGroup>
            </div>

            <div className={Classes.DIALOG_FOOTER}>
                <div className={Classes.DIALOG_FOOTER_ACTIONS}>
                    <Button
                        intent={Intent.PRIMARY}
                        onClick={handleCreate}
                    >
                        Create
                    </Button>
                    <Button onClick={onDiscard}>Cancel</Button>
                </div>
            </div>

        </Dialog>
    )
}
