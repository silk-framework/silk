import React, { useState } from "react";
import { Classes, Intent } from "@wrappers/blueprint/constants";
import Dialog from "@wrappers/blueprint/dialog";
import InputGroup from "@wrappers/blueprint/input-group";
import { FormGroup } from "@blueprintjs/core";
import TextArea from "@wrappers/blueprint/textarea";
import Loading from "../../../Loading";
import FileUploader from "../../../FileUploader";
import { workspaceOp } from "@ducks/workspace";
import { useDispatch } from "react-redux";
import { Button } from '@wrappers/index';

interface IFormData {
    label: string;
    description: string;
}

export interface IProps {
    onConfirm(formData: IFormData): void;
}

export function ProjectForm({onConfirm}: IProps) {
    const dispatch = useDispatch();

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
        try {
            dispatch(workspaceOp.fetchCreateProjectAsync(formData.label, formData.description));
            onConfirm(formData);
        } finally {
        }
    };

    return <>
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
        </>
}
