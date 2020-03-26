import React, { useState } from "react";
import { Classes, Intent } from "@wrappers/blueprint/constants";
import Dialog from "@wrappers/blueprint/dialog";
import InputGroup from "@wrappers/blueprint/input-group";
import { FormGroup } from "@blueprintjs/core";
import TextArea from "@wrappers/blueprint/textarea";
import Loading from "../../../Loading";
import FileUploader from "../../../FileUploader";

interface IFormData {
    label: string;
    description: string;
}

export interface IProps {
    onChange(formData: IFormData): void;
}

export function ProjectForm({onChange}: IProps) {
    const [formData, setFormData] = useState<IFormData>({
        label: '',
        description: ''
    });

    const handleInputChange = (e: any) => {
        const updated = {
            ...formData,
            [e.target.name]: e.target.value
        };
        setFormData(updated);
        onChange(updated);
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
                    name={'label'}
                    onChange={handleInputChange}
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
                    name={'description'}
                    growVertically={true}
                    placeholder="Project Description"
                    onChange={handleInputChange}
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
