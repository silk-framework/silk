import React from "react";
import { Draggable } from "react-beautiful-dnd";
import { MappingRule } from "./MappingRule";
import { ScrollingHOC } from "gui-elements-deprecated";

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

class DraggableItem extends React.Component {
    constructor(props) {
        super(props);
        this.expandedRuleRef = React.createRef();
    }

    state = {
        isPasted: isPasted(this.props.id),
        expanded: isExpanded(this.props.id) || isPasted(this.props.id),
    };

    componentDidMount() {
        if (this.state.isPasted) {
            sessionStorage.removeItem("pastedId");
            this.props.scrollIntoView();
        }

        const searchQuery = new URLSearchParams(window.location.search).get("ruleId");
        if (searchQuery === this.props.id) {
            this.setState({ expanded: true });
            this.props.scrollIntoView();
        }
    }

    updateHistory = (ruleId) => {
        if (!this.props.startFullScreen) {
            const history = getHistory();
            history.replace({
                search: `?${new URLSearchParams({ ruleId })}`,
            });
        }
    };

    updateQueryOnExpansion() {
        if (this.state.expanded) {
            this.updateHistory(this.props.id);
            this.props.scrollIntoView();
        } else {
            this.updateHistory(this.props.parentRuleId ?? "");
        }
    }

    handleExpand = (expanded = !this.state.expanded, id = true) => {
        // only trigger state / render change if necessary
        if (
            expanded !== this.state.expanded &&
            this.props.type !== MAPPING_RULE_TYPE_OBJECT &&
            (id === true || id === this.props.id)
        ) {
            this.setState(
                {
                    expanded,
                },
                this.updateQueryOnExpansion,
            );
        }
    };

    render() {
        return (
            <Draggable
                isDragDisabled={this.state.expanded}
                style={{ width: "15" }}
                key={this.props.id}
                draggableId={`draggable-${this.props.id}`}
                index={this.props.pos}
            >
                {(provided, snapshot) => (
                    <MappingRule
                        provided={provided}
                        snapshot={snapshot}
                        isPasted={this.state.isPasted}
                        expanded={this.state.expanded}
                        onExpand={this.handleExpand}
                        refFromParent={this.expandedRuleRef}
                        onOrderRules={this.props.onOrderRules}
                        updateHistory={this.updateHistory}
                        mapRuleLoading={this.props.mapRuleLoading}
                        viewActions={this.props.viewActions}
                        {...this.props}
                    />
                )}
            </Draggable>
        );
    }
}

export default ScrollingHOC(DraggableItem);
