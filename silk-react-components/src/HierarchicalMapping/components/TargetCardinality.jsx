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
                         <b>JSON:</b> No arrays will be used for writing values.
                     </div>
                 }>
            Only a single value is allowed
        </Tooltip>
    }

    renderMultipleValues() {
        return <Tooltip
            label={
                <div style={{textAlign: "left"}}>
                    Multiple values may be generated for this property.
                    <br/>
                    In addition, the following datasets will adapt the generated schema:
                    <br/>
                    <b>XML:</b> Values will be written as nested elements.
                    <br/>
                    <b>JSON:</b> Arrays will be used for writing values.
                </div>
            }>
            Multiple values are allowed
        </Tooltip>
    }
}

TargetCardinality.propTypes = {
    isAttribute: PropTypes.bool.isRequired,
    editable: PropTypes.bool,
    onChange: PropTypes.func
};

export default TargetCardinality;
