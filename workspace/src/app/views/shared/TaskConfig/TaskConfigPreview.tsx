import {IProjectTask} from "@ducks/shared/typings";
import {
    Notification,
    OverflowText,
    PropertyName,
    PropertyValue,
    PropertyValueList,
    PropertyValuePair,
} from "@eccenca/gui-elements";
import React from "react";
import {IArtefactItemProperty, IPluginDetails} from "@ducks/common/typings";
import {useTranslation} from "react-i18next";
import {INPUT_TYPES} from "../../../constants";

interface IProps {
    taskData: IProjectTask;
    taskDescription: IPluginDetails;
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
        return <Notification>{t("widget.TaskConfigWidget.noPreview", "No preview available")}</Notification>;
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
                    .filter(([key]) => {
                        const pd = paramDescriptions[key];
                        const passwordParameter = pd.parameterType === INPUT_TYPES.PASSWORD;
                        return pd && pd.visibleInDialog && !pd.advanced && !passwordParameter;
                    })
                    .forEach(([paramName, paramValue]) => {
                        const value = paramDisplayValue(paramValue);
                        const propertyTitle = t("widget.ConfigWidget.properties."+paramDescriptions[paramName].title, paramDescriptions[paramName].title);
                        if (typeof value === "object" && value !== null) {
                            taskValuesRec(
                                value,
                                propertyTitle + ": ",
                                paramDescriptions[paramName].properties as Record<string, IArtefactItemProperty>
                            );
                        } else {
                            result[labelPrefix + propertyTitle] = value;
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
            return t("widget.ConfigWidget.values."+parameterValue, parameterValue);
        } else if (typeof parameterValue.label === "string") {
            return t("widget.ConfigWidget.values."+parameterValue.label, parameterValue.label);
        } else if (typeof parameterValue.value === "string") {
            return t("widget.ConfigWidget.values."+parameterValue.value, parameterValue.value);
        } else if (parameterValue.value) {
            // withLabels "object" value
            return parameterValue.value;
        } else {
            // non-labelled "object" value
            return parameterValue;
        }
    };
    // Because of line_height: 1, underscores are not rendered
    const fixStyle = { lineHeight: "normal" };
    const taskParameterValues: Record<string, string> = taskValues(taskData.data.parameters)
    if(taskDescription.taskType === "Dataset" && taskData.data.uriProperty) {
        taskParameterValues[t("DatasetUriPropertyParameter.label")] = taskData.data.uriProperty
    }
    return (
        <OverflowText passDown>
            <PropertyValueList>
                {Object.entries(taskParameterValues)
                    // Only non-empty parameter values are shown
                    .filter(([paramId, value]) => value.trim() !== "")
                    .map(([paramId, value]) => {
                        return (
                            <PropertyValuePair hasDivider key={paramId}>
                                <PropertyName title={paramId}>{paramId}</PropertyName>
                                <PropertyValue>
                                    <code style={fixStyle}>{value}</code>
                                </PropertyValue>
                            </PropertyValuePair>
                        );
                    })}
            </PropertyValueList>
        </OverflowText>
    );
}
