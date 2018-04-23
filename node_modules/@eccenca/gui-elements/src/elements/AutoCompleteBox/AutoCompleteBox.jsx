import React from 'react';
import _ from 'lodash';
import Highlighter from 'react-highlight-words';
import cx from 'classnames';
import SelectBox from '../SelectBox/SelectBox';

const Highlight = props => {
    const {textToHighlight, searchWord} = props;

    if (!_.isString(textToHighlight) || _.isEmpty(textToHighlight)) {
        return false;
    }

    if (!_.isString(searchWord) || _.isEmpty(searchWord)) {
        return <span>{textToHighlight}</span>;
    }

    return (
        <Highlighter
            textToHighlight={textToHighlight}
            searchWords={[searchWord]}
        />
    );
};

class AutoCompleteBox extends React.Component {
    currentInputValue = null;

    optionRender = option => {
        const {label, value, description} = option;

        // only show value entry if it is not same as label
        const optionValue =
            value === label ? (
                false
            ) : (
                <code key="autoCompleteValue" className="Select-option__value">
                    <Highlight
                        textToHighlight={value}
                        searchWord={this.currentInputValue}
                    />
                </code>
            );

        const optionDescription = description ? (
            <span
                key="autoCompleteDescription"
                className="Select-option__description">
                <Highlight
                    textToHighlight={description}
                    searchWord={this.currentInputValue}
                />
            </span>
        ) : (
            false
        );

        return [
            <strong key="autoCompleteLabel" className="Select-option__label">
                <Highlight
                    textToHighlight={label}
                    searchWord={this.currentInputValue}
                />
            </strong>,
            optionValue,
            optionDescription,
        ];
    };

    render = () => (
        <SelectBox
            {...this.props}
            className={cx(this.props.className, 'Select--AutoComplete')}
            onInputChange={inputValue => {
                this.currentInputValue = _.clone(inputValue);
                return inputValue;
            }}
            searchable
            optionRenderer={this.optionRender}
        />
    );
}

export default AutoCompleteBox;
