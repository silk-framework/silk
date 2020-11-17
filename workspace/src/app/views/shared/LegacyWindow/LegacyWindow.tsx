import React, { useState, useEffect } from "react";
import { useSelector } from "react-redux";
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
 * TODO
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
            if (!activeLegacyLink) setActiveLegacyLink(legacyLinks[0]);
        } catch (e) {}
    };
    useEffect(() => {
        if (!!legacyLinks && legacyLinks.length > 0) {
            setItemLinks(legacyLinks);
            if (!activeLegacyLink) setActiveLegacyLink(legacyLinks[0]);
        } else if (projectId && taskId) {
            getItemLinks();
        } else {
            setItemLinks([]);
        }
    }, [projectId, taskId]);

    const iframeWidget = (
        <Card isOnlyLayout={true} elevation={displayFullscreen ? 4 : 1}>
            <CardHeader>
                <CardTitle>
                    <h2>{!!title ? title : !!activeLegacyLink ? activeLegacyLink.label : ""}</h2>
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
                                {itemLink.label}
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
                        title={activeLegacyLink.label}
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
