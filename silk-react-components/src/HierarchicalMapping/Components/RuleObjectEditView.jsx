import React from 'react';
import {UseMessageBus} from 'ecc-mixins';
import {Button, SelectBox, Radio, RadioGroup} from 'ecc-gui-elements';
import hierarchicalMappingChannel from '../store';

const RuleObjectEditView = React.createClass({
    mixins: [UseMessageBus],

    // define property types
    // FIXME: check propTypes
    propTypes: {
        comment: React.PropTypes.string,
        id: React.PropTypes.string,
        name: React.PropTypes.string,
        type: React.PropTypes.string,
        typeRules: React.PropTypes.array,
        mappingTarget: React.PropTypes.object,
        targetProperty: React.PropTypes.string,
        pattern: React.PropTypes.string,
        uriRule: React.PropTypes.object,
        onClose: React.PropTypes.func.isRequired,
    },

    getInitialState() {
        return {
            targetProperty: this.props.targetProperty,
            // FIXME: get it from props in edit mode
            targetEntityType: undefined,
            entityConnection: 'from',
        };
    },

    handleConfirm() {
        hierarchicalMappingChannel.subject('ruleId.createMapping').onNext({
            // if id is undefined -> we are creating a new rule
            id: this.props.id,
            type: this.props.type,
            targetProperty: this.state.targetProperty,
            targetEntityType: this.state.targetEntityType,
        });
        this.props.onClose();
    },

    handleChangeSelectBox(state, value) {
        this.setState({
            [state]: value,
        });
    },

    handleChangeRadio(state, {value}) {
        this.setState({
            [state]: value,
        });
    },

    // template rendering
    render () {
        const {
            id,
        } = this.props;

        const allowConfirm = (
            this.state.targetProperty && this.state.targetEntityType
        );

        console.warn('debug OBJECT edit view', this.props);
        return (
            <div
                className="ecc-component-hierarchicalMapping__content-editView-object"
            >
                <div className="mdl-card mdl-shadow--2dp mdl-card--stretch stretch-vertical">
                    <div
                        className="mdl-card__title"
                    >
                        {id ? 'Edit' : 'Add'} object mapping
                    </div>
                    <div className="mdl-card__content">
                        <SelectBox
                            placeholder={'Choose target property'}
                            className="ecc-component-hierarchicalMapping__content-editView-object__content__targetProperty"
                            // TODO: get list of target properties
                            options={[]}
                            value={this.state.targetProperty}
                            onChange={this.handleChangeSelectBox.bind(null, 'targetProperty')}
                        />
                        <RadioGroup
                            onChange={this.handleChangeRadio.bind(null, 'entityConnection')}
                            value={this.state.entityConnection}
                            name=""
                        >
                            <Radio
                                value="from"
                                label="Connects from entity"
                            />
                            <Radio
                                value="to"
                                label="Connects to entity"
                            />
                        </RadioGroup>
                        entity chooser
                        <SelectBox
                            placeholder={'Choose target entity type'}
                            className="ecc-component-hierarchicalMapping__content-editView-object__content__targetEntityType"
                            // TODO: get list of target entity types
                            options={[]}
                            value={this.state.targetEntityType}
                            onChange={this.handleChangeSelectBox.bind(null, 'targetEntityType')}
                        />
                        <div className="ecc-component-hierarchicalMapping__content-editView-object__actionrow">
                            <Button
                                className="ecc-component-hierarchicalMapping__content-editView-object__actionrow-save"
                                onClick={() => {}}
                                disabled
                            >
                                Save
                            </Button>
                            <Button
                                className="ecc-component-hierarchicalMapping__content-editView-object__actionrow-cancel"
                                onClick={this.props.onClose}
                            >
                                Cancel
                            </Button>
                        </div>
                    </div>
                </div>
            </div>
        );
    },

});

export default RuleObjectEditView;