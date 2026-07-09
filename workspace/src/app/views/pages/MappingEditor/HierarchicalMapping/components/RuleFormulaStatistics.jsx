import React from "react";

const RuleFormulaStatistics = ({
    introLabel,
    pathCount,
    renderPaths,
    operatorCount,
    operatorLabels,
    ruleBlockCount,
}) => (
    <span>
        {introLabel} {pathCount} value path{pathCount > 1 ? "s" : ""}: {renderPaths()}
        <br />
        and {operatorCount} operator function{operatorCount > 1 ? "s" : ""}
        {operatorCount > 0 ? (
            <>
                : <code>{operatorLabels}</code>
            </>
        ) : null}
        .
        {ruleBlockCount > 0 ? (
            <>
                <br />
                and {ruleBlockCount} rule block{ruleBlockCount > 1 ? "s" : ""}.
            </>
        ) : null}
    </span>
);

export default RuleFormulaStatistics;
