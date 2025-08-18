import React from "react";
import _ from "lodash";
import className from "classnames";
import { NotAvailable } from "gui-elements-deprecated";
import { Card, CardHeader, CardTitle, CardOptions, Divider } from "@eccenca/gui-elements";
import { ThingIcon } from "../components/ThingIcon";
import RuleTitle from "../elements/RuleTitle";
import RuleTypes from "../elements/RuleTypes";
import ObjectRule from "./MappingRule/ObjectRule/ObjectRule";
import { MAPPING_RULE_TYPE_COMPLEX_URI, MAPPING_RULE_TYPE_URI, MESSAGES } from "../utils/constants";
import EventEmitter from "../utils/EventEmitter";
import ExpandButton from "../elements/buttons/ExpandButton";
import { getHistory } from "../../../../../store/configureStore";

/** The top rule (root or object) for a specific level in the mapping hierarchy. */
class RootMappingRule extends React.Component {
    state = {
        expanded: false,
        editing: false,
    };

    constructor(props) {
        super(props);
        this.handleRuleToggle = this.handleRuleToggle.bind(this);
        this.onOpenEdit = this.onOpenEdit.bind(this);
        this.onCloseEdit = this.onCloseEdit.bind(this);
        this.handleDiscardChanges = this.handleDiscardChanges.bind(this);
        this.handleToggleExpand = this.handleToggleExpand.bind(this);
        this.discardAll = this.discardAll.bind(this);
        this.expandedRuleRef = React.createRef();
    }

    componentDidMount() {
        EventEmitter.on(MESSAGES.RULE_VIEW.TOGGLE, this.handleRuleToggle);
        EventEmitter.on(MESSAGES.RULE_VIEW.CHANGE, this.onOpenEdit);
        EventEmitter.on(MESSAGES.RULE_VIEW.UNCHANGED, this.onCloseEdit);
        EventEmitter.on(MESSAGES.RULE_VIEW.DISCARD_ALL, this.discardAll);

        const searchQuery = new URLSearchParams(window.location.search).get("ruleId");
        if (searchQuery === this.props.rule.id) {
            this.setState({ expanded: true });
            this.expandedRuleRef.current?.scrollIntoView({ behavior: "smooth" });
        }
    }

    componentWillUnmount() {
        EventEmitter.off(MESSAGES.RULE_VIEW.TOGGLE, this.handleRuleToggle);
        EventEmitter.off(MESSAGES.RULE_VIEW.CHANGE, this.onOpenEdit);
        EventEmitter.off(MESSAGES.RULE_VIEW.UNCHANGED, this.onCloseEdit);
        EventEmitter.off(MESSAGES.RULE_VIEW.DISCARD_ALL, this.discardAll);
    }

    handleRuleToggle({ expanded, id }) {
        // only trigger state / render change if necessary
        if ((id === true || id === this.props.rule.id) && expanded !== this.state.expanded) {
            this.setState({ expanded });
        }
    }

    onOpenEdit(obj) {
        if (this.props.rule.id === obj.id) {
            this.setState({
                editing: true,
            });
        }
    }

    onCloseEdit(obj) {
        if (this.props.rule.id === obj.id) {
            this.setState({
                editing: false,
            });
        }
    }

    handleDiscardChanges() {
        this.setState({
            expanded: !this.state.expanded,
        });
        this.props.onAskDiscardChanges(false);
        EventEmitter.emit(MESSAGES.RULE_VIEW.UNCHANGED, { id: this.props.rule.id });
    }

    updateQueryOnExpansion() {
        const history = getHistory();
        if (this.state.expanded) {
            !this.props.startFullScreen &&
                history.replace({
                    search: `?${new URLSearchParams({ ruleId: this.props.rule.id })}`,
                });
            this.expandedRuleRef.current?.scrollIntoView({ behavior: "smooth" });
        }
        // Collapsing the object rule should have no effect on the currently selected rule
    }

    handleToggleExpand() {
        if (this.state.editing) {
            this.props.onAskDiscardChanges(true);
        } else {
            this.setState(
                {
                    expanded: !this.state.expanded,
                },
                this.updateQueryOnExpansion,
            );
        }
    }

    discardAll() {
        this.setState({
            editing: false,
        });
    }

    render() {
        if (_.isEmpty(this.props.rule)) {
            return false;
        }
        const breadcrumbs = _.get(this.props, "rule.breadcrumbs", []);
        const parent = _.last(breadcrumbs);

        let uriPattern = <NotAvailable label="automatic default pattern" inline />;

        const uriRuleType = _.get(this.props.rule.rules, "uriRule.type", false);
        if (uriRuleType === MAPPING_RULE_TYPE_URI) {
            uriPattern = _.get(this, "props.rule.rules.uriRule.pattern");
        } else if (uriRuleType === MAPPING_RULE_TYPE_COMPLEX_URI) {
            uriPattern = "URI formula";
        }

        return (
            <div data-test-id="root-mapping-rule" className="ecc-silk-mapping__rulesobject" ref={this.expandedRuleRef}>
                <Card elevated={!this.state.expanded} interactive={!this.state.expanded}>
                    <CardHeader onClick={this.handleToggleExpand}>
                        <CardTitle>
                            <div className="ecc-silk-mapping__ruleitem" style={{ width: "100%" }}>
                                <div
                                    className={className("ecc-silk-mapping__ruleitem-summary", {
                                        "ecc-silk-mapping__ruleitem-summary--expanded": this.state.expanded,
                                    })}
                                    style={{ backgroundColor: "transparent" }}
                                >
                                    <div className="mdl-list__item" style={{ padding: "0", cursor: "inherit" }}>
                                        <div className="mdl-list__item-primary-content">
                                            <div className="ecc-silk-mapping__ruleitem-headline">
                                                <ThingIcon type="object" />
                                                <RuleTitle
                                                    rule={this.props.rule}
                                                    className="ecc-silk-mapping__rulesobject__title-property"
                                                />
                                            </div>
                                            <RuleTypes
                                                rule={this.props.rule}
                                                className="ecc-silk-mapping__ruleitem-subline ecc-silk-mapping__rulesobject__title-type"
                                            />
                                            <div className="ecc-silk-mapping__ruleitem-subline ecc-silk-mapping__rulesobject__title-uripattern">
                                                {uriPattern}
                                            </div>
                                        </div>
                                        <div className="mdl-list__item-secondary-content" key="action"></div>
                                    </div>
                                </div>
                            </div>
                        </CardTitle>
                        <CardOptions>
                            <ExpandButton
                                id={this.props.rule.id}
                                expanded={this.state.expanded}
                                onToggle={this.handleToggleExpand}
                            />
                        </CardOptions>
                    </CardHeader>
                    {this.state.expanded && (
                        <>
                            <Divider />
                            <ObjectRule
                                ruleData={this.props.rule}
                                parentId={_.get(parent, "id", "")}
                                parent={parent}
                                edit={false}
                                handleCopy={this.props.handleCopy}
                                handleClone={this.props.handleClone}
                                onClickedRemove={this.props.onClickedRemove}
                                openMappingEditor={this.props.openMappingEditor}
                                type={this.props.rule.type}
                                viewActions={this.props.viewActions}
                            />
                        </>
                    )}
                </Card>
            </div>
        );
    }
}

export default RootMappingRule;
