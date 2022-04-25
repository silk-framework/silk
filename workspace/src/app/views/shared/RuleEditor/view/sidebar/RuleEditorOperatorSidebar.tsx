import React from "react";
import { RuleEditorContext } from "../../contexts/RuleEditorContext";
import { Grid, GridColumn, GridRow, Icon, Spacing, Tabs, TabTitle } from "@eccenca/gui-elements";
import Loading from "../../../Loading";
import { IPreConfiguredOperators, RuleOperatorList } from "./RuleOperatorList";
import {
    IRuleOperator,
    IRuleSideBarFilterTabConfig,
    IRuleSidebarPreConfiguredOperatorsTabConfig,
} from "../../RuleEditor.typings";
import { extractSearchWords, matchesAllWords } from "@eccenca/gui-elements/src/components/Typography/Highlighter";
import { SidebarSearchField } from "./SidebarSearchField";
import { partitionArray, sortLexically } from "../../../../../utils/basicUtils";
import { TabProps } from "@eccenca/gui-elements/src/components/Tabs/Tab";
import { colors as tabColors } from "@eccenca/gui-elements/src/cmem/react-flow/configuration/linking";
import useErrorHandler from "../../../../../hooks/useErrorHandler";
import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";
import { commonSel } from "@ducks/common";
import { ISuggestionWithReplacementInfo } from "@eccenca/gui-elements/src/components/AutoSuggestion/AutoSuggestion";

