import React from "react";
import { Spinner as BlueprintSpinner, Overlay as BlueprintOverlay } from "@blueprintjs/core";

function Spinner({
    className = "",
    color = "inherit", // primary | success | warning | danger | inherit | colordefinition (red, #0f0, ...)
    description = "Loading indicator", // currently unsupported (TODO)
    position = "local", // global | local | inline
    size, // tiny | small | medium | large | xlarge | inherit
    stroke, // thin | medium | bold
    ...otherProps
}: any) {
    const availableColor = ["primary", "success", "warning", "danger", "inherit"];
    const internSizes = {
        thin: 100,
        medium: 50,
        bold: 10,
    };

    const spinnerElement = position === "inline" ? "span" : "div";
    const spinnerColor = availableColor.indexOf(color) < 0 ? color : null;
    const spinnerIntent = availableColor.indexOf(color) < 0 ? "usercolor" : color;

    const availableSize = ["tiny", "small", "medium", "large", "xlarge", "inherit"];
    let spinnerSize = null;
    const availableStroke = ["thin", "medium", "bold"];
    let spinnerStroke = null;
    switch (position) {
        case "local":
            spinnerSize = "medium";
            spinnerStroke = "medium";
            break;
        case "global":
            spinnerSize = "large";
            spinnerStroke = "thin";
            break;
        case "inline":
            spinnerSize = "inherit";
            spinnerStroke = "bold";
            break;
        default:
            spinnerSize = availableSize.indexOf(size) < 0 ? "medium" : size;
            spinnerStroke = availableStroke.indexOf(stroke) < 0 ? "medium" : stroke;
    }

    let spinner = (
        <BlueprintSpinner
            size={internSizes[spinnerStroke]}
            tagName={spinnerElement}
            className={
                "ecc-spinner" +
                (className ? " " + className : "") +
                " ecc-spinner--intent-" +
                spinnerIntent +
                " ecc-spinner--size-" +
                spinnerSize
            }
            {...otherProps}
        />
    );

    if (spinnerColor) {
        spinner = <span style={{ color: spinnerColor }}>{spinner}</span>;
    }

    return position === "global" ? (
        <BlueprintOverlay
            {...otherProps}
            className="ecc-spinner__overlay"
            backdropClassName={"ecc-spinner__backdrop"}
            canOutsideClickClose={false}
            canEscapeKeyClose={false}
            hasBackdrop={true}
            isOpen={true}
        >
            {spinner}
        </BlueprintOverlay>
    ) : (
        spinner
    );
}

export default Spinner;
