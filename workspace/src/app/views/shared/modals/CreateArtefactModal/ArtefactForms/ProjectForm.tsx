import React from "react";
import { FormGroup } from "@blueprintjs/core";
import {
    TextField,
    TextArea
} from "@wrappers/index";
import FileUploader from "../../../FileUploader";

export interface IProps {
    form: any;
}

export function ProjectForm({form}: IProps) {
    return <>
        <FormGroup
            inline={false}
            label={"Title"}
            labelFor="title-input"
            labelInfo={"(required)"}
        >
            <TextField
                id="title-input"
                placeholder="Project title"
                name={'label'}
                inputRef={form.register({required: true})}
            />
        </FormGroup>
        <FormGroup
            inline={false}
            label={"Description"}
            labelFor="desc-input"
        >
            <TextArea
                style={{'width': '100%'}}
                id="desc-input"
                name={'description'}
                growVertically={true}
                placeholder="Project description"
                inputRef={form.register()}
            />
        </FormGroup>
        <FormGroup
            inline={false}
            label={"Import project backup file"}
        >
            <FileUploader/>
        </FormGroup>
    </>
}
