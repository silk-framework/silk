import React from "react";
import { RuleEditorContext } from "../../contexts/RuleEditorContext";
import { Grid, GridColumn, GridRow, Icon, Spacing, Tabs } from "gui-elements";
import Loading from "../../../Loading";
import { RuleOperatorList } from "./RuleOperatorList";
import { IRuleOperator, IRuleSidebarExternalTabConfig, IRuleSideBarFilterTabConfig } from "../../RuleEditor.typings";
import { extractSearchWords, matchesAllWords } from "gui-elements/src/components/Typography/Highlighter";
import { SidebarSearchField } from "./SidebarSearchField";
import { partitionArray, sortLexically } from "../../../../../utils/basicUtils";
import { TabProps } from "gui-elements/src/components/Tabs/Tabs";
import useErrorHandler from "../../../../../hooks/useErrorHandler";
import { useTranslation } from "react-i18next";

/** Contains the list of operators that can be dragged and dropped onto the editor canvas and supports filtering. */
export const RuleEditorOperatorSidebar = () => {
    const editorContext = React.useContext(RuleEditorContext);
    const [t] = useTranslation();
    const { registerError } = useErrorHandler();
    const [filteredOperators, setFilteredOperators] = React.useState<IRuleOperator[]>([]);
    // The query that was input in the search field. This won't get immediately active.
    const [textQuery, setTextQuery] = React.useState<string>("");
    const searchWords = extractSearchWords(textQuery);
    const [operatorList, setOperatorList] = React.useState<IRuleOperator[] | undefined>();
    /** Tab handling. */
    // The currently active tab
    const [activeTabId, setActiveTabId] = React.useState<string | undefined>(undefined);
    // Operators that are loaded externally, i.e. when the tab is configured via IRuleSidebarExternalTabConfig
    const activeTab: IRuleSidebarExternalTabConfig | IRuleSideBarFilterTabConfig | undefined = (
        editorContext.tabs ?? []
    ).find((tab) => tab.id === activeTabId);
    const [externalOperatorListLoading, setExternalOperatorListLoading] = React.useState(false);

    // Filter operator list when active query or filters change
    React.useEffect(() => {
        if (operatorList) {
            if (searchWords.length > 0) {
                setFilteredOperators(filterAndSortOperators(operatorList, searchWords));
            } else {
                setFilteredOperators(operatorList);
            }
        }
    }, [textQuery, operatorList]);

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
                    } else {
                        const customTabConfig = tabConfig as IRuleSidebarExternalTabConfig;
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
    const loadExternalOperators = async (config: IRuleSidebarExternalTabConfig) => {
        setExternalOperatorListLoading(true);
        try {
            const externalOperators = await config.fetchOperators();
            setOperatorList((externalOperators ?? []).map((externalOp) => config.convertToOperator(externalOp)));
        } catch (ex) {
            registerError(
                "RuleEditorOperatorSidebar.loadExternalOperators",
                t("taskViews.ruleEditor.errors.loadExternalOperators", { tabName: config.label }),
                ex
            );
        } finally {
            setExternalOperatorListLoading(false);
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
            {externalOperatorListLoading ? (
                <Loading />
            ) : (
                <GridRow verticalStretched={true}>
                    <GridColumn full style={{ paddingTop: "3px" }}>
                        <RuleOperatorList ruleOperatorList={filteredOperators} textQuery={textQuery} />
                    </GridColumn>
                </GridRow>
            )}
        </Grid>
    );
};

// Filter the operators by search query and sort them
const filterAndSortOperators = (operators: IRuleOperator[], searchWords: string[]): IRuleOperator[] => {
    const textToSearchIn = (ruleOperator: IRuleOperator): string => {
        return `${ruleOperator.label} ${(ruleOperator.tags ?? []).join(" ")} ${ruleOperator.description ?? ""} ${(
            ruleOperator.categories ?? []
        ).join(" ")}`.toLowerCase();
    };
    const filtered = operators.filter((op) => {
        return matchesAllWords(textToSearchIn(op), searchWords);
    });
    const matchCount = new Map<IRuleOperator, number>();
    const { matches, nonMatches } = partitionArray(
        filtered,
        (op) => !!searchWords.find((w) => op.label.toLowerCase().includes(w))
    );
    matches.forEach((match) => {
        const label = match.label.toLowerCase();
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
            return matchA.label.toLowerCase() < matchB.label.toLowerCase() ? -1 : 1;
        }
    });
    const byLabel = (op: IRuleOperator) => op.label;
    return [...sortedLabelMatches, ...sortLexically(nonMatches, byLabel)];
};
