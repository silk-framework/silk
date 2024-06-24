import {
    ActivityControlWidget,
    ContextMenu,
    ContextOverlay,
    Divider,
    HtmlContentBlock,
    IActivityStatus,
    MenuItem,
    Notification,
    OverflowText,
    SearchField,
    Spacing,
    Spinner,
    Switch,
    Table,
    TableBody,
    TableContainer,
    TableExpandHeader,
    TableHead,
    TableHeader,
    TableRow,
    Tabs,
    Tag,
    TagList,
    Toolbar,
    ToolbarSection,
    WhiteSpaceContainer,
} from "@eccenca/gui-elements";
import React from "react";
import { useTranslation } from "react-i18next";
import { TaskActivityWidget } from "../../../../shared/TaskActivityWidget/TaskActivityWidget";
import { getEvaluatedLinks, getLinkRuleInputPaths, updateReferenceLink } from "./LinkingEvaluationViewUtils";
import {
    EvaluationLinkInputValue,
    LinkEvaluationFilters,
    LinkEvaluationSortBy,
    LinkEvaluationSortByObj,
    LinkingEvaluationResult,
    LinkRuleEvaluationResult,
    ReferenceLinkType,
} from "./typings";
import utils from "../LinkingRuleEvaluation.utils";
import { IAggregationOperator, IComparisonOperator } from "../../linking.types";
import { EvaluationResultType } from "../LinkingRuleEvaluation";
import { requestRuleOperatorPluginsDetails } from "@ducks/common/requests";
import { IPluginDetails } from "@ducks/common/typings";
import { workspaceSel } from "@ducks/workspace";
import { useSelector } from "react-redux";
import { usePagination } from "@eccenca/gui-elements/src/components/Pagination/Pagination";
import { useFirstRender } from "../../../../../hooks/useFirstRender";
import { DataTableCustomRenderProps, DataTableHeader } from "carbon-components-react";
import { LinkingEvaluationRow } from "./LinkingEvaluationRow";
import { tagColor } from "../../../../shared/RuleEditor/view/sidebar/RuleOperator";
import { TabProps } from "@eccenca/gui-elements/src/components/Tabs/Tab";
import { ReferenceLinksRemoveModal } from "./modals/ReferenceLinksRemoveModal";
import { ImportReferenceLinksModal } from "./modals/ImportReferenceLinksModal";
import { AddReferenceLinkModal } from "./modals/AddReferenceLinkModal";
import useErrorHandler from "../../../../../hooks/useErrorHandler";
import { getHistory } from "../../../../../store/configureStore";
import { legacyLinkingEndpoint } from "../../../../../utils/getApiEndpoint";
import { extractSearchWords, createMultiWordRegex } from "@eccenca/gui-elements/src/components/Typography/Highlighter";

interface LinkingEvaluationTabViewProps {
    projectId: string;
    linkingTaskId: string;
}

const sortDirectionMapping = {
    NONE: "ASC",
    ASC: "DESC",
    DESC: "NONE",
} as const;

type LinkingEvaluationResultWithId = LinkingEvaluationResult & { id: string };

const pageSizes = [10, 20, 50];

const linkingTabs: TabProps[] = [
    { id: 0, title: "Evaluated Links" },
    { id: 1, title: "Reference Links" },
];

