import React from 'react';
import UseMessageBus from '../../UseMessageBusMixin';
import {
    Button,
    AffirmativeButton,
    DismissiveButton,
    DisruptiveButton,
    Info,
} from 'ecc-gui-elements';
import ExampleView from './ExampleView';
import hierarchicalMappingChannel from '../../store';
import _ from 'lodash';
import ValueMappingRuleForm from './Forms/ValueMappingRuleForm';
import {
    SourcePath,
    ThingName,
    ThingDescription,
} from './SharedComponents';

const RuleValueView = React.createClass({
    mixins: [UseMessageBus],

    // define property types
    // FIXME: check propTypes
    propTypes: {
        comment: React.PropTypes.string,
        id: React.PropTypes.string,
        //operator: React.PropTypes.object,
        type: React.PropTypes.string,
        sourcePath: React.PropTypes.string,
        mappingTarget: React.PropTypes.object,
        edit: React.PropTypes.bool.isRequired,
    },
    handleCloseEdit(obj) {
        if (obj.id === this.props.id)
            this.setState({edit: false})
    },
    componentDidMount() {
        this.subscribe(hierarchicalMappingChannel.subject('ruleView.close'), this.handleCloseEdit);
    },
    getInitialState() {
        this.subscribe(hierarchicalMappingChannel
                .request({topic: 'rule.getEditorHref', data: {id: this.props.id}}),
            ({href}) => this.setState({href})
        )

        return {
            edit: this.props.edit,
            href: null,
        };
    },
    handleComplexEdit(event) {
        if (__DEBUG__) {
            event.stopPropagation();
            alert('Normally this would open the complex editor (aka jsplumb view)');
            return false;
        }
    },
    // open view in edit mode
    handleEdit(event) {
        event.stopPropagation();
        this.setState({
            edit: !this.state.edit,
        })
    },
    handleClose(event) {
        event.stopPropagation();
        hierarchicalMappingChannel.subject('ruleView.unchanged').onNext({id: this.props.id});
    },
    // template rendering
    render () {
        const {edit} = this.state;

        if (edit) {
            return <ValueMappingRuleForm
                id={this.props.id}
                parentId={this.props.parentId}
            />
        }

        return (
            (
                <div
                    className="ecc-silk-mapping__rulesviewer"
                >

                    <div className="mdl-card mdl-card--stretch">
                        <div
                            className="ecc-silk-mapping__rulesviewer__title mdl-card__title mdl-card--border clickable"
                            onClick={this.props.handleToggleExpand}
                        >
                            <div className="mdl-card__title-text">
                                Readable name of {_.get(this.props, 'mappingTarget.uri', undefined)}
                            </div>
                        </div>
                        <div className="mdl-card__content">
                            {
                                _.get(this.props, 'mappingTarget.uri', undefined) ? (
                                    <div
                                        className="ecc-silk-mapping__rulesviewer__targetProperty"
                                    >
                                        <dl className="ecc-silk-mapping__rulesviewer__attribute">
                                            <dt className="ecc-silk-mapping__rulesviewer__attribute-label">
                                                Target property
                                            </dt>
                                            <dd className="ecc-silk-mapping__rulesviewer__attribute-title">
                                                <ThingName id={_.get(this.props, 'mappingTarget.uri', undefined)}/>
                                            </dd>
                                            <dd className="ecc-silk-mapping__rulesviewer__attribute-info">
                                                <code>{_.get(this.props, 'mappingTarget.uri', undefined)}</code>
                                            </dd>
                                            <dd className="ecc-silk-mapping__rulesviewer__attribute-info">
                                                <Info border>
                                                    <ThingDescription
                                                        id={_.get(this.props, 'mappingTarget.uri', undefined)}/>
                                                </Info>
                                            </dd>
                                        </dl>
                                    </div>
                                ) : false
                            }
                            {
                                _.get(this.props, 'mappingTarget.valueType.nodeType', undefined) ? (
                                    <div
                                        className="ecc-silk-mapping__rulesviewer__propertyType"
                                    >
                                        <dl className="ecc-silk-mapping__rulesviewer__attribute">
                                            <dt className="ecc-silk-mapping__rulesviewer__attribute-label">
                                                Data type
                                            </dt>
                                            <dd className="ecc-silk-mapping__rulesviewer__attribute-title">
                                                {_.get(this.props, 'mappingTarget.valueType.nodeType', undefined)}
                                            </dd>
                                            <dd className="ecc-silk-mapping__rulesviewer__attribute-info">
                                                Any other information available here? (TODO)
                                            </dd>
                                        </dl>
                                    </div>
                                ) : false
                            }
                            {

                                this.props.type === 'direct' ? (
                                    <div
                                        className="ecc-silk-mapping__rulesviewer__sourcePath"
                                    >
                                        <dl className="ecc-silk-mapping__rulesviewer__attribute">
                                            <dt className="ecc-silk-mapping__rulesviewer__attribute-label">
                                                Value path
                                            </dt>
                                            <dd className="ecc-silk-mapping__rulesviewer__attribute-title">
                                                <code>{this.props.sourcePath}</code>
                                            </dd>
                                            <dd className="ecc-silk-mapping__rulesviewer__attribute-info">
                                                <a
                                                    className="ecc-silk-mapping__ruleseditor__actionrow-complex-edit"
                                                    onClick={this.handleComplexEdit}
                                                    href={this.state.href}
                                                >
                                                    Convert value path to value formula
                                                </a>
                                            </dd>
                                        </dl>
                                    </div>
                                ) : (
                                    <div
                                        className="ecc-silk-mapping__rulesviewer__sourcePath"
                                    >
                                        <dl className="ecc-silk-mapping__rulesviewer__attribute">
                                            <dt className="ecc-silk-mapping__rulesviewer__attribute-label">
                                                Value formula
                                            </dt>
                                            <dd className="ecc-silk-mapping__rulesviewer__attribute-title">
                                                <code>
                                                    Value Paths: {_.get(this, 'props.sourcePaths', []).join(', ')}
                                                </code>
                                            </dd>
                                            <dd className="ecc-silk-mapping__rulesviewer__attribute-info">
                                                TODO: comma-separated list of used operator functions
                                            </dd>
                                            <dd className="ecc-silk-mapping__rulesviewer__attribute-info">
                                                <a
                                                    className="ecc-silk-mapping__ruleseditor__actionrow-complex-edit"
                                                    onClick={this.handleComplexEdit}
                                                    href={this.state.href}
                                                >
                                                    Edit value formula
                                                </a>
                                            </dd>
                                        </dl>
                                    </div>

                                )
                            }
                            {
                                _.has(this, 'props.metadata.description', false) ? (
                                    <div
                                        className="ecc-silk-mapping__rulesviewer__comment"
                                    >
                                        <dl className="ecc-silk-mapping__rulesviewer__attribute">
                                            <dt className="ecc-silk-mapping__rulesviewer__attribute-label">
                                                Description
                                            </dt>
                                            <dd className="ecc-silk-mapping__rulesviewer__attribute-info">
                                                {_.get(this, 'props.metadata.description')}
                                            </dd>
                                        </dl>
                                    </div>
                                ) : false
                            }
                            { _.has(this.props, 'id')
                                ? <div
                                    className="ecc-silk-mapping__rulesviewer__examples"
                                >
                                    <dl className="ecc-silk-mapping__rulesviewer__attribute">
                                        <dt className="ecc-silk-mapping__rulesviewer__attribute-label">
                                            Examples of target data
                                        </dt>
                                        <dd className="ecc-silk-mapping__rulesviewer__attribute-info">

                                            <ExampleView
                                                id={this.props.id}
                                            />
                                        </dd>
                                    </dl>
                                </div>
                                : false}
                        </div>
                        <div className="ecc-silk-mapping__ruleseditor__actionrow mdl-card__actions mdl-card--border">
                            <Button
                                className="ecc-silk-mapping__ruleseditor__actionrow-edit"
                                onClick={this.handleEdit}
                            >
                                Edit
                            </Button>
                            <DisruptiveButton
                                className="ecc-silk-mapping__ruleseditor__actionrow-remove"
                                onClick={() => hierarchicalMappingChannel.subject(
                                    'removeClick'
                                ).onNext(
                                    {
                                        id: this.props.id,
                                        uri: this.props.mappingTarget.uri,
                                        type: this.props.type,
                                        parent: this.props.parentId
                                    }
                                )}
                                disabled={false} // FIXME: all elements are removable?
                            >
                                Remove
                            </DisruptiveButton>
                        </div>
                    </div>
                </div>
            )
        );
    },

});

export default RuleValueView;
