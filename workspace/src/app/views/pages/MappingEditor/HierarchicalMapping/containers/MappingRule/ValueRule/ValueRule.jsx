import React from "react";
import { Card, CardActions, CardContent, Divider } from "@eccenca/gui-elements";
import _ from "lodash";
import PropTypes from "prop-types";
import { getEditorHref } from "../../../store";
import ValueRuleForm from "./ValueRuleForm";
import { MAPPING_RULE_TYPE_DIRECT, MESSAGES } from "../../../utils/constants";
import EventEmitter from "../../../utils/EventEmitter";
import EditButton from "../../../elements/buttons/EditButton";
import CopyButton from "../../../elements/buttons/CopyButton";
import CloneButton from "../../../elements/buttons/CloneButton";
import DeleteButton from "../../../elements/buttons/DeleteButton";
import TargetProperty from "../../../components/TargetProperty";
import ValueNodeType from "../../../components/ValueMapping/ValueNodeType";
import ObjectSourcePath from "../../../components/ObjectMapping/ObjectSourcePath";
import ValueSourcePaths from "../../../components/ValueMapping/ValueSourcePaths";
import ExampleTarget from "../../../components/ExampleTarget";
import MetadataLabel from "../../../components/Metadata/MetadataLabel";
import MetadataDesc from "../../../components/Metadata/MetadataDesc";
import { IconButton, Spinner } from "@eccenca/gui-elements";

class ValueRule extends React.Component {
    // define property types
    static propTypes = {
        comment: PropTypes.string,
        id: PropTypes.string,
        type: PropTypes.string,
        sourcePath: PropTypes.string,
        mappingTarget: PropTypes.object,
        openMappingEditor: PropTypes.func,
        edit: PropTypes.bool.isRequired,
    };

    state = {
        edit: this.props.edit,
        href: getEditorHref(this.props.id),
    };

    constructor(props) {
        super(props);
        this.handleCloseEdit = this.handleCloseEdit.bind(this);
        this.handleComplexEdit = this.handleComplexEdit.bind(this);
        this.handleEdit = this.handleEdit.bind(this);
        this.handleClose = this.handleClose.bind(this);
        this.handleCopy = this.handleCopy.bind(this);
        this.handleClone = this.handleClone.bind(this);
    }

    componentDidMount() {
        EventEmitter.on(MESSAGES.RULE_VIEW.CLOSE, this.handleCloseEdit);
    }

    componentWillUnmount() {
        EventEmitter.off(MESSAGES.RULE_VIEW.CLOSE, this.handleCloseEdit);
    }

    handleCloseEdit(obj) {
        if (obj.id === this.props.id) {
            this.setState({ edit: false });
        }
    }

    handleComplexEdit(event) {
        this.props.openMappingEditor(this.props.id);
    }

    // open view in edit mode
    handleEdit(event) {
        event.stopPropagation();
        this.setState({
            edit: !this.state.edit,
        });
    }

    handleClose(event) {
        event.stopPropagation();
        EventEmitter.emit(MESSAGES.RULE_VIEW.UNCHANGED, { id: this.props.id });
    }

    handleCopy() {
        this.props.handleCopy(this.props.id, this.props.type);
    }

    handleClone() {
        this.props.handleClone(this.props.id, this.props.type);
    }

    render() {
        const { edit } = this.state;
        const {
            id,
            parentId,
            operator,
            mappingTarget = {},
            sourcePath,
            sourcePaths,
            metadata,
            mapRuleLoading,
            openMappingEditor,
        } = this.props;
        if (edit) {
            return (
                <ValueRuleForm
                    id={id}
                    parentId={parentId}
                    openMappingEditor={openMappingEditor}
                    viewActions={this.props.viewActions}
                />
            );
        }
        const nodeType = _.get(mappingTarget, "valueType.nodeType");
        if (mapRuleLoading) {
            return <Spinner />;
        }
        return (
            <div className="ecc-silk-mapping__rulesviewer">
                <Card elevation={-1}>
                    <CardContent>
                        {mappingTarget.uri ? (
                            <TargetProperty
                                key={"ObjectTargetProperty"}
                                mappingTargetUri={mappingTarget.uri}
                                isObjectMapping={false}
                                isAttribute={mappingTarget.isAttribute}
                            />
                        ) : null}
                        {nodeType ? <ValueNodeType nodeType={nodeType} valueType={mappingTarget.valueType} /> : null}
                        {this.props.type === MAPPING_RULE_TYPE_DIRECT && !sourcePaths ? (
                            <ObjectSourcePath>
                                <code>{sourcePath ? sourcePath : "<empty>"}</code>
                                <IconButton
                                    name="item-edit"
                                    data-test-id="complex-rule-edit-button"
                                    onClick={this.handleComplexEdit}
                                    text="Open value formula editor"
                                />
                            </ObjectSourcePath>
                        ) : null}
                        {this.props.type !== MAPPING_RULE_TYPE_DIRECT && sourcePaths ? (
                            <ValueSourcePaths paths={sourcePaths} operator={operator}>
                                <IconButton
                                    name="item-edit"
                                    data-test-id="complex-rule-edit-button"
                                    onClick={this.handleComplexEdit}
                                    text="Editor value formula"
                                />
                            </ValueSourcePaths>
                        ) : null}
                        {id ? <ExampleTarget uriRuleId={id} /> : null}
                        {_.get(metadata, "label") ? <MetadataLabel label={_.get(metadata, "label")} /> : null}
                        {_.get(metadata, "description") ? (
                            <MetadataDesc description={_.get(metadata, "description")} />
                        ) : null}
                    </CardContent>
                    <Divider />
                    <CardActions
                        data-test-id={"mapping-rule-card-actions"}
                        className="ecc-silk-mapping__ruleseditor__actionrow">
                        <EditButton onEdit={this.handleEdit} />
                        <CopyButton onCopy={this.handleCopy} />
                        <CloneButton onClone={this.handleClone} />
                        <DeleteButton
                            onDelete={() => {
                                this.props.onClickedRemove({
                                    id,
                                    uri: mappingTarget.uri,
                                    type: this.props.type,
                                    parent: parentId,
                                });
                            }}
                        />
                    </CardActions>
                </Card>
            </div>
        );
    }
}

export default ValueRule;
