import React from "react";
import { RuleEditorContext } from "../../contexts/RuleEditorContext";
import { Grid, GridColumn, GridRow, Icon, Spacing, Tabs } from "gui-elements";
import Loading from "../../../Loading";
import { IPreConfiguredOperators, RuleOperatorList } from "./RuleOperatorList";
import {
    IRuleOperator,
    IRuleSidebarPreConfiguredOperatorsTabConfig,
    IRuleSideBarFilterTabConfig,
} from "../../RuleEditor.typings";
import { extractSearchWords, matchesAllWords } from "gui-elements/src/components/Typography/Highlighter";
import { SidebarSearchField } from "./SidebarSearchField";
import { partitionArray, sortLexically } from "../../../../../utils/basicUtils";
import { TabProps } from "gui-elements/src/components/Tabs/Tabs";
import useErrorHandler from "../../../../../hooks/useErrorHandler";
import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";
import { commonSel } from "@ducks/common";

/** Contains the list of operators that can be dragged and dropped onto the editor canvas and supports filtering. */
export const RuleEditorOperatorSidebar = () => {
    const editorContext = React.useContext(RuleEditorContext);
    const [t] = useTranslation();
    const prefLang = useSelector(commonSel.localeSelector);
    const { registerError } = useErrorHandler();
    const [filteredOperators, setFilteredOperators] = React.useState<IRuleOperator[]>([]);
    // The query that was input in the search field. This won't get immediately active.
    const [textQuery, setTextQuery] = React.useState<string>("");
    const searchWords = extractSearchWords(textQuery);
    const [operatorList, setOperatorList] = React.useState<IRuleOperator[] | undefined>();
    /** Tab handling. */
    // The currently active tab
    const [activeTabId, setActiveTabId] = React.useState<string | undefined>(undefined);
    const activeTab: IRuleSidebarPreConfiguredOperatorsTabConfig | IRuleSideBarFilterTabConfig | undefined = (
        editorContext.tabs ?? []
    ).find((tab) => tab.id === activeTabId);
    /** Pre-configured operators */
    const [preConfiguredOperatorListLoading, setPreConfiguredOperatorListLoading] = React.useState(false);
    //
    const [preConfiguredOperators, setPreconfiguredOperators] = React.useState<
        | (IPreConfiguredOperators<any> & {
              itemSearchText: (listItem: any) => string;
              itemLabel: (listItem: any) => string;
          })
        | undefined
    >(undefined);
    const [filteredPreConfiguredOperators, setFilteredPreConfiguredOperators] = React.useState<
        IPreConfiguredOperators<any> | undefined
    >(undefined);

    // Filter operator list when active query or filters change
    React.useEffect(() => {
        if (preConfiguredOperators && !operatorList) {
            if (searchWords.length > 0) {
                const filteredOps = filterAndSortOperators(
                    preConfiguredOperators.originalOperators,
                    preConfiguredOperators.itemSearchText,
                    preConfiguredOperators.itemLabel,
                    searchWords
                );
                setFilteredPreConfiguredOperators({ ...preConfiguredOperators, originalOperators: filteredOps });
            } else {
                setFilteredPreConfiguredOperators(preConfiguredOperators);
            }
            setFilteredOperators([]);
        } else if (operatorList && !preConfiguredOperators) {
            if (searchWords.length > 0) {
                setFilteredOperators(
                    filterAndSortOperators(operatorList, ruleOperatorSearchText, (op) => op.label, searchWords)
                );
            } else {
                setFilteredOperators(operatorList);
            }
            setFilteredPreConfiguredOperators(undefined);
        }
    }, [textQuery, operatorList, preConfiguredOperators]);

    // Set active tab initially
    React.useEffect(() => {
        if (editorContext.tabs && editorContext.tabs.length > 0) {
            setActiveTabId(editorContext.tabs[0].id);
        } else {
            setActiveTabId(undefined);
        }
    }, [editorContext.tabs]);

    // Handle tab changes
    React.useEffect(() => {
        if (editorContext.operatorList) {
            if (editorContext.tabs && activeTabId) {
                const tabConfig = activeTab;
                if (tabConfig) {
                    if ((tabConfig as IRuleSideBarFilterTabConfig).filterAndSort) {
                        const filterTabConfig = tabConfig as IRuleSideBarFilterTabConfig;
                        setOperatorList(filterTabConfig.filterAndSort(editorContext.operatorList));
                        setPreconfiguredOperators(undefined);
                    } else {
                        const customTabConfig = tabConfig as IRuleSidebarPreConfiguredOperatorsTabConfig;
                        loadExternalOperators(customTabConfig);
                    }
                }
            } else {
                setOperatorList(editorContext.operatorList);
            }
            // Reset text query on tab change
            setTextQuery("");
        }
    }, [editorContext.operatorList, activeTabId]);

    // Load external operators
    const loadExternalOperators = async (config: IRuleSidebarPreConfiguredOperatorsTabConfig) => {
        setPreConfiguredOperatorListLoading(true);
        try {
            const originalOperators = await config.fetchOperators(prefLang);
            setPreconfiguredOperators({
                originalOperators: originalOperators ?? [],
                isOriginalOperator: config.isOriginalOperator,
                toPreConfiguredRuleOperator: config.convertToOperator,
                // At the moment we don't mix pre-configured and "empty" operators, so the position does not matter.
                position: "top",
                itemSearchText: config.itemSearchText,
                itemLabel: config.itemLabel,
                itemId: config.itemId,
            });
            setOperatorList(undefined);
        } catch (ex) {
            registerError(
                "RuleEditorOperatorSidebar.loadExternalOperators",
                t("taskViews.ruleEditor.errors.loadExternalOperators", { tabName: config.label }),
                ex
            );
        } finally {
            setPreConfiguredOperatorListLoading(false);
        }
    };

    const tabs: TabProps[] = (editorContext.tabs ?? []).map((tab) => ({
        id: tab.id,
        titlePrefix: tab.icon ? <Icon name={tab.icon} /> : undefined,
        title: tab.label,
    }));

    return editorContext.operatorListLoading ? (
        <Loading />
    ) : (
        <Grid data-test-id={"rule-editor-sidebar"} verticalStretchable={true} useAbsoluteSpace={true}>
            {tabs.length > 0 && activeTabId ? (
                <GridRow>
                    <GridColumn full>
                        {editorContext.tabs && (
                            <Tabs
                                id={"rule-editor-sidebar-tabs"}
                                tabs={tabs}
                                activeTab={activeTabId}
                                onTabClick={(tabId: string) => {
                                    setActiveTabId(tabId);
                                }}
                            />
                        )}
                    </GridColumn>
                </GridRow>
            ) : null}
            <GridRow>
                <GridColumn full style={{ paddingTop: "3px" }}>
                    <SidebarSearchField onQueryChange={setTextQuery} />
                    <Spacing size={"small"} />
                </GridColumn>
            </GridRow>
            {preConfiguredOperatorListLoading ? (
                <Loading />
            ) : (
                <GridRow verticalStretched={true}>
                    <GridColumn full style={{ paddingTop: "3px" }}>
                        <RuleOperatorList
                            ruleOperatorList={filteredOperators}
                            textQuery={textQuery}
                            preConfiguredOperators={filteredPreConfiguredOperators}
                        />
                    </GridColumn>
                </GridRow>
            )}
        </Grid>
    );
};

