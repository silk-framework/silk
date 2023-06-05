import { Loading } from "../../../shared/Loading/Loading";
import React, { useEffect } from "react";
import {
    Card,
    CardContent,
    CardHeader,
    CardOptions,
    CardTitle,
    Divider,
    IconButton,
    Label,
    OverflowText,
    PropertyName,
    PropertyValue,
    PropertyValueList,
    PropertyValuePair,
} from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";
import { LinkageRuleConfigModal } from "./LinkageRuleConfigModal";
import useErrorHandler from "../../../../hooks/useErrorHandler";
import { fetchLinkSpec, updateLinkageRule } from "../../../taskViews/linking/LinkingRuleEditor.requests";
import { ILinkingRule, LabelledParameterValue } from "../../../taskViews/linking/linking.types";
import { invalidUriChars } from "../../Project/ProjectNamespacePrefixManagementWidget/PrefixNew";
import { IAutocompleteDefaultResponse } from "@ducks/shared/typings";
import { requestSearchForGlobalVocabularyProperties } from "@ducks/workspace/requests";
import { FetchResponse } from "../../../../services/fetch/responseInterceptor";

interface IProps {
    linkingTaskId: string;
    projectId: string;
}

export interface LinkageRuleConfigItem {
    /** ID of the parameter. */
    id: string;
    /** Label of the parameter. */
    label: string;
    /** Description of the parameter. */
    description: string;
    /** Value of the parameter. */
    value: string | undefined;
    /** */
    valueLabel?: string;
    /** Either validates or an error message is given. */
    validation: (value: string) => true | string;
    /** placeholder for empty values. */
    placeholder?: string;
    /** Auto-complete search function. The parameter will be rendered as auto-complete field. */
    onSearch?: (textQuery: string, limit: number) => Promise<FetchResponse<IAutocompleteDefaultResponse[]>>;
}

export const LinkageRuleConfig = ({ linkingTaskId, projectId }: IProps) => {
    const [loading, setLoading] = React.useState(false);
    const [parameters, setParameters] = React.useState<LinkageRuleConfigItem[] | undefined>(undefined);
    const [showModal, setShowModal] = React.useState(false);
    const { registerError } = useErrorHandler();
    const [t] = useTranslation();

    useEffect(() => {
        init();
    }, [projectId, linkingTaskId]);

    const fetchLinkingRule = async (catchError: boolean = true): Promise<ILinkingRule | undefined> => {
        try {
            const linkingTask = await fetchLinkSpec(projectId, linkingTaskId, false);
            const rule = linkingTask.data.parameters.rule;
            const linkingRule: ILinkingRule =
                (rule as ILinkingRule).linkType != null
                    ? (rule as ILinkingRule)
                    : (rule as LabelledParameterValue<ILinkingRule>).value;
            return linkingRule;
        } catch (ex) {
            if (catchError) {
                registerError(
                    "LinkageRuleConfig-fetch-linking-rule",
                    t("widget.LinkingRuleConfigWidget.fetchError"),
                    ex
                );
            } else {
                throw ex;
            }
        }
    };

    const init = async () => {
        setLoading(true);
        const linkingRule = await fetchLinkingRule();
        setLoading(false);
        const unlimited = t("common.words.unlimited");
        if (linkingRule) {
            setParameters([
                {
                    id: "linkType",
                    label: t("widget.LinkingRuleConfigWidget.parameters.linkType.label"),
                    value: linkingRule.linkType,
                    description: t("widget.LinkingRuleConfigWidget.parameters.linkType.description"),
                    validation: (value) => {
                        const invalidCharMatches = value.match(invalidUriChars);
                        if (invalidCharMatches && invalidCharMatches.index != null) {
                            const invalidChar = value.substring(invalidCharMatches.index, invalidCharMatches.index + 1);
                            return `Invalid character found in string: '${invalidChar}'`;
                        }
                        return true;
                    },
                    onSearch: (q: string, l: number) => requestSearchForGlobalVocabularyProperties(q, l, projectId),
                },
                {
                    id: "limit",
                    label: t("widget.LinkingRuleConfigWidget.parameters.limit.label"),
                    value: linkingRule.filter.limit != null ? `${linkingRule.filter.limit}` : "",
                    description: t("widget.LinkingRuleConfigWidget.parameters.limit.description"),
                    valueLabel: linkingRule.filter.limit != null ? `${linkingRule.filter.limit}` : unlimited,
                    validation: (value) => {
                        const notAnInteger = t("form.validations.integer");
                        if (!value || value.trim() === "") {
                            // No limit set
                            return true;
                        } else {
                            const int = Number(value);
                            return Number.isNaN(int) || !Number.isInteger(int) ? notAnInteger : true;
                        }
                    },
                    placeholder: unlimited,
                },
            ]);
        }
    };

    const saveConfig = async (parameters: [string, string | undefined][]) => {
        try {
            const linkingRule = await fetchLinkingRule(false);
            if (linkingRule) {
                const paramValue = (parameterId: string): string | undefined => {
                    const param = parameters.find(([paramId]) => paramId === parameterId);
                    return param ? param[1] : undefined;
                };
                const linkType = paramValue("linkType");
                if (linkType != null) {
                    linkingRule.linkType = linkType;
                }
                const limit = paramValue("limit");
                const limitNr = Number(limit);
                if (limit != null && limit.trim() !== "" && Number.isInteger(limitNr)) {
                    linkingRule.filter.limit = limitNr;
                } else {
                    linkingRule.filter.limit = undefined;
                }
                await updateLinkageRule(projectId, linkingTaskId, linkingRule);
                setShowModal(false);
                init();
            }
        } catch (ex) {
            registerError("LinkageRuleConfig-save-config", t("widget.LinkingRuleConfigWidget.saveError"), ex);
        }
    };

    // Because of line_height: 1, underscores are not rendered
    const fixStyle = { lineHeight: "normal" };

    return (
        <Card data-test-id={"linkageRuleConfigWidget"}>
            <CardHeader>
                <CardTitle>
                    <h3>{t("widget.LinkingRuleConfigWidget.title")}</h3>
                </CardTitle>
                <CardOptions>
                    <IconButton
                        data-test-id="linkage-rule-config-edit-btn"
                        name={"item-edit"}
                        text={t("common.action.configure", "Configure")}
                        onClick={() => setShowModal(true)}
                    />
                </CardOptions>
            </CardHeader>
            <Divider />
            <CardContent>
                {loading || !parameters ? (
                    <Loading />
                ) : (
                    <OverflowText passDown>
                        <PropertyValueList>
                            {parameters.map((paramConfig) => {
                                return (
                                    <PropertyValuePair hasDivider key={paramConfig.id}>
                                        <PropertyName title={paramConfig.label}>
                                            <Label
                                                isLayoutForElement="span"
                                                text={<OverflowText inline>{paramConfig.label}</OverflowText>}
                                                style={fixStyle}
                                            />
                                        </PropertyName>
                                        <PropertyValue>
                                            <code style={fixStyle}>{paramConfig.valueLabel ?? paramConfig.value}</code>
                                        </PropertyValue>
                                    </PropertyValuePair>
                                );
                            })}
                        </PropertyValueList>
                    </OverflowText>
                )}
            </CardContent>
            {showModal && parameters ? (
                <LinkageRuleConfigModal
                    parameters={parameters}
                    submit={saveConfig}
                    onClose={() => setShowModal(false)}
                />
            ) : null}
        </Card>
    );
};
