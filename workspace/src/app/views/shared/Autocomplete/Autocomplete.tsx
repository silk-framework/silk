import React, { useEffect, useState } from "react";
import InputGroup from "@wrappers/blueprint/input-group";
import styles from './styles.module.scss';
import { sharedOp } from "@ducks/shared";
import { IPropertyAutocomplete } from "@ducks/common/typings";

interface IProps {
    artefactId: string;
    parameterId: string;
    projectId: string;
    autoCompletion: IPropertyAutocomplete;

    onChange(value: string);
    name: string;
    id?: string;
    value?: string;
}

export function Autocomplete(props: IProps) {
    // The active selection's index
    const [active, setActive] = useState<number>(-1);
    // The suggestions that match the user's input
    const [filtered, setFiltered] = useState<any[]>([]);
    // Whether or not the suggestion list is shown
    const [visible, setVisible] = useState<boolean>(false);
    // What the user has entered
    const [userInput, setUserInput] = useState<string>('');

    // const getSuggestionsDelayed: any = debounce(sharedOp.getAutocompleteResultsAsync,  1000);

    useEffect(() => {
        setUserInput(props.value);
    }, [props.value]);

    const updateSuggestions = async (input) => {
        try {
            const { parameterId, artefactId, projectId, autoCompletion } =  props;
            const list = await sharedOp.getAutocompleteResultsAsync({
                pluginId: artefactId,
                parameterId,
                projectId,
                dependsOnParameterValues: autoCompletion.autoCompletionDependsOnParameters,
                textQuery: input
            });

            // Filter our suggestions that don't contain the user's input
            const filteredSuggestions = list.filter(
                ({label, value}) =>
                    value.toLowerCase().indexOf(input.toLowerCase()) > -1 ||
                    label.toLowerCase().indexOf(input.toLowerCase()) > -1
            );

            setFiltered(filteredSuggestions);
        } finally {
            // Update the user input and filtered suggestions, reset the active
            // suggestion and make sure the suggestions are shown
            setVisible(true);
        }
    };

    const onFocus = () => {
        updateSuggestions(userInput);
    };

    const onChange = e => {
        const input = e.currentTarget.value;
        setUserInput(input);

        updateSuggestions(input);
    };

    // Event fired when the user clicks on a suggestion
    const onClick = value => {
        // Update the user input and reset the rest of the state
        setActive(0);
        setFiltered([]);
        setVisible( false);
        setUserInput(value);

        props.onChange(value);
    };

    const onKeyDown = e => {
        // User pressed the enter key, update the input and close the
        // suggestions
        if (e.keyCode === 13) {
            let selectedValue = filtered[active];
            if (active === -1) {
                selectedValue = props.autoCompletion.allowOnlyAutoCompletedValues ? null : userInput;
            }

            if (selectedValue) {
                setActive(-1);
                setVisible(false);
                setUserInput(selectedValue.value);
                props.onChange(selectedValue.value);
            }
        }
        // User pressed the up arrow, decrement the index
        else if (e.keyCode === 38) {
            if (active === -1) {
                return;
            }
            setActive(active - 1);
        }
        // User pressed the down arrow, increment the index
        else if (e.keyCode === 40) {
            if (active - 1 === filtered.length) {
                return;
            }

            setActive(active + 1);
        }
    };

    let suggestionsListComponent = null;
    if (visible) {
        if (filtered.length) {
            suggestionsListComponent = (
                <ul className={styles.suggestions}>
                    {filtered.map((suggestion, index) => {
                        let className;

                        // Flag the active suggestion with a class
                        if (index === active) {
                            className = styles.suggestionActive;
                        }

                        return (
                            <li
                                className={className}
                                key={suggestion.value}
                                onClick={() => onClick(suggestion.value)}
                            >
                                {suggestion.label || suggestion.value}
                            </li>
                        );
                    })}
                </ul>
            );
        }
        // else {
        //     suggestionsListComponent = (
        //         <div className={styles.noSuggestions}>
        //             <em>No suggestions, you're on your own!</em>
        //         </div>
        //     );
        // }
    }

    return (
        <>
            <InputGroup
                onChange={onChange}
                onKeyDown={onKeyDown}
                onFocus={onFocus}
                name={props.name}
                id={props.id}
                value={userInput}
            />
            {suggestionsListComponent}
        </>
    );

}