const ruleOperatorSearchText = (ruleOperator: IRuleOperator): string => {
    return `${ruleOperator.label} ${(ruleOperator.tags ?? []).join(" ")} ${ruleOperator.description ?? ""} ${(
        ruleOperator.categories ?? []
    ).join(" ")}`.toLowerCase();
};

// Filter the operators by search query and sort them
function filterAndSortOperators<T>(
    operators: T[],
    searchText: (T) => string,
    labelText: (T) => string,
    searchWords: string[]
): T[] {
    const filtered = operators.filter((op) => {
        return matchesAllWords(searchText(op), searchWords);
    });
    const matchCount = new Map<T, number>();
    const { matches, nonMatches } = partitionArray(
        filtered,
        (op) => !!searchWords.find((w) => labelText(op).toLowerCase().includes(w))
    );
    matches.forEach((match) => {
        const label = labelText(match).toLowerCase();
        const labelMatchCount = searchWords.filter((word) => label.includes(word)).length;
        matchCount.set(match, labelMatchCount);
    });
    // Sort label and other matches independently
    const sortedLabelMatches = matches.sort((matchA, matchB) => {
        const matchCountA = matchCount.get(matchA) ?? 0;
        const matchCountB = matchCount.get(matchB) ?? 0;
        if (matchCountA > matchCountB) {
            return -1;
        } else if (matchCountB > matchCountA) {
            return 1;
        } else {
            return labelText(matchA).toLowerCase() < labelText(matchB).toLowerCase() ? -1 : 1;
        }
    });
    const byLabel = (op: T) => labelText(op);
    return [...sortedLabelMatches, ...sortLexically(nonMatches, byLabel)];
}
