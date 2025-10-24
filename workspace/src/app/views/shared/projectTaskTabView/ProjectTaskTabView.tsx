import React, { useEffect, useState, Fragment } from "react";
import { useDispatch, useSelector } from "react-redux";
import { useHistory } from "react-router";
import { useTranslation } from "react-i18next";
import locationParser from "query-string";
import { CLASSPREFIX as eccgui } from "@eccenca/gui-elements/src/configuration/constants";
import {
    Button,
    Card,
    CardContent,
    CardHeader,
    CardOptions,
    CardTitle,
    Divider,
    Grid,
    GridRow,
    IconButton,
    Modal,
    Notification,
    modalPreventEvents,
    Spacing,
} from "@eccenca/gui-elements";
import { IItemLink } from "@ducks/shared/typings";
import { requestItemLinks } from "@ducks/shared/requests";
import { commonSel } from "@ducks/common";
import Loading from "../Loading";
import { SERVE_PATH } from "../../../constants/path";
import { IProjectTaskView, IViewActions, pluginRegistry } from "../../plugins/PluginRegistry";
import PromptModal from "./PromptModal";
import ErrorBoundary from "../../../ErrorBoundary";
import { ProjectTaskTabViewContext } from "./ProjectTaskTabViewContext";

const getBookmark = () => window.location.pathname.split("/").slice(-1)[0];

/** Returns the bookmark location for the currently selected tab
 *
 * @param id The ID of the tab.
 * @param unBookmarkedSuffix Optional path suffix when no tab selection is present in the URL.
 * @param tabViews All tab views
 */
const calculateBookmark = (
    id: string,
    unBookmarkedSuffix: string | undefined,
    tabViews: Partial<IProjectTaskView & IItemLink>[],
    search = "",
) => {
    const pathnameArray = window.location.pathname.split("/");
    const [currentTab] = pathnameArray.slice(-1);
    const currentTabExist = tabViews.find((view) => view.id === currentTab);
    if (currentTabExist || (!!unBookmarkedSuffix && currentTab !== unBookmarkedSuffix)) {
        pathnameArray.splice(-1);
    }
    return `${pathnameArray.join("/")}/${id}${search}`;
};

interface IProjectTaskTabView {
    // The title of the widget
    title?: string;
    // array of URLs, each one will be extended by parameter inlineView=true
    srcLinks?: IItemLink[];
    // URL that should initially be used when loaded, must be part of srcLinks or a view ID from the task views
    startWithLink?: IItemLink | string;
    // show initially as fullscreen
    startFullscreen?: boolean;
    // integrate only as modal that can be closed by this handler
    handlerRemoveModal?: () => void;
    // name attribute value of the i-frame
    iFrameName?: string;
    // When defined, the registered task views will be shown
    taskViewConfig?: {
        // The plugin ID of the project task
        pluginId: string;
        // If the views are not shown on the task details page, this must be set
        projectId?: string;
        // If the views are not shown on the task details page, this must be set
        taskId?: string;
    };
    viewActions?: IViewActions;
    // Modal ID for tracking its open/closed state
    modalId?: string;
}

export const defaultProjectTaskTabViewFullScreenModalId = "projectTaskTabViewFullScreenModal"

/**
 * Displays views of a specific project task. Item links are displayed in an i-frame whereas component views are directly
 * rendered.
 */
