import React from "react";
import InputGroup from "@wrappers/blueprint/input-group";
import { FormGroup } from "@blueprintjs/core";
import TextArea from "@wrappers/blueprint/textarea";
import FileUploader from "../../../FileUploader";

export interface IProps {
    form: any;
}

export function ProjectForm({form}: IProps) {
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
                inputRef={form.register({required: true})}
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
                inputRef={form.register()}
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
