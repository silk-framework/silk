import React from "react";
import { SuggestionTypeValues } from "../suggestion.typings";

export default function TypesList({ onChange, selected }) {
    const handleSelectTarget = (value: SuggestionTypeValues) => {
        onChange(value);
    };

    return <select onChange={e => handleSelectTarget(e.target.value as SuggestionTypeValues)}>
            {
                ['object', 'value'].map((type) =>
                    <option
                        key={type}
                        value={type}
                        selected={selected === type}
                    >
                        {type}
                    </option>)
            }
        </select>

}
