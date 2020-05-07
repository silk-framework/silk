import React from "react";

function OverflowText({
    className = "",
    children,
    ellipsis,
    inline = false,
    passDown = false,
    useHtmlElement,
    ...otherProps
}: any) {
    const defaultHtmlElement = inline ? "span" : "div";
    const overflowtextElement = useHtmlElement
        ? React.createElement(useHtmlElement)
        : React.createElement(defaultHtmlElement);

    return (
        <overflowtextElement.type
            className={
                "ecc-typography__overflowtext" +
                (className ? " " + className : "") +
                (ellipsis && (ellipsis === "reverse" || ellipsis === "none")
                    ? " ecc-typography__overflowtext--ellipsis-" + ellipsis
                    : "") +
                (inline ? " ecc-typography__overflowtext--inline" : "") +
                (passDown ? " ecc-typography__overflowtext--passdown" : "")
            }
        >
            {children}
        </overflowtextElement.type>
    );
}

export default OverflowText;
