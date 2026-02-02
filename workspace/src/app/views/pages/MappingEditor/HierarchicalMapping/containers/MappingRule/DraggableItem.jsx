import React, { useState, useEffect, useRef, useCallback } from "react";
import { useSortable } from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";
import { MappingRule } from "./MappingRule";
import { ScrollingHOC } from "../../utils/ScrollingHOC";

import { URI } from "ecc-utils";
import { MAPPING_RULE_TYPE_OBJECT } from "../../utils/constants";
import { getHistory } from "../../../../../../store/configureStore";

const isPasted = (id) => {
    const pastedId = sessionStorage.getItem("pastedId");
    return pastedId !== null && pastedId === id;
};

const isExpanded = (id) => {
    const uriTemplate = new URI(window.location.href);
    if (uriTemplate.segment(-2) === "rule") {
        return uriTemplate.segment(-1) === id;
    }
};

const DraggableItem = (props) => {
    const expandedRuleRef = useRef();
    const [isPastedState, setIsPastedState] = useState(isPasted(props.id));
    const [expanded, setExpanded] = useState(isExpanded(props.id) || isPasted(props.id));

    const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({
        id: `draggable-${props.id}`,
        disabled: expanded,
    });

    const style = {
        transform: CSS.Transform.toString(transform),
        transition,
    };

    useEffect(() => {
        if (isPastedState) {
            sessionStorage.removeItem("pastedId");
            props.scrollIntoView();
        }

        const searchQuery = new URLSearchParams(window.location.search).get("ruleId");
        if (searchQuery === props.id) {
            setExpanded(true);
            props.scrollIntoView();
        }
    }, []); // Only run on mount

    const updateHistory = useCallback(
        (ruleId) => {
            if (!props.startFullScreen) {
                const history = getHistory();
                history.replace({
                    search: `?${new URLSearchParams({ ruleId })}`,
                });
            }
        },
        [props.startFullScreen],
    );

    const updateQueryOnExpansion = useCallback(() => {
        if (expanded) {
            updateHistory(props.id);
            props.scrollIntoView();
        } else {
            updateHistory(props.parentRuleId ?? "");
        }
    }, [expanded, props.id, props.parentRuleId, props.scrollIntoView, updateHistory]);

    const handleExpand = useCallback(
        (newExpanded = !expanded, id = true) => {
            // only trigger state / render change if necessary
            if (
                newExpanded !== expanded &&
                props.type !== MAPPING_RULE_TYPE_OBJECT &&
                (id === true || id === props.id)
            ) {
                setExpanded(newExpanded);
            }
        },
        [expanded, props.type, props.id],
    );

    // Call updateQueryOnExpansion when expanded changes
    useEffect(() => {
        updateQueryOnExpansion();
    }, [expanded]);

    // Create provided and snapshot objects compatible with the existing MappingRule component
    const provided = {
        innerRef: setNodeRef,
        draggableProps: {
            style,
            ...attributes,
        },
        dragHandleProps: listeners,
    };

    const snapshot = {
        isDragging,
    };

    return (
        <MappingRule
            provided={provided}
            snapshot={snapshot}
            isPasted={isPastedState}
            expanded={expanded}
            onExpand={handleExpand}
            refFromParent={expandedRuleRef}
            onOrderRules={props.onOrderRules}
            updateHistory={updateHistory}
            mapRuleLoading={props.mapRuleLoading}
            viewActions={props.viewActions}
            {...props}
        />
    );
};

export default ScrollingHOC(DraggableItem);
