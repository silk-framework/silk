import React, { useState, useEffect } from "react";
import { useDispatch, useSelector } from "react-redux";
import { useLocation, useHistory } from "react-router";
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
    IconButton,
    Modal,
} from "@gui-elements/index";
import { IItemLink } from "@ducks/shared/typings";
import { requestItemLinks } from "@ducks/shared/requests";
import { commonSel } from "@ducks/common";
import Loading from "../Loading";
import { SERVE_PATH } from "../../../constants/path";
import "./iframewindow.scss";

const getBookmark = (locationHashPart: string) => {
    const hashParsed = locationParser.parse(locationHashPart, { parseNumbers: true });
    return !!hashParsed.iframewindow && hashParsed.iframewindow > -1 ? hashParsed.iframewindow.toString() : false;
};

const calculateBookmarkLocation = (currentLocation, indexBookmark) => {
    const hashParsed = locationParser.parse(currentLocation.hash, { parseNumbers: true });
    hashParsed["iframewindow"] = indexBookmark;
    const updatedHash = locationParser.stringify(hashParsed);
    return `${currentLocation.pathname}${currentLocation.search}#${updatedHash}`;
};

interface IIframeWindowProps {
    // The title of the widget
    title?: string;
    // array of URLs, each one will be extended by parameter inlineView=true
    srcLinks?: IItemLink[];
    // URL that should initially be used when loaded, must be part of srcLinks
    startWithLink?: IItemLink | null;
    // show initially as fullscreen
    startFullscreen?: boolean;
    // integrate only as modal that can be closed by this handler
    handlerRemoveModal?: () => void;
}

/**
 * Component can display views from other application, integrated by iframe
 */
export function IframeWindow({
    title,
    srcLinks,
    startWithLink = null,
    startFullscreen = false,
    handlerRemoveModal,
    ...otherProps
}: IIframeWindowProps) {
    const projectId = useSelector(commonSel.currentProjectIdSelector);
    const taskId = useSelector(commonSel.currentTaskIdSelector);
    const dispatch = useDispatch();
    const history = useHistory();
    const location = useLocation();
    const [t] = useTranslation();

    // flag if the widget is shown as fullscreen modal
    const [displayFullscreen, setDisplayFullscreen] = useState(!!handlerRemoveModal || startFullscreen);
    // handler for toggling fullscreen mode
    const toggleFullscreen = () => {
        setDisplayFullscreen(!displayFullscreen);
    };

    // active link
    const [activeSource, setActiveSource] = useState<IItemLink | null>(startWithLink);
    // handler for link change
    const toggleIframeSource = (linkItem) => {
        setActiveSource(linkItem);
        if (!startWithLink) {
            dispatch(history.push(calculateBookmarkLocation(location, itemLinks.indexOf(linkItem))));
        }
    };

    const getInitialActiveLink = (itemLinks) => {
        if (activeSource) return activeSource;
        const locationHashBookmark = getBookmark(location.hash);
        return locationHashBookmark ? itemLinks[locationHashBookmark] : itemLinks[0];
    };

    // list of aggregated links
    const [itemLinks, setItemLinks] = useState<IItemLink[]>([]);
    // update item links by rest api request
    const getItemLinks = async () => {
        try {
            const { data } = await requestItemLinks(projectId, taskId);
            // remove current page link
            const srcLinks = data.filter((item) => !item.path.startsWith(SERVE_PATH));
            setItemLinks(srcLinks);
            setActiveSource(getInitialActiveLink(srcLinks));
        } catch (e) {}
    };
    useEffect(() => {
        if (!!srcLinks && srcLinks.length > 0) {
            setItemLinks(srcLinks);
            setActiveSource(getInitialActiveLink(srcLinks));
        } else if (projectId && taskId) {
            getItemLinks();
        } else {
            setItemLinks([]);
        }
    }, [projectId, taskId]);

    const tLabel = (label) => {
        return t("common.iframeWindow." + label, label);
    };

    const iframeWidget = (
        <Card isOnlyLayout={true} elevation={displayFullscreen ? 4 : 1}>
            <CardHeader>
                <CardTitle>
                    <h2>{!!title ? title : !!activeSource ? tLabel(activeSource.label) : ""}</h2>
                </CardTitle>
                <CardOptions>
                    {itemLinks.length > 1 &&
                        itemLinks.map((itemLink) => (
                            <Button
                                key={itemLink.path}
                                onClick={() => {
                                    toggleIframeSource(itemLink);
                                }}
                                minimal={true}
                                disabled={!!activeSource && activeSource.path === itemLink.path}
                            >
                                {tLabel(itemLink.label)}
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
            <CardContent style={{ padding: 0, position: "relative" }}>
                {!!activeSource ? (
                    <iframe
                        src={activeSource.path + "?inlineView=true"}
                        title={tLabel(activeSource.label)}
                        style={{
                            position: "absolute",
                            width: "100%",
                            height: "100%",
                        }}
                    />
                ) : (
                    <Loading />
                )}
            </CardContent>
        </Card>
    );

    return !!handlerRemoveModal ? (
        <Modal size="fullscreen" isOpen={true} canEscapeKeyClose={true} onClose={handlerRemoveModal}>
            {iframeWidget}
        </Modal>
    ) : (
        <section className={"diapp-iframewindow"} {...otherProps}>
            <div className="diapp-iframewindow__placeholder" />
            <div className={displayFullscreen ? "diapp-iframewindow--fullscreen" : ""}>{iframeWidget}</div>
        </section>
    );
}
