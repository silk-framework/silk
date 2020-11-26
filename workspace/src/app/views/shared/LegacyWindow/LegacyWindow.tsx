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
import "./legacywindow.scss";

const getBookmark = (locationHashPart: string) => {
    const hashParsed = locationParser.parse(locationHashPart, { parseNumbers: true });
    return !!hashParsed.legacywindow && hashParsed.legacywindow > -1 ? hashParsed.legacywindow.toString() : false;
};

const calculateBookmarkLocation = (currentLocation, indexBookmark) => {
    const hashParsed = locationParser.parse(currentLocation.hash, { parseNumbers: true });
    hashParsed["legacywindow"] = indexBookmark;
    const updatedHash = locationParser.stringify(hashParsed);
    return `${currentLocation.pathname}${currentLocation.search}#${updatedHash}`;
};

interface ILegacyWindowProps {
    // The title of the widget
    title?: string;
    // array of links to legacy gui
    legacyLinks?: IItemLink[];
    // legacy link that should initially be used when loaded, must be part of the legacy links list
    startWithLink?: IItemLink | null;
    // show initially as fullscreen
    startFullscreen?: boolean;
    // integrate only as modal that can be closed by this handler
    handlerRemoveModal?: () => void;
}

/**
 * Legacy window component includes some views (mainly editors) from the legacy GUI
 */
export function LegacyWindow({
    title,
    legacyLinks,
    startWithLink = null,
    startFullscreen = false,
    handlerRemoveModal,
    ...otherProps
}: ILegacyWindowProps) {
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

    // active legacy link
    const [activeLegacyLink, setActiveLegacyLink] = useState<IItemLink | null>(startWithLink);
    // handler for link change
    const toggleLegacyLink = (linkItem) => {
        setActiveLegacyLink(linkItem);
        if (!startWithLink) {
            dispatch(history.push(calculateBookmarkLocation(location, itemLinks.indexOf(linkItem))));
        }
    };

    const getInitialActiveLink = (itemLinks) => {
        if (activeLegacyLink) return activeLegacyLink;
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
            const legacyLinks = data.filter((item) => !item.path.startsWith(SERVE_PATH));
            setItemLinks(legacyLinks);
            setActiveLegacyLink(getInitialActiveLink(legacyLinks));
        } catch (e) {}
    };
    useEffect(() => {
        if (!!legacyLinks && legacyLinks.length > 0) {
            setItemLinks(legacyLinks);
            setActiveLegacyLink(getInitialActiveLink(legacyLinks));
        } else if (projectId && taskId) {
            getItemLinks();
        } else {
            setItemLinks([]);
        }
    }, [projectId, taskId]);

    const tLabel = (label) => {
        return t("common.legacyGui." + label, label);
    };

    const iframeWidget = (
        <Card isOnlyLayout={true} elevation={displayFullscreen ? 4 : 1}>
            <CardHeader>
                <CardTitle>
                    <h2>{!!title ? title : !!activeLegacyLink ? tLabel(activeLegacyLink.label) : ""}</h2>
                </CardTitle>
                <CardOptions>
                    {itemLinks.length > 1 &&
                        itemLinks.map((itemLink) => (
                            <Button
                                key={itemLink.path}
                                onClick={() => {
                                    toggleLegacyLink(itemLink);
                                }}
                                minimal={true}
                                disabled={!!activeLegacyLink && activeLegacyLink.path === itemLink.path}
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
                {!!activeLegacyLink ? (
                    <iframe
                        src={activeLegacyLink.path + "?inlineView=true"}
                        title={tLabel(activeLegacyLink.label)}
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
        <section className={"diapp-legacywindow"} {...otherProps}>
            <div className="diapp-legacywindow__placeholder" />
            <div className={displayFullscreen ? "diapp-legacywindow--fullscreen" : ""}>{iframeWidget}</div>
        </section>
    );
}
