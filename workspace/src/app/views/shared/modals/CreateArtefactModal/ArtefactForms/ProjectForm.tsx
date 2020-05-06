import React from "react";
import { FieldItem, TextField, TextArea } from "@wrappers/index";
import FileUploader from "../../../FileUploader";

export interface IProps {
    form: any;
}

export function ProjectForm({ form }: IProps) {
    return (
        <>
            <FieldItem
                labelAttributes={{
                    text: "Title",
                    info: "required",
                    htmlFor: "title-input",
                }}
            >
                <TextField
                    id="title-input"
                    placeholder="Project title"
                    name={"label"}
                    inputRef={form.register({ required: true })}
                />
            </FieldItem>
            <FieldItem
                labelAttributes={{
                    text: "Description",
                    htmlFor: "desc-input",
                }}
            >
                <TextArea
                    id="desc-input"
                    name={"description"}
                    growVertically={true}
                    placeholder="Project description"
                    inputRef={form.register()}
                />
            </FieldItem>
            <FieldItem
                labelAttributes={{
                    text: "Restore data from backup",
                }}
                helperText={
                    "In case you want to restore project data you can attach the backup file that has been exported before."
                }
            >
                <FileUploader />
            </FieldItem>
        </>
    );
}
