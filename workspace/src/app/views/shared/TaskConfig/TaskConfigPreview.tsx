import { IProjectTask } from "@ducks/shared/typings";
import { FieldItem } from "@wrappers/index";
import React from "react";
import { IArtefactItemProperty, IDetailedArtefactItem } from "@ducks/common/typings";

interface IProps {
    taskData: IProjectTask;
    taskDescription: IDetailedArtefactItem;
}

export function TaskConfigPreview({ taskData, taskDescription }: IProps) {
    if (!taskData) {
        return <p>No preview available</p>;
    }

    // Generates a flat object of (nested) parameter labels and their display values, i.e. their label if it exists
    const taskValues = (taskData: any): Record<string, string> => {
        if (taskData) {
            const result: Record<string, string> = {};
            const taskValuesRec = (
                obj: object,
                prefix: string,
                paramDescriptions: Record<string, IArtefactItemProperty>
            ) => {
                Object.entries(obj)
                    .filter(([key, v]) => {
                        return paramDescriptions[key] && paramDescriptions[key].visibleInDialog;
                    })
                    .forEach(([paramName, paramValue]) => {
                        const value = paramDisplayValue(paramValue);
                        if (typeof value === "object" && value !== null) {
                            taskValuesRec(
                                value,
                                paramDescriptions[paramName].title + ": ",
                                paramDescriptions[paramName].properties
                            );
                        } else {
                            result[prefix + paramDescriptions[paramName].title] = value;
                        }
                    });
            };
            taskValuesRec(taskData, "", taskDescription.properties);
            return result;
        } else {
            return {};
        }
    };

    /** Returns the string value if this is an atomic value, else it returns the parameter value object. */
    const paramDisplayValue = (parameterValue: any): string | any => {
        if (typeof parameterValue === "string") {
            return parameterValue;
        } else if (typeof parameterValue.label === "string") {
            return parameterValue.label;
        } else if (typeof parameterValue.value === "string") {
            return parameterValue.value;
        } else if (parameterValue.value) {
            return parameterValue.value;
        } else {
            return parameterValue;
        }
    };
    return (
        <form>
            <>
                {Object.entries(taskValues(taskData.data.parameters))
                    // Only non-empty parameter values are shown
                    .filter(([paramId, value]) => value.trim() !== "")
                    .map(([paramId, value]) => {
                        return (
                            <FieldItem
                                key={paramId}
                                labelAttributes={{
                                    text: paramId,
                                    htmlFor: paramId,
                                }}
                            >
                                {value}
                            </FieldItem>
                        );
                    })}
            </>
        </form>
    );
}
