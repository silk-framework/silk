import {
    ActivityControlWidget,
    ContextMenu,
    Divider,
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
    Tag,
    TagList,
    Toolbar,
    ToolbarSection,
} from "@eccenca/gui-elements";
import React from "react";
import {useTranslation} from "react-i18next";
import {TaskActivityWidget} from "../../../../../views/shared/TaskActivityWidget/TaskActivityWidget";
import {getEvaluatedLinks, getLinkRuleInputPaths, updateReferenceLink,} from "./LinkingEvaluationViewUtils";
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
import {IAggregationOperator, IComparisonOperator} from "../../linking.types";
import {EvaluationResultType} from "../LinkingRuleEvaluation";
import {requestRuleOperatorPluginDetails} from "@ducks/common/requests";
import {IPluginDetails} from "@ducks/common/typings";
import {debounce} from "lodash";
import {workspaceSel} from "@ducks/workspace";
import {useSelector} from "react-redux";
import {usePagination} from "@eccenca/gui-elements/src/components/Pagination/Pagination";
import {useFirstRender} from "../../../../../hooks/useFirstRender";
import {DataTableCustomRenderProps} from "carbon-components-react";
import {LinkingEvaluationRow} from "./LinkingEvaluationRow";

interface LinkingEvaluationTabViewProps {
    projectId: string;
    linkingTaskId: string;
}

const sortDirectionMapping = {
    NONE: "ASC",
    ASC: "DESC",
    DESC: "NONE",
} as const;

type LinkingEvaluationResultWithId = LinkingEvaluationResult & {id: string}

const pageSizes = [10, 20, 50]

