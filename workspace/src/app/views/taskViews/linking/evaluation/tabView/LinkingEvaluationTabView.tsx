import {
    ActivityControlWidget,
    ContextOverlay,
    ContextMenu,
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
    Tag,
    TagList,
    Toolbar,
    ToolbarSection,
    WhiteSpaceContainer,
    Button,
    IconButton,
    SimpleDialog,
    OverviewItem,
    Checkbox,
    TextField,
    FieldItem,
    Select,
} from "@eccenca/gui-elements";
import React from "react";
import { useTranslation } from "react-i18next";
import { TaskActivityWidget } from "../../../../shared/TaskActivityWidget/TaskActivityWidget";
import {
    getEvaluatedLinks,
    getLinkRuleInputPaths,
    referenceLinkResource,
    updateReferenceLink,
} from "./LinkingEvaluationViewUtils";
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
import { requestRuleOperatorPluginDetails } from "@ducks/common/requests";
import { IPluginDetails } from "@ducks/common/typings";
import { workspaceSel } from "@ducks/workspace";
import { useSelector } from "react-redux";
import { usePagination } from "@eccenca/gui-elements/src/components/Pagination/Pagination";
import { useFirstRender } from "../../../../../hooks/useFirstRender";
import { DataTableCustomRenderProps, DataTableHeader } from "carbon-components-react";
import { LinkingEvaluationRow } from "./LinkingEvaluationRow";
import { tagColor } from "../../../../shared/RuleEditor/view/sidebar/RuleOperator";

interface LinkingEvaluationTabViewProps {
    projectId: string;
    linkingTaskId: string;
}

const sortDirectionMapping = {
    NONE: "ASC",
    ASC: "DESC",
    DESC: "NONE",
} as const;

const referenceLinksMap = new Map([
    ["positive", false],
    ["negative", false],
    ["unlabeled", false],
]) as Map<ReferenceLinkType, boolean>;

type LinkingEvaluationResultWithId = LinkingEvaluationResult & { id: string };

