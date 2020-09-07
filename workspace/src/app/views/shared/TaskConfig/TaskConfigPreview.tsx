import { IProjectTask } from "@ducks/shared/typings";
import { OverflowText, PropertyValueList, PropertyValuePair, PropertyName, PropertyValue } from "@gui-elements/index";
import React from "react";
import { IArtefactItemProperty, IDetailedArtefactItem } from "@ducks/common/typings";
import { useTranslation } from "react-i18next";

interface IProps {
    taskData: IProjectTask;
    taskDescription: IDetailedArtefactItem;
}

/**
 * Shows a preview of the config data.
 * Only lists parameters that are visible in dialogs, are not marked as 'advanced' and have a non-empty value.
 * @param taskData        The data value of the task.
 * @param taskDescription The schema and description of the task type.
 */
export function TaskConfigPreview({ taskData, taskDescription }: IProps) {
    const [t] = useTranslation();
    if (!taskData) {
        return <p>{t("widget.TaskConfigWidget.noPreview", "No preview available")}</p>;
    }

    // Generates a flat object of (nested) parameter labels and their display values, i.e. their label if it exists
    const taskValues = (taskData: any): Record<string, string> => {
        if (taskData) {
            const result: Record<string, string> = {};
            // Recursively extracts (nested) parameter display values.
            const taskValuesRec = (
                obj: object,
                labelPrefix: string,
                paramDescriptions: Record<string, IArtefactItemProperty>
            ) => {
                Object.entries(obj)
                    .filter(([key, v]) => {
                        const pd = paramDescriptions[key];
                        return pd && pd.visibleInDialog && !pd.advanced;
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
                            result[labelPrefix + paramDescriptions[paramName].title] = value;
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
            // withLabels "object" value
            return parameterValue.value;
        } else {
            // non-labelled "object" value
            return parameterValue;
        }
    };
    // TODO: only return list when it has items, so we need calculate content before rendering
    return (
        <OverflowText passDown>
            <PropertyValueList>
                {Object.entries(taskValues(taskData.data.parameters))
                    // Only non-empty parameter values are shown
                    .filter(([paramId, value]) => value.trim() !== "")
                    .map(([paramId, value]) => {
                        return (
                            <PropertyValuePair hasDivider key={paramId}>
                                <PropertyName>{paramId}</PropertyName>
                                <PropertyValue>
                                    <code>{value}</code>
                                </PropertyValue>
                            </PropertyValuePair>
                        );
                    })}
            </PropertyValueList>
        </OverflowText>
    );
}
