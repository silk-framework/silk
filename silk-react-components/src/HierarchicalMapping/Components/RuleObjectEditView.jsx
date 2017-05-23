import React from 'react';
import {UseMessageBus} from 'ecc-mixins';
import {Button} from 'ecc-gui-elements';

const RuleObjectEditView = React.createClass({
    mixins: [UseMessageBus],

    // define property types
    // FIXME: check propTypes
    propTypes: {
        comment: React.PropTypes.string,
        id: React.PropTypes.string,
        name: React.PropTypes.string,
        operator: React.PropTypes.object,
        type: React.PropTypes.string, // mapping type
        typeRules: React.PropTypes.array,
        mappingTarget: React.PropTypes.object,
        sourcePath: React.PropTypes.string,
        targetProperty: React.PropTypes.string,
        pattern: React.PropTypes.string,
        uriRule: React.PropTypes.object,
        onClose: React.PropTypes.func.isRequired,
    },

    // template rendering
    render () {
        console.warn('debug OBJECT edit view', this.props);
        return (
            <div
                className="ecc-component-hierarchicalMapping__content-editView-object"
            >
                <div className="mdl-card mdl-shadow--2dp mdl-card--stretch stretch-vertical">
                    <div
                        className="mdl-card__title"
                    >
                        (Add) object mapping
                    </div>
                    <div className="mdl-card__content">
                        View here of id {this.props.id}
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