const pageSizes = [10, 20, 50];

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
    const [showStatisticOverlay, setShowStatisticOverlay] = React.useState<boolean>(false);
    const [inputValues, setInputValues] = React.useState<Array<EvaluationLinkInputValue>>([]);
    const [expandedRows, setExpandedRows] = React.useState<Map<number, number>>(new Map());
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
    const [showReferenceLinks, setShowReferenceLinks] = React.useState<boolean>(false);
    const [showImportLinkModal, setShowImportLinkModal] = React.useState<boolean>(false);
    const [shouldGenerateNegativeLink, setShouldGenerateNegativeLink] = React.useState<boolean>(false);
    const [importedReferenceLinkFile, setImportedReferenceLinkFile] = React.useState<FormData>(new FormData());
    const [showAddLinkModal, setShowAddLinkModal] = React.useState<boolean>(false);
    const [newSourceReferenceLink, setNewSourceReferenceLink] = React.useState<string>("");
    const [newLinkCreationLoading, setNewLinkCreationLoading] = React.useState<boolean>(false);
    const [newLinkImportLoading, setNewLinkImportLoading] = React.useState<boolean>(false);
    const [newTargetReferenceLink, setNewTargetReferenceLink] = React.useState<string>("");
    const [newLinkType, setNewLinkType] = React.useState<ReferenceLinkType>("unlabeled");
    const [showDeleteReferenceLinkModal, setShowDeleteReferenceLinkModal] = React.useState<boolean>(false);
    const [deleteReferenceLinkLoading, setDeleteReferenceLinkLoading] = React.useState<boolean>(false);
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
    const [deleteReferenceLinkMap, setDeleteReferenceLinkMap] =
        React.useState<Map<ReferenceLinkType, boolean>>(referenceLinksMap);

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

    const getEvaluatedLinksUtil = React.useCallback(
        async (pagination, searchQuery = "", filters, linkSortBy, showReferenceLinks) => {
            try {
                setLoading(true);
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
            getEvaluatedLinksUtil(
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
            getEvaluatedLinksUtil(
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

    const handleRowExpansion = React.useCallback(
        (rowId?: number) => {
            setExpandedRows((prevExpandedRows) => {
                if (typeof rowId !== "undefined" && prevExpandedRows.has(rowId)) {
                    prevExpandedRows.delete(rowId);
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
                return true;
            } catch (err) {
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
        if (status.concreteStatus !== "Successful") {
            setLoading(false);
        }
        setTaskEvaluationStatus(status.concreteStatus);
    }, []);

    const handleDeleteLinkTypeChecked = React.useCallback((linkType: ReferenceLinkType, checked: boolean) => {
        setDeleteReferenceLinkMap((prev) => new Map([...prev, [linkType, checked]]));
    }, []);

    const handleDeleteReferenceLinks = React.useCallback(async () => {
        try {
            setDeleteReferenceLinkLoading(true);
            await referenceLinkResource(projectId, linkingTaskId, {
                positive: deleteReferenceLinkMap.get("positive")!,
                negative: deleteReferenceLinkMap.get("negative")!,
                unlabeled: deleteReferenceLinkMap.get("unlabeled")!,
            });
            await getEvaluatedLinksUtil(
                pagination,
                searchQuery,
                linkStateFilter ? [linkStateFilter] : [],
                linkSortBy,
                showReferenceLinks
            );
            closeDeleteReferenceLinksMap();
        } catch (err) {
        } finally {
            setDeleteReferenceLinkLoading(false);
        }
    }, [deleteReferenceLinkMap]);

    const closeDeleteReferenceLinksMap = React.useCallback(() => {
        setDeleteReferenceLinkMap(referenceLinksMap);
        setShowDeleteReferenceLinkModal(false);
    }, []);

    const handleImportReferenceLinks = React.useCallback(async () => {
        try {
            setNewLinkImportLoading(true);
            await referenceLinkResource(
                projectId,
                linkingTaskId,
                { generateNegative: shouldGenerateNegativeLink },
                importedReferenceLinkFile,
                "PUT"
            );
            await getEvaluatedLinksUtil(
                pagination,
                searchQuery,
                linkStateFilter ? [linkStateFilter] : [],
                linkSortBy,
                showReferenceLinks
            );
            closeImportReferenceLinkModal();
        } catch (err) {
        } finally {
            setNewLinkImportLoading(false);
        }
    }, [shouldGenerateNegativeLink, importedReferenceLinkFile]);

    const closeImportReferenceLinkModal = React.useCallback(() => {
        setImportedReferenceLinkFile(new FormData());
        setShouldGenerateNegativeLink(false);
        setShowImportLinkModal(false);
    }, []);

    const handleImportReferenceLinkFile = (event: React.ChangeEvent<HTMLInputElement>) => {
        const file = event.target.files?.[0];
        if (file) {
            const newFormData = new FormData();
            newFormData.append("file", file);
            setImportedReferenceLinkFile(newFormData);
        }
    };

    const closeAddNewReferenceLinkModal = React.useCallback(() => {
        setNewSourceReferenceLink("");
        setNewTargetReferenceLink("");
        setNewLinkType("unlabeled");
        setShowAddLinkModal(false);
    }, []);

    const handleAddNewReferenceLinks = React.useCallback(async () => {
        try {
            setNewLinkCreationLoading(true);
            await updateReferenceLink(
                projectId,
                linkingTaskId,
                newSourceReferenceLink,
                newTargetReferenceLink,
                newLinkType
            );
            await getEvaluatedLinksUtil(
                pagination,
                searchQuery,
                linkStateFilter ? [linkStateFilter] : [],
                linkSortBy,
                showReferenceLinks
            );
            closeAddNewReferenceLinkModal();
        } catch (err) {
        } finally {
            setNewLinkCreationLoading(false);
        }
    }, [newSourceReferenceLink, newTargetReferenceLink, newLinkType]);

    return (
        <section className="diapp-linking-evaluation">
            <SimpleDialog
                size="small"
                title="Remove Reference links"
                hasBorder
                isOpen={showDeleteReferenceLinkModal}
                onClose={closeDeleteReferenceLinksMap}
                notifications={
                    <p>
                        Reference links would be deleted for every of the selection above, please make sure you have
                        checked correctly
                    </p>
                }
                actions={[
                    <Button key="cancel" hasStateDanger onClick={handleDeleteReferenceLinks}>
                        {deleteReferenceLinkLoading ? <Spinner size="tiny" /> : "Delete"}
                    </Button>,
                    <Button key="submit" elevated onClick={closeDeleteReferenceLinksMap}>
                        Close
                    </Button>,
                ]}
            >
                <OverviewItem>
                    {Array.from(deleteReferenceLinkMap).map(([linkType, isChecked]) => (
                        <React.Fragment key={linkType}>
                            <Checkbox
                                value={linkType}
                                checked={isChecked}
                                label={linkType}
                                key={linkType}
                                onChange={(e) => handleDeleteLinkTypeChecked(linkType, e.currentTarget.checked)}
                            />
                            <Spacing vertical size="tiny" />
                        </React.Fragment>
                    ))}
                </OverviewItem>
            </SimpleDialog>
            <SimpleDialog
                isOpen={showAddLinkModal}
                size="small"
                title="Add Reference Links"
                onClose={closeAddNewReferenceLinkModal}
                actions={[
                    <Button key="cancel" elevated onClick={handleAddNewReferenceLinks}>
                        {newLinkCreationLoading ? <Spinner size="tiny" /> : "Add"}
                    </Button>,
                    <Button key="submit" onClick={closeAddNewReferenceLinkModal}>
                        Close
                    </Button>,
                ]}
            >
                <FieldItem
                    labelProps={{
                        text: "Source",
                    }}
                >
                    <TextField
                        value={newSourceReferenceLink}
                        placeholder="http://dbpedia.org/resource/Little_Nicky"
                        onChange={(e) => setNewSourceReferenceLink(e.target.value)}
                    />
                </FieldItem>
                <Spacing size="small" />
                <FieldItem
                    labelProps={{
                        text: "Target",
                    }}
                >
                    <TextField
                        value={newTargetReferenceLink}
                        placeholder="http://data.linkedmdb.org/resource/film/1749"
                        onChange={(e) => setNewTargetReferenceLink(e.target.value)}
                    />
                </FieldItem>
                <Spacing size="small" />
                <FieldItem
                    labelProps={{
                        text: "Type",
                    }}
                >
                    <Select
                        items={Array.from(referenceLinksMap).map((r) => ({ label: r[0] }))}
                        onItemSelect={() => {}}
                        itemRenderer={(item, props) => {
                            return (
                                <MenuItem
                                    text={item.label}
                                    onClick={() => setNewLinkType(item.label as ReferenceLinkType)}
                                />
                            );
                        }}
                        filterable={false}
                    >
                        <Button alignText="left" text={newLinkType} fill outlined rightIcon="toggler-showmore" />
                    </Select>
                </FieldItem>
            </SimpleDialog>
            <SimpleDialog
                isOpen={showImportLinkModal}
                size="small"
                title="Import Reference Links"
                onClose={closeImportReferenceLinkModal}
                actions={[
                    <Button key="cancel" elevated onClick={handleImportReferenceLinks}>
                        {newLinkImportLoading ? <Spinner size="tiny" /> : "Import"}
                    </Button>,
                    <Button key="submit" onClick={closeImportReferenceLinkModal}>
                        Close
                    </Button>,
                ]}
            >
                <>
                    <FieldItem
                        labelProps={{
                            text: "File",
                        }}
                    >
                        <TextField type="file" placeholder="Choose file" onChange={handleImportReferenceLinkFile} />
                    </FieldItem>
                    <Spacing size="small" />
                    <Checkbox
                        checked={shouldGenerateNegativeLink}
                        label={"Generate Negative Links"}
                        onChange={(e) => setShouldGenerateNegativeLink(e.currentTarget.checked)}
                    />
                </>
            </SimpleDialog>

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
                {taskEvaluationStatus === "Successful" ? (
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
            <Toolbar>
                <ToolbarSection>
                    <Button
                        small
                        elevated={showReferenceLinks}
                        disabled={!showReferenceLinks}
                        onClick={() => {
                            setShowReferenceLinks(false);
                        }}
                    >
                        Evaluated Links
                    </Button>
                    <Button
                        small
                        elevated={!showReferenceLinks}
                        disabled={showReferenceLinks}
                        onClick={() => setShowReferenceLinks(true)}
                    >
                        Reference Links
                    </Button>
                </ToolbarSection>
                {showReferenceLinks && (
                    <>
                        <ToolbarSection canGrow />
                        <ToolbarSection>
                            <Button small onClick={() => setShowImportLinkModal(true)}>
                                Import Link
                            </Button>
                            <Spacing vertical size="small" />
                            <Button small onClick={() => setShowAddLinkModal(true)}>
                                Add new Link
                            </Button>
                        </ToolbarSection>
                        <ToolbarSection canGrow />
                        <ToolbarSection>
                            <IconButton
                                name="item-download"
                                hasStatePrimary
                                text={t("common.action.download")}
                                href={`/linking/tasks/${projectId}/${linkingTaskId}/referenceLinks`}
                            />
                            <Spacing vertical size="tiny" />
                            <IconButton
                                name="item-remove"
                                text="Remove"
                                hasStateDanger
                                onClick={() => setShowDeleteReferenceLinkModal(true)}
                            />
                        </ToolbarSection>
                    </>
                )}
            </Toolbar>
            <Divider addSpacing="medium" />
            <SearchField
                onChange={(e) => debouncedSearch(e.target.value)}
                onClearanceHandler={() => setSearchQuery("")}
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
                    <Table {...getTableProps()} size="medium" columnWidths={["30px", "40%", "40%", "7rem", "9rem"]}>
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
                                    return (
                                        <LinkingEvaluationRow
                                            key={rowIdx}
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