const LinkingEvaluationTabView: React.FC<LinkingEvaluationTabViewProps> = ({ projectId, linkingTaskId }) => {
    const [t] = useTranslation();
    const commonSel = useSelector(workspaceSel.commonSelector);
    const evaluationResults = React.useRef<LinkRuleEvaluationResult | undefined>();
    const [pagination, paginationElement, onTotalChange] = usePagination({
        pageSizes,
        initialPageSize: 20,
    });
    const [loading, setLoading] = React.useState<boolean>(true);
    const [showInputValues, setShowInputValues] = React.useState<boolean>(true);
    const [showOperators, setShowOperators] = React.useState<boolean>(true);
    const [inputValues, setInputValues] = React.useState<Array<EvaluationLinkInputValue>>([]);
    const [expandedRows, setExpandedRows] = React.useState<Map<number, number>>(new Map());
    const linksToValueMap = React.useRef<Array<Map<string, EvaluationResultType[number]>>>([]);
    const [taskEvaluationStatus, setTaskEvaluationStatus] = React.useState<
        IActivityStatus["concreteStatus"] | undefined
    >();
    const [operatorPlugins, setOperatorPlugins] = React.useState<Array<IPluginDetails>>([]);
    const [searchQuery, setSearchQuery] = React.useState<string>("");
    const [linkStateFilter, setLinkStateFilter] = React.useState<keyof typeof LinkEvaluationFilters>();
    const [linkSortBy, setLinkSortBy] = React.useState<Array<LinkEvaluationSortBy>>([]);
    const hasRenderedBefore = useFirstRender();
    const [tableSortDirection, setTableSortDirection] = React.useState<
        Map<typeof headerData[number]["key"], "ASC" | "DESC" | "NONE">
    >(
        () =>
            new Map([
                ["source", "NONE"],
                ["target", "NONE"],
                ["confidence", "NONE"],
            ])
    );

    //fetch operator plugins
    React.useEffect(() => {
        (async () => {
            setOperatorPlugins(Object.values((await requestRuleOperatorPluginDetails(false)).data));
        })();
    }, []);

    React.useEffect(() => {
        if (evaluationResults.current) {
            onTotalChange(evaluationResults.current.resultStats.filteredLinkCount);
        }
    }, [evaluationResults.current]);

    const getEvaluatedLinksUtil = React.useCallback(async (pagination, searchQuery = "", filters, linkSortBy) => {
        try {
            setLoading(true);
            const results = (
                await getEvaluatedLinks(projectId, linkingTaskId, pagination, searchQuery, filters, linkSortBy)
            )?.data;
            evaluationResults.current = results;
            linksToValueMap.current = results?.links.map((link) => utils.linkToValueMap(link as any)) ?? [];
            // TODO: Is this really needed anymore?
            // setInputValuesExpansion(
            //     () => new Map(results?.links.map((_, idx) => [idx, { expanded: showInputValues, precinct: true }]))
            // );
            // setOperatorsExpansion(
            //     () => new Map(results?.links.map((_, idx) => [idx, { expanded: showOperators, precinct: true }]))
            // );
        } catch (err) {
        } finally {
            setLoading(false);
        }
    }, []);

    const debouncedSearchUtil = React.useCallback(
        debounce(async (searchQuery: string) => {
            await getEvaluatedLinksUtil(pagination, searchQuery, linkStateFilter ? [linkStateFilter] : [], linkSortBy);
        }, 500),
        []
    );

    React.useEffect(() => {
        let shouldCancel = false;
        if (!shouldCancel && hasRenderedBefore) {
            debouncedSearchUtil(searchQuery);
        }

        return () => {
            shouldCancel = true;
        };
    }, [searchQuery]);

    const sortString = linkSortBy.join(",")

    //initial loads of links
    React.useEffect(() => {
        let shouldCancel = false;
        if (!shouldCancel && taskEvaluationStatus === "Successful") {
            getEvaluatedLinksUtil(pagination, searchQuery, linkStateFilter ? [linkStateFilter] : [], linkSortBy);
        }
        return () => {
            shouldCancel = true;
        };
    }, [pagination.current, pagination.limit, taskEvaluationStatus, linkStateFilter, sortString]);

    // TODO: Do we need this behavior?
    // const handleAlwaysExpandSwitch = React.useCallback(
    //     (inputSwitch: "operator" | "inputValue") => {
    //         if (evaluationResults.current?.links.length) {
    //             //if expansion buttons have been triggered by user, maintain user's update while changing the default of the rest.
    //             const newUpdate = (prev) =>
    //                 new Map(
    //                     evaluationResults.current?.links.map((_: any, idx: number) => {
    //                         const { precinct, expanded } = prev.get(idx);
    //                         return [
    //                             idx,
    //                             {
    //                                 expanded: precinct ? !expanded : expanded,
    //                                 precinct: precinct ?? true,
    //                             },
    //                         ];
    //                     })
    //                 );
    //             inputSwitch === "inputValue" ? setInputValuesExpansion(newUpdate) : setOperatorsExpansion(newUpdate);
    //         }
    //     },
    //     [showOperators, showInputValues, evaluationResults]
    // );

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

    const headerData = [
        {
            key: "source",
            header: t("linkingEvaluationTabView.table.header.source"),
        },
        {
            key: "target",
            header: t("linkingEvaluationTabView.table.header.target"),
        },
        {
            key: "confidence",
            header: t("linkingEvaluationTabView.table.header.score"),
        },
    ];

    const rowData: LinkingEvaluationResultWithId[] = React.useMemo(() => {
            return evaluationResults.current?.links.map((evaluation, i) => ({...evaluation, id: `${i}`})) ?? []
        },
        [evaluationResults.current]
    );

    const handleRowExpansion = React.useCallback(
        (rowId?: number) => {
            setExpandedRows((prevExpandedRows) => {
                if (typeof rowId !== "undefined" && prevExpandedRows.has(rowId)) {
                    prevExpandedRows.delete(rowId);
                    //when universal expansion btn is pressed,
                    //modify input and operator list behavior
                    // TODO: Is this really needed?
                    // handleNodeExpand(rowId, showOperators, true);
                    // setInputValuesExpansion((prevInputExpansion) => {
                    //     prevInputExpansion.set(rowId, {
                    //         expanded: showInputValues,
                    //         precinct: true,
                    //     });
                    //     return new Map(prevInputExpansion);
                    // });
                    return new Map([...prevExpandedRows]);
                } else if (typeof rowId !== "undefined") {
                    //provided row id doesn't exist in record
                    return new Map([...prevExpandedRows, [rowId, rowId]]);
                } else {
                    //should either collapse all or expand all.
                    if (prevExpandedRows.size === rowData.length) return new Map();
                    return new Map(rowData.map((_row: any, i: number) => [i, i]));
                }
            });
        },
        [rowData]
    );

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
                const updateResp = await updateReferenceLink(projectId, linkingTaskId, source, target, linkType);
                if (updateResp.axiosResponse.status === 200 && evaluationResults.current) {
                    //update one
                    evaluationResults.current.links[index].decision = linkType;
                }
                return true
            } catch (err) {
                return false
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
            const sortBy = sortDirectionMapping[sortDirection] === "NONE"
                ? []
                : [LinkEvaluationSortByObj[sortDirectionMapping[sortDirection]][key]]
            setLinkSortBy(sortBy);
            setTableSortDirection((prev) => {
                const newMap = new Map<string, "ASC" | "DESC" | "NONE">([...prev])
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

        if(taskEvaluationStatus === "Waiting" || taskEvaluationStatus === "Running") {
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
            return (
                <Notification data-test-id="empty-links-banner">
                    {t("linkingEvaluationTabView.messages.emptyWithoutFilters")}
                </Notification>
            );
        }

        if (taskEvaluationStatus === "Successful" && (!!searchQuery || !!linkStateFilter)) {
            // evaluation action done, filters for link state or search query active
            return (
                <Notification data-test-id="notification-empty-with-filters">
                    {t("linkingEvaluationTabView.messages.emptyWithFilters")}
                </Notification>
            );
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

    const registerForTaskUpdates = React.useCallback((status: IActivityStatus) => {
        if(status.concreteStatus !== "Successful") {
            setLoading(false)
        }
        setTaskEvaluationStatus(status.concreteStatus);
    }, []);

    return (
        <section className="diapp-linking-evaluation">
            <Toolbar noWrap>
                <ToolbarSection canShrink>
                    <Switch
                        id={`input-value-${showInputValues ? "checked" : "unchecked"}`}
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
                        id={`operator-${showInputValues ? "checked" : "unchecked"}`}
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
                {taskEvaluationStatus === "Successful" && (
                    <ToolbarSection canShrink style={{ maxWidth: "25%" }}>
                        <ActivityControlWidget
                            border
                            small
                            canShrink
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
                                    action: () => {}, // TODO
                                    tooltip: t("linkingEvaluationTabView.toolbar.statsShowInfo"),
                                },
                            ]}
                        />
                    </ToolbarSection>
                )}
                <ToolbarSection canShrink>
                    <Spacing vertical size="small" />
                    <TaskActivityWidget
                        projectId={projectId}
                        taskId={linkingTaskId}
                        label="Evaluate Linking"
                        activityName="EvaluateLinking"
                        registerToReceiveUpdates={registerForTaskUpdates}
                        layoutConfig={{
                            small: true,
                            border: true,
                            hasSpacing: true,
                            canShrink: true,
                        }}
                    />
                </ToolbarSection>
            </Toolbar>
            <Divider addSpacing="medium" />
            <SearchField
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                onClearanceHandler={() => setSearchQuery("")}
            />
            <Spacing size="small" />
            {linkStateFilter && (
                <TagList label="Link State">
                    <Tag onRemove={() => handleLinkFilterStateChange(linkStateFilter)}>
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
                        columnWidths={["30px", "40%", "40%", "7rem", "9rem"]}
                        useZebraStyles
                    >
                        <TableHead>
                            <TableRow>
                                <TableExpandHeader
                                    enableToggle
                                    isExpanded={expandedRows.size === rowData.length}
                                    onExpand={() => handleRowExpansion()}
                                    togglerText={
                                        expandedRows.size === rowData.length
                                            ? t("linkingEvaluationTabView.table.header.collapseRows")
                                            : t("linkingEvaluationTabView.table.header.expandRows")
                                    }
                                />
                                {headers.map((header) => (
                                    <TableHeader
                                        data-test-id={header.key}
                                        className={tableSortDirection.get(header.key)}
                                        {...getHeaderProps({ header, isSortable: true })}
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
                                    <ContextMenu togglerElement="operation-filter">
                                        <MenuItem
                                            text={t("ReferenceLinks.confirmed")}
                                            active={linkStateFilter === LinkEvaluationFilters.positiveLinks.key}
                                            onClick={() => {
                                                handleLinkFilterStateChange(LinkEvaluationFilters.positiveLinks.key);
                                            }}
                                        />
                                        <MenuItem
                                            text={t("ReferenceLinks.uncertainOnly")}
                                            active={linkStateFilter === LinkEvaluationFilters.undecidedLinks.key}
                                            onClick={() => {
                                                handleLinkFilterStateChange(LinkEvaluationFilters.undecidedLinks.key);
                                            }}
                                        />
                                        <MenuItem
                                            text={t("ReferenceLinks.declined")}
                                            active={linkStateFilter === LinkEvaluationFilters.negativeLinks.key}
                                            onClick={() => {
                                                handleLinkFilterStateChange(LinkEvaluationFilters.negativeLinks.key);
                                            }}
                                        />
                                    </ContextMenu>
                                </TableHeader>
                            </TableRow>
                        </TableHead>
                        {(evaluationResults.current && evaluationResults.current.links.length && !loading && (
                            <TableBody>
                                {rowData.map((row, rowIdx) => {
                                    return <LinkingEvaluationRow
                                        colSpan={headers.length + 2}
                                        rowIdx={rowIdx}
                                        inputValues={inputValues[rowIdx]}
                                        linkingEvaluationResult={evaluationResults.current?.links[rowIdx]!}
                                        rowIsExpanded={expandedRows.has(rowIdx)}
                                        handleReferenceLinkTypeUpdate={handleReferenceLinkTypeUpdate}
                                        searchQuery={searchQuery}
                                        // carbonRowProps={getRowProps({ row })} // TODO: What is this needed for? This leads to unnecessary re-renders even though it didn't change
                                        handleRowExpansion={handleRowExpansion}
                                        linkRuleOperatorTree={evaluationResults.current?.linkRule.operator}
                                        inputValuesExpandedByDefault={showInputValues}
                                        operatorTreeExpandedByDefault={showOperators}
                                        evaluationMap={linksToValueMap.current[rowIdx]}
                                        operatorPlugins={operatorPlugins}
                                    />;
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
