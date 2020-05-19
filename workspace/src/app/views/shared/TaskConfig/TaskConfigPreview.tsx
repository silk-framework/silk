import { IProjectTask } from "@ducks/shared/typings";
import { FieldItem } from "@wrappers/index";
import React from "react";

interface IProps {
    taskData: IProjectTask;
}

export function TaskConfigPreview({ taskData }: IProps) {
    if (!taskData) {
        return <p>No preview available</p>;
    }
    const paramValueToString = (parameterValue: any): string => {
        if (typeof parameterValue === "string") {
            return parameterValue;
        } else if (typeof parameterValue.label === "string" && parameterValue.label) {
            return parameterValue.label;
        } else if (typeof parameterValue.value === "string" && parameterValue.value) {
            return parameterValue.value;
        } else {
            return "";
        }
    };
    return (
        <form>
            <>
                {Object.entries(taskData.data.parameters)
                    // Only non-empty parameter values are shown
                    .filter(([paramId, parameter]) => paramValueToString(parameter).trim() !== "")
                    .map(([paramId, parameter]) => {
                        if (parameter !== "" || (parameter !== null && !!(parameter as any).value)) {
                            return (
                                <FieldItem
                                    key={paramId}
                                    labelAttributes={{
                                        text: paramId,
                                        htmlFor: paramId,
                                    }}
                                >
                                    {paramValueToString(parameter)}
                                </FieldItem>
                            );
                        } else {
                            return null;
                        }
                    })}
            </>
        </form>
    );
}
