import React from "react";
import { Sparklines, SparklinesBars } from "react-sparklines";

export function SimpleDiagram({ data, titlePrefix = "Data", ...restProps }: any) {
    if (data.length < 1) {
        return <></>;
    }

    return (
        <div className="app-di-simplediagram" title={titlePrefix + ": " + data.join(", ")}>
            <Sparklines data={data} {...restProps}>
                <SparklinesBars
                    barWidth={data.length > 1 ? 0 : { ...restProps }.width}
                    style={{
                        stroke: "transparent",
                        strokeWidth: "0",
                        fill: "inherit",
                        opacity: "0.61",
                    }}
                />
            </Sparklines>
        </div>
    );
}
