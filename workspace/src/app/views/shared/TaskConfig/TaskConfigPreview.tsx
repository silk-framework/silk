import { IProjectTask } from "@ducks/shared/typings";
import {
    Icon,
    IconButton,
    Notification,
    OverviewItemList,
    PropertyName,
    PropertyValue,
    PropertyValuePair,
    Toolbar,
    ToolbarSection,
} from "@eccenca/gui-elements";
import React from "react";
import { IArtefactItemProperty, IPluginDetails } from "@ducks/common/typings";
import { useTranslation } from "react-i18next";
import { INPUT_TYPES } from "../../../constants";
import { CONTEXT_PATH } from "../../../constants/path";
import { objectToFlatRecord } from "../../../utils/transformers";

interface IProps {
    taskData: IProjectTask;
    taskDescription: IPluginDetails;
}

export interface ParameterConfigValue {
    value: string;
    templateValue?: string;
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
    const taskValues = (parameters: any): Record<string, ParameterConfigValue> => {
        if (parameters) {
            const templates = objectToFlatRecord(taskData.data.templates ?? {}, {}, false);
            const result: Record<string, ParameterConfigValue> = Object.create(null);
            // Recursively extracts (nested) parameter display values.
            const taskValuesRec = (
                obj: object,
                labelPrefix: string,
                parameterIdPrefix: string,
                paramDescriptions: Record<string, IArtefactItemProperty>
            ) => {
                Object.entries(obj)
                    .filter(([key]) => {
                        const pd = paramDescriptions[key];
                        // It is possible that a new version of the task plugin has changed or removed parameters. Ignore parameters without parameter spec.
                        if (!pd) {
                            return false;
                        } else {
                            const passwordParameter = pd.parameterType === INPUT_TYPES.PASSWORD;
                            return pd && pd.visibleInDialog && !pd.advanced && !passwordParameter;
                        }
                    })
                    .forEach(([paramName, paramValue]) => {
                        const value = paramDisplayValue(paramValue);
                        const propertyTitle = t(
                            "widget.ConfigWidget.properties." + paramDescriptions[paramName].title,
                            paramDescriptions[paramName].title
                        );
                        if (typeof value === "object" && value !== null) {
                            taskValuesRec(
                                value,
                                propertyTitle + ": ",
                                `${paramName}.`,
                                paramDescriptions[paramName].properties as Record<string, IArtefactItemProperty>
                            );
                        } else {
                            result[labelPrefix + propertyTitle] = {
                                value,
                                templateValue: templates[parameterIdPrefix + paramName],
                            };
                        }
                    });
            };
            taskValuesRec(parameters, "", "", taskDescription.properties);
            return result;
        } else {
            return {};
        }
    };

    /** Returns the string value if this is an atomic value, else it returns the parameter value object. */
    const paramDisplayValue = (parameterValue: any): string | any => {
        if (typeof parameterValue === "string") {
            return t("widget.ConfigWidget.values." + parameterValue, parameterValue);
        } else if (typeof parameterValue.label === "string") {
            return t("widget.ConfigWidget.values." + parameterValue.label, parameterValue.label);
        } else if (typeof parameterValue.value === "string") {
            return t("widget.ConfigWidget.values." + parameterValue.value, parameterValue.value);
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
    const taskParameterValues: Record<string, ParameterConfigValue> = taskValues(taskData.data.parameters);
    if (taskDescription.taskType === "Dataset") {
        if (taskData.data.readOnly === true) {
            taskParameterValues[t("CreateModal.ReadOnlyParameter.label")] = { value: "true" };
        }
        if (taskData.data.uriProperty) {
            taskParameterValues[t("DatasetUriPropertyParameter.label")] = { value: taskData.data.uriProperty };
        }
    }

    const taskResourceParameterType = Object.values(taskDescription.properties).reduce((obj, property) => {
        obj[property.title] = {
            type: property.parameterType,
        };
        return obj;
    }, {});

    return (
        <OverviewItemList hasDivider>
            {Object.entries(taskParameterValues)
                // Only non-empty parameter values are shown
                .filter(([paramId, { value }]) => value.trim() !== "")
                .map(([paramId, { value, templateValue }]) => {
                    return (
                        <Toolbar data-test-id={`task-config-preview-parameter-${paramId}`} noWrap key={paramId}>
                            <ToolbarSection canGrow canShrink>
                                <PropertyValuePair hasDivider nowrap>
                                    <PropertyName
                                        title={paramId}
                                        size="large"
                                        labelProps={{
                                            style: fixStyle,
                                        }}
                                    >
                                        {paramId}
                                    </PropertyName>
                                    <PropertyValue>
                                        <code title={value.length > 30 ? value : undefined} style={fixStyle}>
                                            {value}
                                        </code>
                                    </PropertyValue>
                                </PropertyValuePair>
                            </ToolbarSection>
                            <ToolbarSection style={{ minWidth: "50px", justifyContent: "right" }}>
                                {taskResourceParameterType[paramId]?.type === "resource" && (
                                    <IconButton
                                        data-test-id={"resource-download-btn"}
                                        name="item-download"
                                        text={t("common.action.download")}
                                        small
                                        href={`${CONTEXT_PATH}/workspace/projects/${
                                            taskData.project
                                        }/files?path=${encodeURIComponent(value)}`}
                                    />
                                )}
                                {templateValue != null && (
                                    <Icon
                                        data-test-id={"template-tooltip-icon"}
                                        name={"form-template"}
                                        intent={"info"}
                                        tooltipText={
                                            t("widget.TaskConfigWidget.templateValueInfo") +
                                            `\n\n\`\`\`${templateValue}\`\`\``
                                        }
                                        tooltipProps={{ placement: "top", markdownEnabler: "```" }}
                                    />
                                )}
                            </ToolbarSection>
                        </Toolbar>
                    );
                })}
        </OverviewItemList>
    );
}
