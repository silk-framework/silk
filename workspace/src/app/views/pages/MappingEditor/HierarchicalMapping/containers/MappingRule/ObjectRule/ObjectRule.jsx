import React from "react";
import PropTypes from "prop-types";
import { CardActions, CardContent, Divider } from "@eccenca/gui-elements";
import _ from "lodash";
import { getEditorHref, updateObjectMappingAsync } from "../../../store";
import ObjectMappingRuleForm from "./ObjectRuleForm";

import { isClonableRule, isCopiableRule, isObjectRule, isRootRule, MESSAGES } from "../../../utils/constants";
import transformRuleOfObjectMapping from "../../../utils/transformRuleOfObjectMapping";
import EventEmitter from "../../../utils/EventEmitter";

import EditButton from "../../../elements/buttons/EditButton";
import CopyButton from "../../../elements/buttons/CopyButton";
import CloneButton from "../../../elements/buttons/CloneButton";
import DeleteButton from "../../../elements/buttons/DeleteButton";
import TargetProperty from "../../../components/TargetProperty";
import ObjectEntityRelation from "../../../components/ObjectMapping/ObjectEntityRelation";
import ObjectTypeRules from "../../../components/ObjectMapping/ObjectTypeRules";
import ObjectSourcePath from "../../../components/ObjectMapping/ObjectSourcePath";
import ObjectUriPattern from "../../../components/ObjectMapping/ObjectUriPattern";
import ExampleTarget from "../../../components/ExampleTarget";
import MetadataLabel from "../../../components/Metadata/MetadataLabel";
import MetadataDesc from "../../../components/Metadata/MetadataDesc";
import { SourcePath } from "../../../components/SourcePath";
import TargetCardinality from "../../../components/TargetCardinality";
import { defaultUriPattern } from "./ObjectRule.utils";

class ObjectRule extends React.Component {
    static propTypes = {
        parentId: PropTypes.string.isRequired,
        parent: PropTypes.object,
        openMappingEditor: PropTypes.func,
        edit: PropTypes.oneOfType([PropTypes.bool, PropTypes.object]).isRequired,
        ruleData: PropTypes.object.isRequired,
    };

    state = {
        edit: !!this.props.edit,
        href: "",
    };

    constructor(props) {
        super(props);
        this.removeUriRule = this.removeUriRule.bind(this);
        this.handleEdit = this.handleEdit.bind(this);
        this.handleCloseEdit = this.handleCloseEdit.bind(this);
        this.handleCopy = this.handleCopy.bind(this);
        this.handleClone = this.handleClone.bind(this);
    }

    componentDidMount() {
        EventEmitter.on(MESSAGES.RULE_VIEW.CLOSE, this.handleCloseEdit);
        if (_.has(this.props, "ruleData.rules.uriRule.id")) {
            this.setState({
                href: getEditorHref(this.props.ruleData.rules.uriRule.id),
            });
        }
    }

    componentWillUnmount() {
        EventEmitter.off(MESSAGES.RULE_VIEW.CLOSE, this.handleCloseEdit);
    }

    componentDidUpdate(prevProps, prevState, snapshot) {
        if (_.has(this.props, "ruleData.rules.uriRule.id")) {
            const newUrl = getEditorHref(this.props.ruleData.rules.uriRule.id);
            if (this.state.href !== newUrl) {
                this.setState({
                    href: newUrl,
                });
            }
        } else if (this.state.href !== "") {
            this.setState({
                href: "",
            });
        }
    }

    componentWillReceiveProps(nextProps) {
        if (_.has(nextProps, "ruleData.rules.uriRule.id")) {
            this.setState({
                href: getEditorHref(_.get(nextProps, "ruleData.rules.uriRule.id", "")),
            });
        }
    }

    openEditor = () => {
        let uriRuleId = _.get(this.props.ruleData, "rules.uriRule")?.id;
        if (!uriRuleId) {
            const rule = _.cloneDeep(this.props.ruleData);
            rule.rules.uriRule = {
                type: "uri",
                pattern: defaultUriPattern(rule.id),
            };
            updateObjectMappingAsync(rule).subscribe(
                (data) => {
                    this.props.openMappingEditor(data.body.rules.uriRule.id);
                },
                (err) => {
                    console.error(err);
                }
            );
        } else {
            this.props.openMappingEditor(uriRuleId);
        }

        return false;
    };

