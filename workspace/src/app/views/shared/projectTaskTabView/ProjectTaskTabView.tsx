import React, { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { useHistory, useLocation } from "react-router";
import { useTranslation } from "react-i18next";
import locationParser from "query-string";
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
    Spacing,
} from "@gui-elements/index";
import { IItemLink } from "@ducks/shared/typings";
import { requestItemLinks } from "@ducks/shared/requests";
import { commonSel } from "@ducks/common";
import Loading from "../Loading";
import { HOST, SERVE_PATH } from "../../../constants/path";
import "./projectTaskTabView.scss";
import { IProjectTaskView, IViewActions, viewRegistry } from "../../registry/ViewRegistry";
import * as H from "history";

// Get the bookmark value
const getBookmark = (locationHashPart: string, maxIdx: number): number | undefined => {
    const hashParsed = locationParser.parse(locationHashPart, { parseNumbers: true });
    return typeof hashParsed.viewIdx === "number" && hashParsed.viewIdx > -1 && hashParsed.viewIdx <= maxIdx
        ? hashParsed.viewIdx
        : undefined;
};

// Returns the bookmark location for the currently selected tab
const calculateBookmarkLocation = (currentLocation: H.Location, indexBookmark: number) => {
    const hashParsed = locationParser.parse(currentLocation.hash, { parseNumbers: true });
    hashParsed["viewIdx"] = indexBookmark;
    const updatedHash = locationParser.stringify(hashParsed);
    return `${currentLocation.pathname}${currentLocation.search}#${updatedHash}`;
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
}

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
    viewActions,
    ...otherProps
}: IProjectTaskTabView) {
    const projectId = taskViewConfig?.projectId ?? useSelector(commonSel.currentProjectIdSelector);
    const taskId = taskViewConfig?.taskId ?? useSelector(commonSel.currentTaskIdSelector);
    const dispatch = useDispatch();
    const history = useHistory();
    const location = useLocation();
    const [t] = useTranslation();
    const [isFetchingLinks, setIsFetchingLinks] = useState(true);
    const iframeRef = React.useRef<HTMLIFrameElement>(null);
    // Keeps track of the loaded path of the i-frame. Only the case when selected tab is an item link.
    const [activeIframePath, setActiveIframePath] = React.useState<IItemLink | undefined>(undefined);
    // active link. Either an item link or a view ID
    const [selectedTab, setSelectedTab] = useState<IItemLink | string | undefined>(startWithLink);
    // list of aggregated links
    const [itemLinks, setItemLinks] = useState<IItemLink[]>([]);
    const [taskViews, setTaskViews] = React.useState<IProjectTaskView[]>([]);
    // Stores request to change the tab. The requests is represented by the tab idx.
    const [tabIdxChangeRequest, setTabIdxChangeRequest] = React.useState<number | undefined>(undefined);

    const viewsAndItemLink: Partial<IProjectTaskView & IItemLink>[] = [...taskViews, ...itemLinks];
    const isTaskView = (viewOrItemLink: Partial<IProjectTaskView & IItemLink>) => !!viewOrItemLink.id;

    const itemLinkActive = selectedTab != null && typeof selectedTab !== "string";
    // Either the path value of an IItemLink or the view ID or undefined
    const activeLabel: string | undefined =
        activeIframePath?.label ?? itemLinkActive
            ? (selectedTab as IItemLink).label
            : taskViews.find((v) => v.id === selectedTab)?.label;

    React.useEffect(() => {
        if (projectId && taskId && taskViewConfig?.pluginId) {
            setTaskViews(viewRegistry.projectTaskViews(taskViewConfig.pluginId));
        }
    }, [projectId, taskId, taskViewConfig?.pluginId]);

    React.useEffect(() => {
        if (
            tabIdxChangeRequest != null &&
            getBookmark(location.hash, viewsAndItemLink.length - 1) === tabIdxChangeRequest
        ) {
            const tabItem = viewsAndItemLink[tabIdxChangeRequest];
            if (isTaskView(tabItem)) {
                setSelectedTab(tabItem.id);
                setActiveIframePath(undefined);
            } else {
                //if path already set, reload iframe source
                if (typeof selectedTab !== "string") {
                    if (tabItem.path && tabItem.path === selectedTab?.path && iframeRef.current) {
                        iframeRef.current.src = createIframeUrl(tabItem.path);
                    }
                } else {
                    setSelectedTab(tabItem as IItemLink);
                }
            }
            setTabIdxChangeRequest(undefined);
        }
    }, [tabIdxChangeRequest, location.hash]);

    // flag if the widget is shown as fullscreen modal
    const [displayFullscreen, setDisplayFullscreen] = useState(!!handlerRemoveModal || startFullscreen);
    // handler for toggling fullscreen mode
    const toggleFullscreen = () => {
        setDisplayFullscreen(!displayFullscreen);
    };

    // handler for link change. Triggers a tab change request. Actual change is done in useEffect.
    const changeTab = (tabItem: IItemLink | string) => {
        if (!startWithLink) {
            const tabIdx =
                typeof tabItem === "string"
                    ? viewsAndItemLink.findIndex((elem) => elem.id === tabItem)
                    : viewsAndItemLink.indexOf(tabItem);
            setTabIdxChangeRequest(tabIdx);
            dispatch(history.push(calculateBookmarkLocation(location, tabIdx)));
        }
    };

    const getInitialActiveLink = (itemLinks) => {
        const locationHashBookmark = getBookmark(location.hash, itemLinks.length + taskViews.length - 1) ?? 0;
        return locationHashBookmark < taskViews.length
            ? taskViews[locationHashBookmark].id
            : itemLinks[locationHashBookmark - taskViews.length];
    };

    React.useEffect(() => {
        if (iframeRef.current && selectedTab && itemLinks.length && taskViews) {
            const iframeLoadHandler = () => {
                if (iframeRef.current) {
                    const regex = new RegExp(HOST + "|\\?.*", "gi");
                    const parsedCurrentIframePath = iframeRef.current.src.replace(regex, "");
                    const focusedIframeSource = itemLinks.find((link) => link.path === parsedCurrentIframePath);
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

    // update item links by rest api request
    const getItemLinks = async (projectId: string, taskId: string) => {
        setIsFetchingLinks(true);
        try {
            const { data } = await requestItemLinks(projectId, taskId);
            // remove current page link
            const srcLinks = data.filter((item) => !item.path.startsWith(SERVE_PATH));
            setItemLinks(srcLinks);
            setSelectedTab(getInitialActiveLink(srcLinks));
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
                getItemLinks(projectId, taskId);
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

    const iframeWidget = () => {
        return (
            <Card className="diapp-iframewindow__content" isOnlyLayout={true} elevation={displayFullscreen ? 4 : 1}>
                <CardHeader>
                    <CardTitle>
                        <h2>{!!title ? title : !!selectedTab && activeLabel ? tLabel(activeLabel) : ""}</h2>
                    </CardTitle>
                    <CardOptions>
                        {viewsAndItemLink.length > 1 &&
                            viewsAndItemLink.map((tabItem) => (
                                <Button
                                    key={tabItem.id ?? tabItem.path}
                                    onClick={() => {
                                        changeTab(tabItem.id ?? (tabItem as IItemLink));
                                    }}
                                    minimal={true}
                                    disabled={activeIframePath?.path === tabItem.path || tabItem.id === selectedTab}
                                >
                                    {tLabel(tabItem.label as string)}
                                </Button>
                            ))}
                        {!!handlerRemoveModal ? (
                            <IconButton name="navigation-close" onClick={handlerRemoveModal} />
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
                                name={otherProps.iFrameName}
                                src={createIframeUrl((selectedTab as IItemLink)?.path)}
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
                            taskViews.find((v) => v.id === selectedTab)?.render(projectId, taskId, viewActions)
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
            </Card>
        );
    };

    return !!handlerRemoveModal ? (
        <Modal size="fullscreen" isOpen={true} canEscapeKeyClose={true} onClose={handlerRemoveModal}>
            {iframeWidget()}
        </Modal>
    ) : (
        <section className={"diapp-iframewindow"} {...otherProps}>
            <div className="diapp-iframewindow__placeholder">
                <Grid fullWidth={true}>
                    <GridRow fullHeight={true} />
                </Grid>
            </div>
            <div className={displayFullscreen ? "diapp-iframewindow--fullscreen" : "diapp-iframewindow--inside"}>
                {iframeWidget()}
            </div>
        </section>
    );
}
