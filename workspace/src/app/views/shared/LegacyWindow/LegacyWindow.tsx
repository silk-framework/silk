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
}

/**
 * TODO
 */
export function LegacyWindow({ title, ...otherProps }: ILegacyWindowProps) {
    const projectId = useSelector(commonSel.currentProjectIdSelector);
    const taskId = useSelector(commonSel.currentTaskIdSelector);

    // flag if the widget is shown as fullscreen modal
    const [displayFullscreen, setDisplayFullscreen] = useState(false);
    // handler for toggling fullscreen mode
    const toggleFullscreen = () => {
        setDisplayFullscreen(!displayFullscreen);
    };

    // list of aggregated links
    const [itemLinks, setItemLinks] = useState<IItemLink[]>([]);
    // Update item links for more menu
    useEffect(() => {
        //getWindowTitle(projectId);
        if (projectId && taskId) {
            getItemLinks();
        } else {
            setItemLinks([]);
        }
    }, [projectId, taskId]);
    const getItemLinks = async () => {
        try {
            const { data } = await requestItemLinks(projectId, taskId);
            // remove current page link
            const legacyLinks = data.filter((item) => !item.path.startsWith(SERVE_PATH));
            setItemLinks(legacyLinks);
            setActiveLegacyLink(legacyLinks[0]);
        } catch (e) {}
    };

    // active legacy link
    const [activeLegacyLink, setActiveLegacyLink] = useState<IItemLink | null>(null);
    // handler for link change
    const toggleLegacyLink = (linkItem) => {
        setActiveLegacyLink(linkItem);
    };

    const iframeWidget = (
        <Card isOnlyLayout={true} elevation={displayFullscreen ? 4 : 1}>
            <CardHeader>
                <CardTitle>
                    <h2>{!!title ? title : !!activeLegacyLink ? activeLegacyLink.label : ""}</h2>
                    {!!activeLegacyLink ? (
                        <IconButton name="item-launch" href={activeLegacyLink.path} target="_blank" />
                    ) : (
                        <></>
                    )}
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
                    <IconButton
                        name={displayFullscreen ? "toggler-minimize" : "toggler-maximize"}
                        onClick={toggleFullscreen}
                    />
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

    return (
        <section className={"diapp-legacywindow"} {...otherProps}>
            <div className="diapp-legacywindow__placeholder" />
            <div className={displayFullscreen ? "diapp-legacywindow--fullscreen" : ""}>{iframeWidget}</div>
        </section>
    );
}