    removeUriRule() {
        const rule = _.cloneDeep(this.props.ruleData);
        const callbackFn = () => {
            rule.rules.uriRule = null;
            updateObjectMappingAsync(rule).subscribe(
                () => {
                    EventEmitter.emit(MESSAGES.RELOAD, true);
                },
                (err) => {
                    console.error(err);
                }
            );
        };
        this.props.onClickedRemove(null, callbackFn);
        return false;
    }

    handleEdit() {
        this.setState({
            edit: !this.state.edit,
        });
    }

    handleCloseEdit = (obj) => {
        if (obj.id === this.props.ruleData.id) {
            this.setState({ edit: false });
        }
    };

    handleCopy = () => {
        const { id, type } = this.props.ruleData;
        this.props.handleCopy(id, type);
    };

    handleClone = () => {
        const { id, type, parentId } = this.props.ruleData;
        this.props.handleClone(id, type, parentId);
    };

    handleAddNewRule = (callback) => {
        EventEmitter.emit(MESSAGES.RELOAD);
        callback && callback();
    };

    render() {
        const { type, ruleData } = this.props;
        const { edit } = this.state;
        const { type: ruleType } = ruleData;

        if (edit) {
            return (
                <ObjectMappingRuleForm
                    id={this.props.ruleData.id}
                    parentId={this.props.parentId}
                    ruleData={transformRuleOfObjectMapping(ruleData)}
                    onAddNewRule={this.handleAddNewRule}
                    viewActions={this.props.viewActions}
                />
            );
        }

        // @FIXME type vs ruleType is it not same?
        return (
            <div>
                <div className="ecc-silk-mapping__rulesviewer">
                    <CardContent>
                        {!isRootRule(type) ? (
                            [
                                <TargetProperty
                                    key={"ObjectTargetProperty"}
                                    mappingTargetUri={_.get(ruleData, "mappingTarget.uri")}
                                    isObjectMapping={true}
                                    isAttribute={_.get(ruleData, "mappingTarget.isAttribute")}
                                />,
                                <ObjectEntityRelation
                                    key={"ObjectEntityRelation"}
                                    isBackwardProperty={_.get(ruleData, "mappingTarget.isBackwardProperty")}
                                    parent={this.props.parent}
                                />,
                            ]
                        ) : (
                            <TargetCardinality
                                isAttribute={_.get(ruleData, "mappingTarget.isAttribute")}
                                isObjectMapping={true}
                                editable={false}
                            />
                        )}
                        {_.get(ruleData, "rules.typeRules[0].typeUri") ? (
                            <ObjectTypeRules typeRules={_.get(ruleData, "rules.typeRules") || {}} />
                        ) : null}
                        {isObjectRule(type) && ruleData.sourcePath ? (
                            <ObjectSourcePath type={ruleData.type}>
                                <SourcePath
                                    rule={{
                                        type,
                                        sourcePath: ruleData.sourcePath,
                                    }}
                                />
                            </ObjectSourcePath>
                        ) : null}
                        {
                            <ObjectUriPattern
                                uriRule={_.get(ruleData, "rules.uriRule") || {}}
                                onRemoveUriRule={this.removeUriRule}
                                ruleData={this.props.ruleData}
                                openMappingEditor={this.openEditor}
                            />
                        }
                        {_.get(ruleData, "rules.uriRule.id") ? (
                            <ExampleTarget uriRuleId={_.get(ruleData, "rules.uriRule.id")} />
                        ) : null}
                        {_.get(ruleData, "metadata.label") ? (
                            <MetadataLabel label={_.get(ruleData, "metadata.label", "")} />
                        ) : null}
                        {_.get(ruleData, "metadata.description") ? (
                            <MetadataDesc description={_.get(ruleData, "metadata.description", "")} />
                        ) : null}
                    </CardContent>
                    <Divider />
                    <CardActions className="ecc-silk-mapping__rulesviewer__actionrow">
                        <EditButton onEdit={this.handleEdit} />
                        {isCopiableRule(ruleType) && <CopyButton onCopy={this.handleCopy} />}
                        {isClonableRule(ruleType) && <CloneButton onClone={this.handleClone} />}
                        {!isRootRule(type) && (
                            <DeleteButton
                                onDelete={() => {
                                    this.props.onClickedRemove({
                                        id: this.props.ruleData.id,
                                        uri: this.props.ruleData.mappingTarget.uri,
                                        type: ruleType,
                                        parent: this.props.parentId,
                                    });
                                }}
                            />
                        )}
                    </CardActions>
                </div>
            </div>
        );
    }
}

export default ObjectRule;