const LinkingEvaluationTabView: React.FC<LinkingEvaluationTabViewProps> = ({ projectId, linkingTaskId }) => {
    const [t] = useTranslation();
    const errorHandler = useErrorHandler();
    const commonSel = useSelector(workspaceSel.commonSelector);
    const evaluationResults = React.useRef<LinkRuleEvaluationResult | undefined>();
    const [pagination, paginationElement, onTotalChange] = usePagination({
        pageSizes,
        initialPageSize: 20,
    });
    const [loading, setLoading] = React.useState<boolean>(true);
    const [showInputValues, setShowInputValues] = React.useState<boolean>(true);
    const [showOperators, setShowOperators] = React.useState<boolean>(true);
    const [showStatisticOverlay, setShowStatisticOverlay] = React.useState<boolean>(false);
    const [inputValues, setInputValues] = React.useState<Array<EvaluationLinkInputValue>>([]);
    const [allRowsExpanded, setAllRowsExpanded] = React.useState<boolean>(false);
    const linksToValueMap = React.useRef<Array<Map<string, EvaluationResultType[number]>>>([]);
    const [taskEvaluationStatus, setTaskEvaluationStatus] = React.useState<
        IActivityStatus["concreteStatus"] | undefined
    >();
    const [operatorPlugins, setOperatorPlugins] = React.useState<Array<IPluginDetails>>([]);
    const searchState = React.useRef<{ currentSearchId?: number }>({});
    const [searchQuery, setSearchQuery] = React.useState<string>("");
    const [linkStateFilter, setLinkStateFilter] = React.useState<keyof typeof LinkEvaluationFilters>();
    const [linkSortBy, setLinkSortBy] = React.useState<Array<LinkEvaluationSortBy>>([]);
    const hasRenderedBefore = useFirstRender();
    const [showReferenceLinks, setShowReferenceLinks] = React.useState<boolean>(() => {
        const show = new URLSearchParams(window.location.search).get("showReferenceLinks");
        console.log({ show });
        return Boolean(show);
    });
    const [showImportLinkModal, setShowImportLinkModal] = React.useState<boolean>(false);
    const [showAddLinkModal, setShowAddLinkModal] = React.useState<boolean>(false);
    const [showDeleteReferenceLinkModal, setShowDeleteReferenceLinkModal] = React.useState<boolean>(false);
    // Tracks if in the current view there has been a link state change. This will prevent re-rendering when changing the state.
    const manualLinkChange = React.useRef<boolean>(false);

    const [tableSortDirection, setTableSortDirection] = React.useState<
        Map<typeof headerData[number]["key"], keyof typeof sortDirectionMapping>
    >(
        () =>
            new Map([
                ["source", "NONE"],
                ["target", "NONE"],
                ["confidence", "NONE"],
            ])
    );
    const linkType = showReferenceLinks ? "Reference" : "Evaluation";

    //fetch operator plugins
    React.useEffect(() => {
        (async () => {
            setOperatorPlugins(Object.values((await requestRuleOperatorPluginsDetails(false)).data));
        })();
    }, []);

    const registerError = React.useCallback(
        (errorId: string, err: any, data = {}) =>
            errorHandler.registerError(errorId, t(`linkingEvaluationTabView.errors.${errorId}`, data), err),
        []
    );

    React.useEffect(() => {
        if (evaluationResults.current) {
            onTotalChange(evaluationResults.current.resultStats.filteredLinkCount);
        }
    }, [evaluationResults.current]);

    const fetchEvaluatedLinks = React.useCallback(
        async (pagination, searchQuery = "", filters, linkSortBy, showReferenceLinks) => {
            try {
                setLoading(true);
                // New view is rendered, reset manual link change
                manualLinkChange.current = false;
                const results = (
                    await getEvaluatedLinks(
                        projectId,
                        linkingTaskId,
                        pagination,
                        searchQuery,
                        filters,
                        linkSortBy,
                        showReferenceLinks,
                        !showReferenceLinks
                    )
                )?.data;
                evaluationResults.current = results;
                linksToValueMap.current = results?.links.map((link) => utils.linkToValueMap(link as any)) ?? [];
            } catch (err) {
                registerError("fetchingLinks.msg", `Could not fetch ${linkType} links`, { linkType });
            } finally {
                setLoading(false);
            }
        },
        []
    );

    const debouncedSearch = React.useCallback((query: string) => {
        if (searchState.current.currentSearchId != null) {
            clearTimeout(searchState.current.currentSearchId);
        }
        searchState.current.currentSearchId = window.setTimeout(() => {
            setSearchQuery(query);
        }, 500);
    }, []);

    React.useEffect(() => {
        if (hasRenderedBefore) {
            fetchEvaluatedLinks(
                pagination,
                searchQuery,
                linkStateFilter ? [linkStateFilter] : [],
                linkSortBy,
                showReferenceLinks
            );
        }
    }, [searchQuery]);

    const sortString = linkSortBy.join(",");

    //initial loads of links
    React.useEffect(() => {
        let shouldCancel = false;
        if (!shouldCancel && taskEvaluationStatus === "Successful") {
            fetchEvaluatedLinks(
                pagination,
                searchQuery,
                linkStateFilter ? [linkStateFilter] : [],
                linkSortBy,
                showReferenceLinks
            );
        }
        return () => {
            shouldCancel = true;
        };
    }, [pagination.current, pagination.limit, taskEvaluationStatus, linkStateFilter, sortString, showReferenceLinks]);

    React.useEffect(() => {
        if (
            evaluationResults.current &&
            evaluationResults.current.linkRule &&
            evaluationResults.current.links &&
            linksToValueMap.current.length
        ) {
            const ruleOperator = evaluationResults.current.linkRule.operator;
            if (ruleOperator) {
                const recursivelyGetInputPath = (operator: any): EvaluationLinkInputValue<string> => {
                    return ((operator as IAggregationOperator)?.inputs ?? []).reduce(
                        (acc, input) => {
                            if (!(input as any)?.inputs?.length) {
                                const linkRuleInputPaths = getLinkRuleInputPaths(input);
                                acc = {
                                    source: {
                                        ...acc.source,
                                        ...linkRuleInputPaths.source,
                                    },
                                    target: {
                                        ...acc.target,
                                        ...linkRuleInputPaths.target,
                                    },
                                };
                            } else {
                                const recursedAggregate = recursivelyGetInputPath(input);
                                acc = {
                                    source: {
                                        ...acc.source,
                                        ...recursedAggregate.source,
                                    },
                                    target: {
                                        ...acc.target,
                                        ...recursedAggregate.target,
                                    },
                                };
                            }

                            return acc;
                        },
                        { source: {}, target: {} } as EvaluationLinkInputValue<string>
                    );
                };

                const inputPaths = (ruleOperator as IComparisonOperator)?.sourceInput
                    ? getLinkRuleInputPaths(ruleOperator)
                    : recursivelyGetInputPath(ruleOperator);

                setInputValues(() =>
                    linksToValueMap.current.map((linkToValueMap) =>
                        ["source", "target"].reduce(
                            (matchingInputValue, inputPathType: "source" | "target") => {
                                Object.entries(inputPaths[inputPathType]).forEach(([uri, operatorId]) => {
                                    matchingInputValue[inputPathType][uri] =
                                        linkToValueMap.get(operatorId)?.value ?? [];
                                });
                                return matchingInputValue;
                            },
                            { source: {}, target: {} } as EvaluationLinkInputValue
                        )
                    )
                );
            }
        }
    }, [linksToValueMap.current]);

    const headerData: DataTableHeader[] = [
        {
            key: "source",
            header: (
                <>
                    {t("linkingEvaluationTabView.table.header.source")}:
                    <Spacing vertical={true} size={"tiny"} />
                    {evaluationResults.current?.metaData.sourceInputLabel && (
                        <Tag backgroundColor={tagColor("Source path")} round={true}>
                            {evaluationResults.current?.metaData.sourceInputLabel}
                        </Tag>
                    )}
                </>
            ),
        },
        {
            key: "target",
            header: (
                <>
                    {t("linkingEvaluationTabView.table.header.target")}:
                    <Spacing vertical={true} size={"tiny"} />
                    {evaluationResults.current?.metaData.targetInputLabel && (
                        <Tag backgroundColor={tagColor("Target path")} round={true}>
                            {evaluationResults.current?.metaData.targetInputLabel}
                        </Tag>
                    )}
                </>
            ),
        },
        {
            key: "confidence",
            header: t("linkingEvaluationTabView.table.header.score") as string,
        },
    ];

    const rowData: LinkingEvaluationResultWithId[] = React.useMemo(() => {
        return evaluationResults.current?.links.map((evaluation, i) => ({ ...evaluation, id: `${i}` })) ?? [];
    }, [evaluationResults.current]);

    const handleAllRowsExpansion = React.useCallback(() => setAllRowsExpanded((e) => !e), []);

    const handleReferenceLinkTypeUpdate = React.useCallback(
        async (
            currentLinkType: ReferenceLinkType,
            linkType: ReferenceLinkType,
            source: string,
            target: string,
            index: number
        ): Promise<boolean> => {
            if (currentLinkType === linkType) return false;

            try {
                // Link state will trigger a reference entity cache update. Prevent re-rendering by setting this flag.
                manualLinkChange.current = true;
                const updateResp = await updateReferenceLink(projectId, linkingTaskId, source, target, linkType);
                if (updateResp.axiosResponse.status === 200 && evaluationResults.current) {
                    //update one
                    evaluationResults.current.links[index].decision = linkType;
                }
                return true;
            } catch (err) {
                registerError("updateLink.msg", `Could not update ${linkType} link`, { linkType });
                return false;
            }
        },
        []
    );
    const { nrSourceEntities, nrTargetEntities, nrLinks } = evaluationResults.current?.evaluationActivityStats ?? {
        nrSourceEntities: 0,
        nrTargetEntities: 0,
        nrLinks: 0,
    };

    const handleLinkFilterStateChange = React.useCallback(
        (linkState: keyof typeof LinkEvaluationFilters) =>
            setLinkStateFilter((prev) => (prev === linkState ? undefined : linkState)),
        []
    );

    const handleRowSorting = React.useCallback(
        (key: typeof headerData[number]["key"]) => {
            const sortDirection = tableSortDirection.get(key)!;
            const sortBy =
                sortDirectionMapping[sortDirection] === "NONE"
                    ? []
                    : [LinkEvaluationSortByObj[sortDirectionMapping[sortDirection]][key]];
            setLinkSortBy(sortBy);
            setTableSortDirection((prev) => {
                const newMap = new Map<string, keyof typeof sortDirectionMapping>([...prev]);
                newMap.set(key, sortDirectionMapping[sortDirection]);
                return newMap;
            });
        },
        [tableSortDirection]
    );

    const createUserNotification = React.useCallback(() => {
        if (
            taskEvaluationStatus === "Not executed" ||
            taskEvaluationStatus === "Cancelled" ||
            typeof taskEvaluationStatus === "undefined"
        ) {
            // evaluation action did not run
            return (
                <Notification data-test-id="notification-missing-execution">
                    {t("linkingEvaluationTabView.messages.missingExecution")}
                </Notification>
            );
        }

        if (taskEvaluationStatus === "Waiting" || taskEvaluationStatus === "Running") {
            // evaluation is still running
            return (
                <Notification data-test-id="notification-missing-execution">
                    {t("linkingEvaluationTabView.messages.missingExecution")}
                </Notification>
            );
        }

        if (taskEvaluationStatus === "Failed") {
            // evaluation action did run with errors
            return (
                <Notification warning data-test-id="notification-unsuccessful-evaluation">
                    {t("linkingEvaluationTabView.messages.unsuccessfulEvaluation")}
                </Notification>
            );
        }

        if (taskEvaluationStatus === "Successful" && !searchQuery && !linkStateFilter) {
            // evaluation action done, no filters, no results
            const translationKey = `linkingEvaluationTabView.messages.${
                showReferenceLinks ? "referenceLinksEmptyWithoutFilters" : "emptyWithoutFilters"
            }`;
            return <Notification data-test-id="empty-links-banner">{t(translationKey)}</Notification>;
        }

        if (taskEvaluationStatus === "Successful" && (!!searchQuery || !!linkStateFilter)) {
            // evaluation action done, filters for link state or search query active
            const translationKey = `linkingEvaluationTabView.messages.${
                showReferenceLinks ? "referenceLinksEmptyWithFilters" : "emptyWithFilters"
            }`;
            return <Notification data-test-id="notification-empty-with-filters">{t(translationKey)}</Notification>;
        }

        // Fallback
        return (
            <Notification warning data-test-id="notification-unknown-problem">
                {t("linkingEvaluationTabView.messages.unknownProblem")}
            </Notification>
        );
    }, [taskEvaluationStatus, searchQuery, linkStateFilter]);

    const handleSwitchChange = React.useCallback((switchType: "inputValue" | "operator") => {
        return (val: boolean) => {
            switchType === "inputValue" ? setShowInputValues(val) : setShowOperators(val);
        };
    }, []);

    const handleActivityUpdates = React.useCallback((status: IActivityStatus) => {
        if (status.concreteStatus !== "Successful") {
            setLoading(false);
        }
        if (!manualLinkChange.current) {
            // Only change status when there has not been a manual link state change before, else this will trigger a re-render
            setTaskEvaluationStatus(status.concreteStatus);
        }
    }, []);

    const refreshIfNecessary = async (needsRefresh: boolean) => {
        if (needsRefresh) {
            await fetchEvaluatedLinks(
                pagination,
                searchQuery,
                linkStateFilter ? [linkStateFilter] : [],
                linkSortBy,
                showReferenceLinks
            );
        }
    };

    const closeDeleteReferenceLinks = async (needsRefresh: boolean) => {
        await refreshIfNecessary(needsRefresh);
        setShowDeleteReferenceLinkModal(false);
    };

    const closeImportReferenceLinkModal = React.useCallback(async (needsRefresh: boolean) => {
        await refreshIfNecessary(needsRefresh);
        setShowImportLinkModal(false);
    }, []);

    const closeAddNewReferenceLinkModal = React.useCallback(async (needsRefresh: boolean) => {
        await refreshIfNecessary(needsRefresh);
        setShowAddLinkModal(false);
    }, []);

    const handleLinkingTabSwitch = React.useCallback((tabId: number, prevTabId: number) => {
        if (prevTabId === tabId) {
            return;
        }
        manualLinkChange.current = false;
        evaluationResults.current = undefined;
        const history = getHistory();
        history.replace({
            search: `?${new URLSearchParams(tabId ? { showReferenceLinks: "true" } : {})}`,
        });
        setShowReferenceLinks(!!tabId);
    }, []);

    const resetManualLinkStateFlag = async (originalAction: () => Promise<boolean>) => {
        // Reset when manually running the activities
        manualLinkChange.current = false;
        return originalAction();
    };
    const activityPreAction = React.useMemo(
        () => ({
            start: resetManualLinkStateFlag,
            restart: resetManualLinkStateFlag,
        }),
        []
    );

    // To check if only the row body matches
    const multiWordSearchRegex = createMultiWordRegex(extractSearchWords(searchQuery, true), false);

    return (
        <section className="diapp-linking-evaluation">
            {showDeleteReferenceLinkModal && (
                <ReferenceLinksRemoveModal
                    projectId={projectId}
                    linkingTaskId={linkingTaskId}
                    onClose={closeDeleteReferenceLinks}
                />
            )}
            {showAddLinkModal && (
                <AddReferenceLinkModal
                    projectId={projectId}
                    linkingTaskId={linkingTaskId}
                    onClose={closeAddNewReferenceLinkModal}
                />
            )}
            {showImportLinkModal && (
                <ImportReferenceLinksModal
                    projectId={projectId}
                    linkingTaskId={linkingTaskId}
                    onClose={closeImportReferenceLinkModal}
                />
            )}
            <Tabs
                id="linkingTabs"
                tabs={linkingTabs}
                defaultSelectedTabId={Number(showReferenceLinks)}
                onChange={handleLinkingTabSwitch}
            />
            <Spacing size={"tiny"} />
            <Toolbar noWrap>
                <ToolbarSection canShrink>
                    <Switch
                        className={`input-value-${showInputValues ? "checked" : "unchecked"}`}
                        data-test-id="input-value-switch"
                        checked={showInputValues}
                        onChange={handleSwitchChange("inputValue")}
                    >
                        <OverflowText inline>{t("linkingEvaluationTabView.toolbar.toggleExampleValues")}</OverflowText>
                    </Switch>
                </ToolbarSection>
                <ToolbarSection canShrink>
                    <Spacing vertical size="small" />
                    <Switch
                        className={`operator-${showOperators ? "checked" : "unchecked"}`}
                        data-test-id="operator-switch"
                        checked={showOperators}
                        onChange={handleSwitchChange("operator")}
                    >
                        <OverflowText inline>{t("linkingEvaluationTabView.toolbar.toggleOperatorsTree")}</OverflowText>
                    </Switch>
                </ToolbarSection>
                <ToolbarSection canGrow>
                    <Spacing vertical />
                </ToolbarSection>
                {taskEvaluationStatus === "Successful" && !showReferenceLinks ? (
                    <ToolbarSection canShrink style={{ maxWidth: "25%" }}>
                        <ContextOverlay
                            isOpen={showStatisticOverlay ? true : undefined}
                            onClose={() => setShowStatisticOverlay(false)}
                            interactionKind={"hover"}
                            content={
                                <WhiteSpaceContainer paddingTop="small" paddingRight="small" paddingBottom="small">
                                    <HtmlContentBlock>
                                        <ul>
                                            <li>
                                                {t("linkingEvaluationTabView.toolbar.statsHeadSources")}:{" "}
                                                {nrSourceEntities.toLocaleString(commonSel.locale)}
                                            </li>
                                            <li>
                                                {t("linkingEvaluationTabView.toolbar.statsHeadTargets")}:{" "}
                                                {nrTargetEntities.toLocaleString(commonSel.locale)}
                                            </li>
                                            <li>
                                                {t("linkingEvaluationTabView.toolbar.statsHeadLinkCount")}:{" "}
                                                {nrLinks.toLocaleString(commonSel.locale)}
                                            </li>
                                        </ul>
                                    </HtmlContentBlock>
                                </WhiteSpaceContainer>
                            }
                        >
                            <ActivityControlWidget
                                border
                                small
                                canShrink
                                data-test-id="linking-evaluation-stats"
                                label={
                                    <strong>
                                        {t("linkingEvaluationTabView.toolbar.statsHeadSources")}
                                        {" / "}
                                        {t("linkingEvaluationTabView.toolbar.statsHeadTargets")}
                                        {" / "}
                                        {t("linkingEvaluationTabView.toolbar.statsHeadLinkCount")}
                                    </strong>
                                }
                                statusMessage={`${nrSourceEntities.toLocaleString(
                                    commonSel.locale
                                )} / ${nrTargetEntities.toLocaleString(commonSel.locale)} / ${nrLinks.toLocaleString(
                                    commonSel.locale
                                )}`}
                                activityActions={[
                                    {
                                        icon: "item-info",
                                        action: () => {
                                            setShowStatisticOverlay(!showStatisticOverlay);
                                        },
                                        tooltip: t("linkingEvaluationTabView.toolbar.statsShowInfo"),
                                    },
                                ]}
                            />
                        </ContextOverlay>
                    </ToolbarSection>
                ) : null}
                <ToolbarSection canShrink>
                    <Spacing vertical size="small" />
                    <TaskActivityWidget
                        projectId={projectId}
                        taskId={linkingTaskId}
                        label={showReferenceLinks ? "Reference Links Cache" : "Evaluate Linking"}
                        activityName={showReferenceLinks ? "ReferenceEntitiesCache" : "EvaluateLinking"}
                        isCacheActivity={showReferenceLinks}
                        updateCallback={handleActivityUpdates}
                        layoutConfig={{
                            small: true,
                            border: true,
                            hasSpacing: true,
                            canShrink: true,
                        }}
                        activityActionPreAction={activityPreAction}
                        testId={`${showReferenceLinks ? "referenceLinks" : "evaluateLinking"}-task-activity-widget`}
                    />
                </ToolbarSection>
                {showReferenceLinks ? (
                    <ToolbarSection>
                        <Spacing vertical />
                        <ContextMenu
                            data-test-id="linking-reference-actions"
                            togglerElement="item-moremenu"
                            togglerText="reference links action"
                        >
                            <MenuItem
                                data-test-id="add-reference-links"
                                text={t("ReferenceLinks.contextMenu.add")}
                                onClick={() => setShowAddLinkModal(true)}
                            />
                            <MenuItem
                                data-test-id="remove-reference-links"
                                text={t("ReferenceLinks.contextMenu.removeReferenceLinks")}
                                onClick={() => setShowDeleteReferenceLinkModal(true)}
                            />
                            <MenuItem
                                text={t("ReferenceLinks.contextMenu.import")}
                                onClick={() => setShowImportLinkModal(true)}
                            />
                            <MenuItem
                                text={t("ReferenceLinks.contextMenu.export")}
                                href={legacyLinkingEndpoint(`/tasks/${projectId}/${linkingTaskId}/referenceLinks`)}
                            />
                        </ContextMenu>
                    </ToolbarSection>
                ) : null}
            </Toolbar>
            <Divider addSpacing="medium" />
            <SearchField
                onChange={(e) => debouncedSearch(e.target.value)}
                onClearanceHandler={() => setSearchQuery("")}
                data-test-id="linking-evaluation-search"
            />
            <Spacing size="small" />
            {linkStateFilter && (
                <TagList label="Link State">
                    <Tag
                        data-test-id={`${LinkEvaluationFilters[linkStateFilter].label}-tag`}
                        onRemove={() => handleLinkFilterStateChange(linkStateFilter)}
                    >
                        {LinkEvaluationFilters[linkStateFilter].label}
                    </Tag>
                </TagList>
            )}
            <Spacing size="small" />
            <TableContainer rows={rowData} headers={headerData}>
                {({ headers, getHeaderProps, getTableProps, getRowProps }: DataTableCustomRenderProps) => (
                    <Table
                        {...getTableProps()}
                        size="medium"
                        columnWidths={["30px", "30px", "40%", "40%", "7rem", "9rem"]}
                    >
                        <TableHead>
                            <TableRow>
                                <TableExpandHeader
                                    enableToggle
                                    isExpanded={allRowsExpanded}
                                    onExpand={handleAllRowsExpansion}
                                    togglerText={
                                        allRowsExpanded
                                            ? t("linkingEvaluationTabView.table.header.collapseRows")
                                            : t("linkingEvaluationTabView.table.header.expandRows")
                                    }
                                />
                                <TableHeader key="warning-column">&nbsp;</TableHeader>
                                {headers.map((header) => (
                                    <TableHeader
                                        data-test-id={header.key}
                                        className={tableSortDirection.get(header.key)}
                                        {...getHeaderProps({ header, isSortable: true })}
                                        key={header.key}
                                        isSortHeader={true}
                                        onClick={() => {
                                            handleRowSorting(header.key);
                                        }}
                                        sortDirection={tableSortDirection.get(header.key)}
                                    >
                                        {header.header}
                                    </TableHeader>
                                ))}
                                <TableHeader>
                                    {t("linkingEvaluationTabView.table.header.linkState")}
                                    <Spacing vertical size="tiny" />
                                    <ContextMenu togglerElement="operation-filter" data-test-id="link-state-filter-btn">
                                        <MenuItem
                                            text={t("ReferenceLinks.confirmed")}
                                            active={linkStateFilter === LinkEvaluationFilters.positiveLinks.key}
                                            onClick={() => {
                                                handleLinkFilterStateChange(LinkEvaluationFilters.positiveLinks.key);
                                            }}
                                            data-test-id="link-confirmed-state"
                                        />
                                        <MenuItem
                                            text={t("ReferenceLinks.uncertainOnly")}
                                            active={linkStateFilter === LinkEvaluationFilters.undecidedLinks.key}
                                            onClick={() => {
                                                handleLinkFilterStateChange(LinkEvaluationFilters.undecidedLinks.key);
                                            }}
                                            data-test-id="link-uncertain-state"
                                        />
                                        <MenuItem
                                            text={t("ReferenceLinks.declined")}
                                            active={linkStateFilter === LinkEvaluationFilters.negativeLinks.key}
                                            onClick={() => {
                                                handleLinkFilterStateChange(LinkEvaluationFilters.negativeLinks.key);
                                            }}
                                            data-test-id="link-declined-state"
                                        />
                                    </ContextMenu>
                                </TableHeader>
                            </TableRow>
                        </TableHead>
                        {(evaluationResults.current && evaluationResults.current.links.length && !loading && (
                            <TableBody>
                                {rowData.map((row, rowIdx) => {
                                    const result = evaluationResults.current?.links[rowIdx]!;
                                    const resultString = `${result.source} ${result.target}`.toLowerCase();
                                    const expandedBecauseOfStringMatch =
                                        !!searchQuery.trim() && !multiWordSearchRegex.test(resultString);
                                    return (
                                        <LinkingEvaluationRow
                                            key={rowIdx}
                                            colSpan={headers.length + 3}
                                            rowIdx={rowIdx}
                                            inputValues={inputValues[rowIdx]}
                                            linkingEvaluationResult={result}
                                            rowIsExpandedByParent={allRowsExpanded}
                                            handleReferenceLinkTypeUpdate={handleReferenceLinkTypeUpdate}
                                            searchQuery={searchQuery}
                                            linkRuleOperatorTree={evaluationResults.current?.linkRule.operator}
                                            inputValuesExpandedByDefault={showInputValues}
                                            operatorTreeExpandedByDefault={showOperators}
                                            evaluationMap={linksToValueMap.current[rowIdx]}
                                            operatorPlugins={operatorPlugins}
                                            expandedBySearch={expandedBecauseOfStringMatch}
                                        />
                                    );
                                })}
                            </TableBody>
                        )) ||
                            null}
                    </Table>
                )}
            </TableContainer>
            <Spacing size="small" />
            {(loading && <Spinner size="medium" />) ||
                (!evaluationResults.current?.links?.length && createUserNotification())}
            {!!evaluationResults.current?.links.length && paginationElement}
        </section>
    );
};

export default LinkingEvaluationTabView;
