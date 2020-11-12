import React, { useState } from "react";
import { Card, CardContent, CardHeader, CardOptions, CardTitle, Divider, IconButton } from "@gui-elements/index";
import "./legacywindow.scss";

interface ILegacyWindowProps {
    // The title of the widget
    title: string;
}

/**
 * TODO
 */
export function LegacyWindow({ title, ...otherProps }: ILegacyWindowProps) {
    // flag if the widget is shown as fullscreen modal
    const [displayFullscreen, setDisplayFullscreen] = useState(false);

    const toggleFullscreen = () => {
        setDisplayFullscreen(!displayFullscreen);
    };

    const iframeWidget = (
        <Card isOnlyLayout={true} elevation={displayFullscreen ? 4 : 1}>
            <CardHeader>
                <CardTitle>
                    <h2>{title}</h2>
                </CardTitle>
                <CardOptions>
                    <IconButton
                        name={displayFullscreen ? "toggler-minimize" : "toggler-maximize"}
                        onClick={toggleFullscreen}
                    />
                </CardOptions>
            </CardHeader>
            <Divider />
            <CardContent style={{ padding: 0, position: "relative" }}>
                <iframe
                    src="http://localhost:9000/"
                    title={title}
                    style={{
                        position: "absolute",
                        width: "100%",
                        height: "100%",
                    }}
                />
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
