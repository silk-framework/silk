import React from 'react';
import UseMessageBus from '../../../UseMessageBusMixin';
import {
    TextField,
    SelectBox,
    ConfirmationDialog,
    AffirmativeButton,
    DismissiveButton,
    Spinner,
} from 'ecc-gui-elements';
import hierarchicalMappingChannel from '../../../store';
import {wasTouched} from './helpers';
import _ from 'lodash';

const ValueMappingRuleForm = React.createClass({
    mixins: [UseMessageBus],

    // define property types
    // FIXME: check propTypes
    propTypes: {
        id: React.PropTypes.string,
    },
    getInitialState() {
        return {
            loading: true,
            changed: false,
        };
    },
    componentDidMount(){
        this.loadData();
    },
    loadData(){
        if (this.props.id) {
            hierarchicalMappingChannel.request(
                {
                    topic: 'rule.get',
                    data: {
                        id: this.props.id,
                    }
                }
            )
                .subscribe(
                    ({rule}) => {

                        const initialValues = {
                            type: _.get(rule, 'type', 'direct'),
                            comment: _.get(rule, 'metadata.description', ''),
                            targetProperty: _.get(rule, 'mappingTarget.uri', undefined),
                            propertyType: _.get(rule, 'mappingTarget.valueType.nodeType', 'AutoDetectValueType'),
                            sourceProperty: rule.sourcePath,
                        };

                        this.setState({
                            loading: false,
                            ...initialValues,
                            initialValues,
                        });
                    },
                    (err) => {
                        console.warn('err MappingRuleOverview: rule.get');
                        this.setState({loading: false});
                    }
                );
        } else {
            this.setState({
                create: true,
                loading: false,
                type: 'direct',
                propertyType: 'AutoDetectValueType',
                sourceProperty: '',
                initialValues: {},
            })
        }
    },
    handleConfirm(event) {
        event.stopPropagation();
        hierarchicalMappingChannel.subject('rule.createValueMapping').onNext({
            id: this.props.id,
            parentId: this.props.parentId,
            type: this.state.type,
            comment: this.state.comment,
            targetProperty: this.state.targetProperty,
            propertyType: this.state.propertyType,
            sourceProperty: this.state.sourceProperty,
        });
        this.handleClose(event);
    },
    // remove rule
    handleChangeTextfield(state, {value}) {
        this.handleChangeValue(state, value);
    },
    handleChangeSelectBox(state, value) {
        this.handleChangeValue(state, value);
    },
    handleChangeValue(name, value) {

        const {initialValues, create, ...currValues} = this.state;

        currValues[name] = value;

        const touched = create || wasTouched(initialValues, currValues);
        const id = _.get(this.props, 'id', 0);

        if (touched) {
            hierarchicalMappingChannel.subject('ruleView.change').onNext({id});
        } else {
            hierarchicalMappingChannel.subject('ruleView.unchanged').onNext({id});
        }

        this.setState({
            [name]: value,
            changed: touched,
        });

    },
    handleClose(event) {
        event.stopPropagation();
        const id = _.get(this.props, 'id', 0);
        hierarchicalMappingChannel.subject('ruleView.unchanged').onNext({id});
        hierarchicalMappingChannel.subject('ruleView.close').onNext({id});
    },
    // template rendering
    render () {
        const {
            id,
        } = this.props;

        const {
            type,
            loading,
        } = this.state;

        if (loading) {
            return <Spinner/>
        }

        // FIXME: also check if data really has changed before allow saving
        const allowConfirm = this.state.targetProperty;

        const title = (
            !id ? (
                <div className="mdl-card__title mdl-card--border">
                    Add value mapping
                </div>
            ) : false
        );

        //TODO: Unfold complex mapping
        let sourcePropertyInput = false;

        if (type === 'direct') {
            sourcePropertyInput = (
                <TextField
                    label={'Source property'}
                    onChange={this.handleChangeTextfield.bind(null, 'sourceProperty')}
                    value={this.state.sourceProperty}
                />
            );
        } else if (type === 'complex') {
            sourcePropertyInput = (
                <TextField
                    disabled={true}
                    label="Source property"
                    value="Complex Mapping"
                />
            )
        }
        //TODO: Where to get the list of target Properties?
        //TODO: Where to get the list of target property types?
        return (
            <div
                className="ecc-silk-mapping__ruleseditor"
            >
                <div className={
                    "mdl-card mdl-card--stretch" +
                    (!id ? ' mdl-shadow--2dp' : '')
                }>
                    {title}
                    <div className="mdl-card__content">
                        <SelectBox
                            placeholder={'Choose target property'}
                            className="ecc-silk-mapping__ruleseditor__targetProperty"
                            options={[
                                'http://xmlns.com/foaf/0.1/name',
                                'http://xmlns.com/foaf/0.1/knows',
                                'http://xmlns.com/foaf/0.1/familyName',
                            ]}
                            value={this.state.targetProperty}
                            onChange={this.handleChangeSelectBox.bind(null, 'targetProperty')}
                        />
                        <SelectBox
                            placeholder={'Choose property type'}
                            className="ecc-silk-mapping__ruleseditor__propertyType"
                            options={[
                                "AutoDetectValueType",
                                "UriValueType",
                                "BooleanValueType",
                                "StringValueType",
                                "IntegerValueType",
                                "LongValueType",
                                "FloatValueType",
                                "DoubleValueType",
                            ]}
                            value={this.state.propertyType}
                            onChange={this.handleChangeSelectBox.bind(null, 'propertyType')}
                        />
                        {sourcePropertyInput}
                        <TextField
                            multiline={true}
                            label="Comment"
                            className="ecc-silk-mapping__ruleseditor__comment"
                            value={this.state.comment}
                            onChange={this.handleChangeTextfield.bind(null, 'comment')}
                        />
                    </div>
                    <div className="ecc-silk-mapping__ruleseditor__actionrow mdl-card__actions mdl-card--border">
                        <AffirmativeButton
                            className="ecc-silk-mapping__ruleseditor__actionrow-save"
                            onClick={this.handleConfirm}
                            disabled={!allowConfirm || !this.state.changed}
                        >
                            Save
                        </AffirmativeButton>
                        <DismissiveButton
                            className="ecc-silk-mapping__ruleseditor___actionrow-cancel"
                            onClick={this.handleClose}
                        >
                            Cancel
                        </DismissiveButton>
                    </div>
                </div>
            </div>

        );
    },

});

export default ValueMappingRuleForm;
