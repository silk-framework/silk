import React from "react";
import Tooltip from "../Tooltip/Tooltip";
import Icon from "../Icon/Icon";

function Label({
    children,
    className = "",
    disabled,
    text,
    info,
    tooltip,
    isLayoutForElement = "label",
    ...otherProps
}: any) {
    let htmlElementstring = isLayoutForElement;
    htmlElementstring = disabled && htmlElementstring === "label" ? "span" : htmlElementstring;
    const labelElement = React.createElement(htmlElementstring);

    return text ? (
        <labelElement.type
            className={"ecc-label" + (className ? " " + className : "") + (disabled ? " ecc-label--disabled" : "")}
            {...otherProps}
            htmlFor={disabled ? "" : otherProps.htmlFor}
        >
            <span className="ecc-label__text">{text}</span>
            {info && <span className="ecc-label__info">{info}</span>}
            {tooltip && (
                <span className="ecc-label__tooltip">
                    <Tooltip content={tooltip} disabled={disabled}>
                        <Icon name="item-info" small />
                    </Tooltip>
                </span>
            )}
            {children && <span className="ecc-label__other">{children}</span>}
        </labelElement.type>
    ) : (
        <></>
    );
}

export default Label;
