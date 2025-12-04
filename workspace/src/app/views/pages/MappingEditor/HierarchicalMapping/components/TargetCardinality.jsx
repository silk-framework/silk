import React from "react";
import { RadioGroup } from "gui-elements-deprecated";
import * as PropTypes from "prop-types";
import { FieldItem, RadioButton, Tooltip } from "@eccenca/gui-elements";

/** Let's a user choose if a target property is single or multi-valued. Depending on the dataset type this will have different implications. */
class TargetCardinality extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            isAttribute: props.isAttribute,
        };
    }

    onChange(isAttribute) {
        this.setState({ isAttribute: isAttribute });
        this.props.onChange(isAttribute);
    }

    render() {
        if (this.props.editable !== false) {
            return this.renderRadioBox();
        } else if (this.state.isAttribute) {
            return this.renderSingleValue();
        } else {
            return this.renderMultipleValues();
        }
    }

    renderRadioBox() {
        return (
            <FieldItem className={this.props.className}>
                <RadioGroup
                    value={this.state.isAttribute ? "single" : "multiple"}
                    name=""
                    onChange={({ value }) => {
                        this.onChange(value === "single");
                    }}
                >
                    <RadioButton name="single" value="single" labelElement={this.renderSingleValue()} />
                    <RadioButton name="multiple" value="multiple" labelElement={this.renderMultipleValues()} />
                </RadioGroup>
            </FieldItem>
        );
    }

    renderSingleValue() {
        if (this.props.isObjectMapping) {
            return (
                <Tooltip
                    content={
                        <span style={{ textAlign: "left" }}>
                            A single entity is expected for each parent entity. Receiving multiple entities will trigger
                            a validation error.
                            <br />
                            In addition, the following datasets will adapt the generated schema:
                            <br />
                            <b>XML:</b> Entities will be written as a nested element. The name of the element tag is
                            specified by the property name.
                            <br />
                            <b>JSON:</b> Entities will be written as an object, which is not wrapped by an array.
                        </span>
                    }
                >
                    Only a single entity is allowed
                </Tooltip>
            );
        } else {
            return (
                <Tooltip
                    content={
                        <span style={{ textAlign: "left" }}>
                            A single value is expected for this property. Receiving multiple values will trigger a
                            validation error.
                            <br />
                            In addition, the following datasets will adapt the generated schema:
                            <br />
                            <b>XML:</b> Values will be written as an attribute. The name of the attribute is specified
                            by the property name.
                            <br />
                            <b>JSON:</b> Values will be written as literals, which are not wrapped in an array.
                        </span>
                    }
                >
                    Only a single value is allowed
                </Tooltip>
            );
        }
    }

    renderMultipleValues() {
        if (this.props.isObjectMapping) {
            return (
                <Tooltip
                    content={
                        <div style={{ textAlign: "left" }}>
                            Multiple entities may be generated for each parent entity.
                            <br />
                            In addition, the following datasets will adapt the generated schema:
                            <br />
                            <b>XML:</b> Entities will be written as nested elements. The name of the element tags is
                            specified by the property name.
                            <br />
                            <b>JSON:</b> Entities will be wrapped in an array.
                        </div>
                    }
                >
                    Multiple entities are allowed
                </Tooltip>
            );
        } else {
            return (
                <Tooltip
                    content={
                        <div style={{ textAlign: "left" }}>
                            Multiple values may be generated for this property.
                            <br />
                            In addition, the following datasets will adapt the generated schema:
                            <br />
                            <b>XML:</b> Values will be written as nested elements. The name of the element is specified
                            by the property name.
                            <br />
                            <b>JSON:</b> Values will be written as an array of literals.
                        </div>
                    }
                >
                    Multiple values are allowed
                </Tooltip>
            );
        }
    }
}

TargetCardinality.propTypes = {
    isAttribute: PropTypes.bool.isRequired,
    isObjectMapping: PropTypes.bool.isRequired,
    editable: PropTypes.bool,
    onChange: PropTypes.func,
};

export default TargetCardinality;