/** Contains the list of operators that can be dragged and dropped onto the editor canvas and supports filtering. */
export const RuleEditorOperatorSidebar = () => {
    const editorContext = React.useContext(RuleEditorContext);
    const [t] = useTranslation();
    const prefLang = useSelector(commonSel.localeSelector);
    const { registerError } = useErrorHandler();
    const [filteredOperators, setFilteredOperators] = React.useState<IRuleOperator[]>([]);
    // The query that was input in the search field. This won't get immediately active.
    const [textQuery, setTextQuery] = React.useState<string>("");
    const [operatorList, setOperatorList] = React.useState<IRuleOperator[] | undefined>();
    const [operatorCategories] = React.useState<ISuggestionWithReplacementInfo[]>([]);
    /** Tab handling. */
    // The currently active tab
    const [activeTabId, setActiveTabId] = React.useState<string | undefined>(undefined);
    const activeTab: IRuleSidebarPreConfiguredOperatorsTabConfig | IRuleSideBarFilterTabConfig | undefined = (
        editorContext.tabs ?? []
    ).find((tab) => tab.id === activeTabId);
    /** Pre-configured operators */
    const [preConfiguredOperatorListLoading, setPreConfiguredOperatorListLoading] = React.useState(false);
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
        const searchWords = extractSearchWords(textQuery);
        if (preConfiguredOperators && !operatorList) {
            if (searchWords.length > 0) {
                const filteredOps = filterAndSortOperators(
                    preConfiguredOperators.originalOperators,
                    preConfiguredOperators.itemSearchText,
                    preConfiguredOperators.itemLabel,
                    () => [],
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
                    filterAndSortOperators<IRuleOperator>(
                        operatorList,
                        ruleOperatorSearchText,
                        (op) => op.label,
                        (op) => op.categories ?? [],
                        searchWords
                    )
                );
            } else {
                // Rank "Recommended" operators to the top by default in unfiltered list
                const { matches: recommended, nonMatches: others } = partitionArray<IRuleOperator>(operatorList, (op) =>
                    (op.categories ?? []).includes("Recommended")
                );
                setFilteredOperators([...recommended, ...others]);
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

    const extractOperatorCategories = (operatorList: IRuleOperator[]) => {
        const categories = new Set<string>();
        if (operatorList) {
            operatorList.forEach((op) => (op.categories ?? []).forEach((cat) => categories.add(cat)));
        }
        const categoryArray = [...categories.values()];
        sortLexically(categoryArray, (elem) => elem);
        operatorCategories.splice(0, operatorCategories.length);
        operatorCategories.push(...categoryArray.map((c) => ({ value: c, query: "", from: 0, length: 0 })));
    };

    // Handle tab changes
    React.useEffect(() => {
        if (editorContext.operatorList) {
            if (editorContext.tabs && activeTabId) {
                const tabConfig = activeTab;
                if (tabConfig) {
                    if ((tabConfig as IRuleSideBarFilterTabConfig).filterAndSort) {
                        const filterTabConfig = tabConfig as IRuleSideBarFilterTabConfig;
                        const filteredOperators = filterTabConfig.filterAndSort(editorContext.operatorList);
                        setOperatorList(filteredOperators);
                        extractOperatorCategories(filteredOperators);
                        setPreconfiguredOperators(undefined);
                    } else {
                        const customTabConfig = tabConfig as IRuleSidebarPreConfiguredOperatorsTabConfig;
                        loadExternalOperators(customTabConfig);
                    }
                }
            } else {
                setOperatorList(editorContext.operatorList);
                extractOperatorCategories(editorContext.operatorList);
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
            operatorCategories.splice(0, operatorCategories.length);
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

    const getTabColor = (id: string): string | undefined => {
        switch (id) {
            case "sourcePaths":
                return tabColors.sourcepathNodeBright;
            case "targetPaths":
                return tabColors.targetpathNodeBright;
            case "comparison":
                return tabColors.comparatorNodeBright;
            case "transform":
                return tabColors.transformationNodeBright;
            case "aggregation":
                return tabColors.aggregatorNodeBright;
            default:
                return undefined;
        }
    };
    const tabs: TabProps[] = (editorContext.tabs ?? []).map((tab) => ({
        id: tab.id,
        title: (
            <TabTitle
                text={tab.icon ? null : tab.label}
                titlePrefix={tab.icon ? <Icon name={tab.icon} /> : undefined}
                tooltip={tab.icon ? tab.label : undefined}
            />
        ),
        dontShrink: tab.icon ? true : false,
        backgroundColor: getTabColor(tab.id),
    }));

    const fetchCategories = () => operatorCategories;

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
                                selectedTabId={activeTabId}
                                onChange={(tabId: string) => {
                                    setActiveTabId(tabId);
                                }}
                            />
                        )}
                    </GridColumn>
                </GridRow>
            ) : null}
            <GridRow>
                <GridColumn full style={{ paddingTop: "3px" }}>
                    <SidebarSearchField
                        activeTabId={activeTabId}
                        onQueryChange={setTextQuery}
                        searchSuggestions={fetchCategories}
                    />
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

/** Filter the operators by search query and sort them
 *
 * @param operators The operators to sort
 * @param searchText The search text to find matches in.
 * @param labelText Returns the label of the operator. Label-matches are ranked to the top.
 * @param categories Returns the categories of the operator. Exact category matches are ranked below label matches.
 * @param searchWords The search words of the user query.
 */
function filterAndSortOperators<T>(
    operators: T[],
    searchText: (op: T) => string,
    labelText: (op: T) => string,
    categories: (op: T) => string[],
    searchWords: string[]
): T[] {
    const filteredOperators = operators.filter((op) => {
        return matchesAllWords(searchText(op), searchWords);
    });
    const matchCount = new Map<T, number>();
    const { matches: labelMatches, nonMatches: nonLabelMatches } = partitionArray(
        filteredOperators,
        (op) => !!searchWords.find((w) => labelText(op).toLowerCase().includes(w))
    );
    const { matches: categoryMatches, nonMatches: nonCategoryMatches } = partitionArray(
        nonLabelMatches,
        (op) => !!searchWords.find((w) => categories(op).findIndex((v) => v.toLowerCase() === w) > -1)
    );
    labelMatches.forEach((match) => {
        const label = labelText(match).toLowerCase();
        const labelMatchCount = searchWords.filter((word) => label.includes(word)).length;
        matchCount.set(match, labelMatchCount);
    });
    // Sort label and other matches independently
    const sortedLabelMatches = labelMatches.sort((matchA, matchB) => {
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
    return [
        ...sortedLabelMatches,
        ...sortLexically(categoryMatches, byLabel),
        ...sortLexically(nonCategoryMatches, byLabel),
    ];
}
