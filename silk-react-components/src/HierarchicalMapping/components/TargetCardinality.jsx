import React from 'react';
import {
    Tooltip,
    Radio,
    RadioGroup,
} from '@eccenca/gui-elements';
import * as PropTypes from "prop-types";

class TargetCardinality extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            isAttribute: props.isAttribute,
        };
    }

    onChange(isAttribute) {
        this.setState({isAttribute: isAttribute});
        this.props.onChange(isAttribute);
    }

    render() {
        if(this.props.editable !== false) {
            return this.renderRadioBox()
        } else if(this.state.isAttribute) {
            return this.renderSingleValue()
        } else {
            return this.renderMultipleValues()
        }
    }

    renderRadioBox() {
        return (
            <RadioGroup
                value={this.state.isAttribute ? 'single' : 'multiple'}
                name = ""
                onChange={({ value }) => { this.onChange(value === "single") }}
            >
                <Radio
                    name="single"
                    value="single"
                    label={ this.renderSingleValue() }
                />
                <Radio
                    name="multiple"
                    value="multiple"
                    label={ this.renderMultipleValues() }
                />
            </RadioGroup>
        )
    }

    renderSingleValue() {
        if(this.props.isObjectMapping) {
            return <Tooltip
                label={
                    <div style={{textAlign: "left"}}>
                        A single entity is expected for each parent entity.
                        Receiving multiple entities will trigger a validation error.
                        <br/>
                        In addition, the JSON dataset will write objects, which are not wrapped by an array.
                    </div>
                }>
                Only a single entity is allowed
            </Tooltip>
        } else {
            return <Tooltip
                label={
                    <div style={{textAlign: "left"}}>
                        A single value is expected for this property.
                        Receiving multiple values will trigger a validation error.
                        <br/>
                        In addition, the following datasets will adapt the generated schema:
                        <br/>
                        <b>XML:</b> Values will be written as an attribute.
                        <br/>
                        <b>JSON:</b> Values will be written as literals, which are not wrapped in an array.
                    </div>
                }>
                Only a single value is allowed
            </Tooltip>
        }
    }

    renderMultipleValues() {
        if(this.props.isObjectMapping) {
            return <Tooltip
                label={
                    <div style={{textAlign: "left"}}>
                        Multiple entities may be generated for each parent entity.
                        <br/>
                        In addition, the JSON dataset will wrap all entities in an array.
                    </div>
                }>
                Multiple entities are allowed
            </Tooltip>
        } else {
            return <Tooltip
                label={
                    <div style={{textAlign: "left"}}>
                        Multiple values may be generated for this property.
                        <br/>
                        In addition, the following datasets will adapt the generated schema:
                        <br/>
                        <b>XML:</b> Values will be written as nested elements.
                        <br/>
                        <b>JSON:</b> Values will be written as an array of literals.
                    </div>
                }>
                Multiple values are allowed
            </Tooltip>
        }
    }
}

TargetCardinality.propTypes = {
    isAttribute: PropTypes.bool.isRequired,
    isObjectMapping: PropTypes.bool.isRequired,
    editable: PropTypes.bool,
    onChange: PropTypes.func
};

export default TargetCardinality;