export function ProjectTaskTabView({
    title,
    srcLinks,
    startWithLink,
    startFullscreen = false,
    handlerRemoveModal,
    taskViewConfig,
    iFrameName,
    viewActions,
    modalId = defaultProjectTaskTabViewFullScreenModalId,
    ...otherProps
}: IProjectTaskTabView) {
    const initialSettings = useSelector(commonSel.initialSettingsSelector);
    const globalProjectId = useSelector(commonSel.currentProjectIdSelector);
    const globalTaskId = useSelector(commonSel.currentTaskIdSelector);
    const projectId = taskViewConfig?.projectId ?? globalProjectId;
    const taskId = taskViewConfig?.taskId ?? globalTaskId;
    const dispatch = useDispatch();
    const history = useHistory();
    const [t] = useTranslation();
    const [isFetchingLinks, setIsFetchingLinks] = useState(true);
    const iframeRef = React.useRef<HTMLIFrameElement>(null);
    // Keeps track of the loaded path of the i-frame. Only the case when selected tab is an item link.
    const [activeIframePath, setActiveIframePath] = React.useState<IItemLink | undefined>(undefined);
    // active link. Either an item link or a view ID
    const [selectedTab, setSelectedTab] = useState<IItemLink | string | undefined>(startWithLink);
    // list of aggregated links
    const [itemLinks, setItemLinks] = useState<IItemLink[]>([]);
    const [taskViews, setTaskViews] = React.useState<IProjectTaskView[] | undefined>(undefined);
    // Stores request to change the tab. The requests is represented by the tab idx.
    const [tabRouteChangeRequest, setTabRouteChangeRequest] = React.useState<string | undefined>(undefined);
    // To react to task changes
    const [selectedTask, setSelectedTask] = React.useState<string | undefined>(taskViewConfig?.taskId);
    const [openTabSwitchPrompt, setOpenTabSwitchPrompt] = React.useState<boolean>(false);
    const [unsavedChanges, setUnsavedChanges] = React.useState<boolean>(false);
    const [blockedTab, setBlockedTab] = React.useState<IItemLink | string | undefined>(undefined);
    const [blockedClosingModal, setBlockedClosingModal] = React.useState<boolean>(false);
    const viewsAndItemLink: Partial<IProjectTaskView & IItemLink>[] = [...(taskViews ?? []), ...itemLinks];
    const isTaskView = (viewOrItemLink: Partial<IProjectTaskView & IItemLink>) => !viewOrItemLink.path;
    const itemLinkActive = selectedTab != null && typeof selectedTab !== "string";
    const [warnings, setWarnings] = React.useState<string[] | undefined>(undefined);

    const handlerRemoveModalWrapper = React.useCallback(
        (overwrite = false) => {
            if (unsavedChanges && !overwrite) {
                setBlockedClosingModal(true);
                setOpenTabSwitchPrompt(true); //trigger modal
            } else {
                setBlockedClosingModal(false);
                handlerRemoveModal && handlerRemoveModal();
            }
        },
        [unsavedChanges, handlerRemoveModal],
    );

    // Either the ID of an IItemLink or the view ID or undefined
    const activeTab: IProjectTaskView | IItemLink | undefined =
        (activeIframePath?.id ?? itemLinkActive)
            ? (selectedTab as IItemLink)
            : (taskViews ?? []).find((v) => v.id === selectedTab);

    React.useEffect(() => {
        if (projectId && taskId) {
            if (taskViewConfig?.pluginId) {
                const taskViewPlugins = pluginRegistry
                    .taskViews(taskViewConfig.pluginId)
                    .filter((plugin) => !plugin.available || plugin.available(initialSettings))
                    .sort((a, b) => {
                        return (a.sortOrder ?? 9999) - (b.sortOrder ?? 9999);
                    });
                setTaskViews(taskViewPlugins);
            } else {
                setTaskViews([]);
            }
        }
    }, [projectId, taskId, taskViewConfig?.pluginId, initialSettings]);

    React.useEffect(() => {
        fetchTaskNotifications();
    }, [viewActions?.taskContext]);

    const fetchTaskNotifications = React.useCallback(async () => {
        const warnings = viewActions?.taskContext
            ? await viewActions.taskContext.taskContextNotification?.(viewActions.taskContext.context)
            : undefined;
        setWarnings(warnings ? warnings.map((w) => w.message) : undefined);
    }, []);

    React.useEffect(() => {
        if (tabRouteChangeRequest != null) {
            const tabItem = viewsAndItemLink.find((itemView) => itemView.id === tabRouteChangeRequest);
            if (tabItem && isTaskView(tabItem)) {
                setSelectedTab(tabItem.id);
                setActiveIframePath(undefined);
            } else {
                setSelectedTab(tabItem as IItemLink);
                if (iframeRef.current) {
                    iframeRef.current.src = createIframeUrl((tabItem as IItemLink).path);
                }
            }
            setTabRouteChangeRequest(undefined);
        }
    }, [tabRouteChangeRequest]);

    React.useEffect(() => {
        if (projectId && taskId && taskId !== selectedTask) {
            setSelectedTask(taskId);
        }
    }, [projectId, taskId, selectedTask]);

    // flag if the widget is shown as fullscreen modal
    const [displayFullscreen, setDisplayFullscreen] = useState(!!handlerRemoveModal || startFullscreen);
    // handler for toggling fullscreen mode
    const toggleFullscreen = () => {
        setDisplayFullscreen(!displayFullscreen);
    };

    function getTabRoute(tabItem: IItemLink | string): Partial<IProjectTaskView & IItemLink> | undefined {
        return viewsAndItemLink.find((itemView) => {
            if (typeof tabItem === "string") {
                return itemView.id === tabItem;
            } else {
                return tabItem.id === itemView.id;
            }
        });
    }

    const getTaskView = (selectedTab: IItemLink | string | undefined): IProjectTaskView | undefined => {
        return (taskViews ?? []).find((tv) => tv.id === selectedTab);
    };

    /** Extracts the search query to propagate to the next task view. */
    const extractSearchQuery = (toTaskView: IProjectTaskView): string => {
        const queryParamsToKeep = toTaskView.queryParametersToKeep ?? [];

        if (queryParamsToKeep.length) {
            const keptParams = new URLSearchParams();
            const currentSearchParams = new URLSearchParams(window.location.search);
            let keep = false;
            queryParamsToKeep.forEach((paramId) => {
                if (currentSearchParams.has(paramId)) {
                    const paramValue = currentSearchParams.get(paramId);
                    keptParams.set(paramId, paramValue ?? "");
                    keep = true;
                }
            });
            if (keep) {
                return "?" + keptParams.toString();
            } else {
                return "";
            }
        } else {
            return "";
        }
    };

    // handler for link change. Triggers a tab change request. Actual change is done in useEffect.
    const changeTab = (tabItem: IItemLink | string, overwrite = false) => {
        if (unsavedChanges && !overwrite) {
            //trigger modal
            setOpenTabSwitchPrompt(true);
            setBlockedTab(tabItem);
            setBlockedClosingModal(false);
        } else {
            const tabRoute = getTabRoute(tabItem);
            setUnsavedChanges(false);
            setOpenTabSwitchPrompt(false);
            setTabRouteChangeRequest(tabRoute?.id);
            const toTaskView = getTaskView(tabRoute?.id);
            const queryToKeep = toTaskView ? extractSearchQuery(toTaskView) : "";
            !startFullscreen &&
                dispatch(history.replace(calculateBookmark(tabRoute?.id ?? "", taskId, viewsAndItemLink, queryToKeep)));
        }
    };

    /** show browser prompt when making route changes except changes with search params */
    React.useEffect(() => {
        window.onbeforeunload = () => (unsavedChanges ? true : null);
        const unBlock = history.block((location) =>
            !location.search.length && unsavedChanges && !openTabSwitchPrompt
                ? (t("Metadata.unsavedMetaDataWarning") as string)
                : undefined,
        );
        return () => unBlock();
    }, [unsavedChanges, openTabSwitchPrompt]);

    const getInitialActiveLink = (
        itemLinks: IItemLink[],
        taskViews: IProjectTaskView[],
    ): IItemLink | string | undefined => {
        const initial = [...taskViews, ...itemLinks].find((elem) => {
            return elem.id === getBookmark();
        });
        if (initial) {
            return (initial as IItemLink).path ? (initial as IItemLink) : initial.id;
        } else {
            return taskViews[0]?.id ?? itemLinks[0];
        }
    };

    React.useEffect(() => {
        if (iframeRef.current && selectedTab && itemLinks.length && taskViews) {
            const iframeLoadHandler = () => {
                if (iframeRef.current) {
                    const regex = new RegExp("\\?.*", "gi");
                    const parsedCurrentIframePath = iframeRef.current.src.replace(regex, "");
                    const focusedIframeSource = itemLinks.find((link) =>
                        parsedCurrentIframePath.endsWith(link.path.replace(regex, "")),
                    );
                    setActiveIframePath(focusedIframeSource);
                }
            };
            iframeRef.current.addEventListener("load", iframeLoadHandler);
            return () => {
                if (iframeRef.current) {
                    iframeRef.current.removeEventListener("load", iframeLoadHandler);
                }
            };
        }
    }, [iframeRef, selectedTab, itemLinks?.length, taskViews]);

    const removeDuplicates = (srcLinks: IItemLink[]): IItemLink[] => {
        const taskViewsLabels = (taskViews || []).map((t) => t.id);
        return srcLinks.filter((item) => !taskViewsLabels.includes(item.id));
    };

    // update item links by rest api request
    const getItemLinksAndSelectTab = async (projectId: string, taskId: string, taskViews: IProjectTaskView[]) => {
        setIsFetchingLinks(true);
        try {
            const { data } = await requestItemLinks(projectId, taskId);
            // remove current page link
            const srcLinks = removeDuplicates(data.filter((item) => !item.path.startsWith(SERVE_PATH)));
            setItemLinks(srcLinks);
            if (!startWithLink) {
                setSelectedTab(getInitialActiveLink(srcLinks, taskViews));
            }
        } catch (e) {
        } finally {
            setIsFetchingLinks(false);
        }
    };

    useEffect(() => {
        if (taskViews) {
            if (!!srcLinks && srcLinks.length > 0) {
                setItemLinks(srcLinks);
                setSelectedTab(startWithLink || srcLinks[0]);
            } else if (projectId && taskId) {
                getItemLinksAndSelectTab(projectId, taskId, taskViews);
            } else {
                setItemLinks([]);
            }
            setIsFetchingLinks(false);
        }
    }, [projectId, taskId, taskViews]);

    const tLabel = (label: string) => {
        return t("common.iframeWindow." + label, label);
    };

    const createIframeUrl = (url: string) => {
        const iframeUrl = locationParser.parseUrl(url, { parseFragmentIdentifier: true });
        iframeUrl.query.inlineView = "true";
        return locationParser.stringifyUrl(iframeUrl);
    };

    const viewActionsUnsavedChanges = React.useCallback((status: boolean) => {
        setUnsavedChanges(status);
    }, []);

    const customModalPreventEvents = React.useMemo(() => {
        const eventHandlers = {
            ...modalPreventEvents,
        };
        const pluginId = taskViewConfig?.pluginId;
        if (pluginId === "linking" || pluginId === "workflow" || pluginId === "transform") {
            // Workaround for mouseup event being swallowed before its handled when connecting edges in the react-flow editors
            eventHandlers.onMouseUp = () => {};
        }
        return eventHandlers;
    }, [taskViewConfig?.pluginId]);

    const extendedViewActions: IViewActions = {
        ...viewActions,
        switchToView: (viewIdx) => {
            // FIXME: Change to viewId when this component is switched to viewId instead of index
            const view = viewsAndItemLink[viewIdx];
            if (view) {
                if (view.id) {
                    changeTab(view.id);
                } else {
                    changeTab(view as IItemLink);
                }
            }
        },
        unsavedChanges: viewActionsUnsavedChanges,
    };

    let tabNr = 1;

    const tabsWidget = (projectId: string | undefined, taskId: string | undefined) => {
        const suffix =
            getTaskView(selectedTab)?.supportsTaskContext && viewActions?.taskContext
                ? viewActions.taskContext.taskViewSuffix?.(viewActions.taskContext.context)
                : undefined;
        return (
            <ProjectTaskTabViewContext.Provider
                value={{
                    fullScreen: displayFullscreen,
                }}
            >
                <Card
                    className="diapp-iframewindow__content"
                    isOnlyLayout={true}
                    elevation={displayFullscreen ? 4 : 1}
                    scrollinOnFocus={displayFullscreen ? undefined : "center"}
                >
                    <CardHeader>
                        <CardTitle>
                            <h2>
                                {tLabel(activeTab?.label ?? "")} {suffix}
                            </h2>
                        </CardTitle>
                        <CardOptions>
                            {viewsAndItemLink.length > 1 &&
                                viewsAndItemLink.map((tabItem, idx) => {
                                    const tabRoute = getTabRoute(tabItem.id ?? (tabItem as IItemLink));
                                    return (
                                        <Button
                                            data-test-id={"taskView-" + (tabItem.id ?? `-iframe-${tabNr++}`)}
                                            key={tabItem.id ?? tabItem.path}
                                            onClick={(e) => {
                                                e.preventDefault();
                                                changeTab(tabItem.id ?? (tabItem as IItemLink));
                                            }}
                                            href={calculateBookmark(tabRoute?.id ?? "", taskId, viewsAndItemLink)}
                                            minimal={true}
                                            disabled={
                                                !!selectedTab &&
                                                (tabItem.path ?? tabItem.id) ===
                                                    ((selectedTab as any)?.path ?? selectedTab)
                                            }
                                        >
                                            {tLabel(tabItem.label as string)}
                                        </Button>
                                    );
                                })}
                            {!!handlerRemoveModal ? (
                                <IconButton
                                    data-test-id={"close-project-tab-view"}
                                    name="navigation-close"
                                    onClick={() => handlerRemoveModalWrapper()}
                                />
                            ) : (
                                <IconButton
                                    name={displayFullscreen ? "toggler-minimize" : "toggler-maximize"}
                                    onClick={toggleFullscreen}
                                />
                            )}
                        </CardOptions>
                    </CardHeader>
                    <Divider />
                    <CardContent
                        className={
                            !!selectedTab && itemLinkActive
                                ? "diapp-iframewindow--iframecontent"
                                : "diapp-iframewindow--jscontent"
                        }
                    >
                        {!!selectedTab ? (
                            itemLinkActive ? (
                                <iframe
                                    ref={iframeRef}
                                    name={iFrameName}
                                    data-test-id={iFrameName}
                                    src={createIframeUrl((selectedTab as IItemLink)?.path ?? "")}
                                    title={tLabel((selectedTab as IItemLink)?.label)}
                                    style={{
                                        position: "absolute",
                                        width: "100%",
                                        height: "100%",
                                    }}
                                />
                            ) : (
                                projectId &&
                                taskId &&
                                (taskViews ?? [])
                                    .find((v) => v.id === selectedTab)
                                    ?.render(projectId, taskId, extendedViewActions, startFullscreen)
                            )
                        ) : isFetchingLinks ? (
                            <Loading />
                        ) : (
                            <>
                                <Spacing />
                                <div style={{ textAlign: "center" }}>No tab available/selected.</div>
                            </>
                        )}
                    </CardContent>
                    {warnings && <CardContentWarnings warnings={warnings} />}
                </Card>
            </ProjectTaskTabViewContext.Provider>
        );
    };

    return (
        <ErrorBoundary>
            <PromptModal
                onClose={() => {
                    setOpenTabSwitchPrompt(false);
                    setBlockedTab(undefined);
                }}
                isOpen={openTabSwitchPrompt}
                proceed={() =>
                    blockedClosingModal ? handlerRemoveModalWrapper(true) : blockedTab && changeTab(blockedTab, true)
                }
            />
            {selectedTask === taskId && !!handlerRemoveModal ? (
                <Modal
                    size="fullscreen"
                    isOpen={true}
                    onClose={() => handlerRemoveModalWrapper()}
                    wrapperDivProps={customModalPreventEvents}
                    modalId={modalId}
                >
                    <ErrorBoundary>{tabsWidget(projectId, taskId)}</ErrorBoundary>
                </Modal>
            ) : selectedTask === taskId ? (
                <section className={"diapp-iframewindow"} {...otherProps}>
                    <div className="diapp-iframewindow__placeholder">
                        <Grid>
                            <GridRow fullHeight={true} />
                        </Grid>
                    </div>
                    <div
                        className={displayFullscreen ? "diapp-iframewindow--fullscreen" : "diapp-iframewindow--inside"}
                    >
                        <ErrorBoundary>{tabsWidget(projectId, taskId)}</ErrorBoundary>
                    </div>
                </section>
            ) : null}
        </ErrorBoundary>
    );
}

interface CardContentWarningsProps {
    /** The warning messages that should be displayed. */
    warnings: string[];
}

const CardContentWarnings = React.memo(({ warnings }: CardContentWarningsProps) => {
    const [warningsStack, setWarningStack] = React.useState(warnings);
    const removeWarning = React.useCallback((warningToRemove: string) => {
        setWarningStack((prevWarnings) => prevWarnings.filter((w) => w !== warningToRemove));
    }, []);
    return warningsStack.length ? (
        <CardContent data-test-id="tab-view-warnings" className={`${eccgui}-dialog__notifications`}>
            {warningsStack.map((warning, idx) => {
                return (
                    <Fragment key={warning}>
                        <Notification
                            warning
                            onDismiss={(didTimeoutExpire) => !didTimeoutExpire && removeWarning(warning)}
                        >
                            {warning}
                        </Notification>
                        {idx < warningsStack.length - 1 && <Spacing size={"small"} />}
                    </Fragment>
                );
            })}
        </CardContent>
    ) : null;
});
