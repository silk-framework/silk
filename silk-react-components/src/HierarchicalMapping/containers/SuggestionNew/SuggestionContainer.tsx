import React, { useEffect, useRef, useState } from 'react';
import {
    Button,
    Divider,
    Grid,
    GridColumn,
    GridRow,
    Section,
    SectionHeader,
    TitleMainsection
} from "@gui-elements/index";
import { TableContainer } from 'carbon-components-react';
import SuggestionList from "./SuggestionList";
import SuggestionHeader from "./SuggestionHeader";
import { generateRuleAsync, getSuggestionsAsync, schemaExampleValuesAsync } from "../../store";
import _ from "lodash";
import { IAddedSuggestion, ITransformedSuggestion } from "./suggestion.typings";

interface ISuggestionListContext {
    portalContainer: HTMLDivElement;
    exampleValues: {
        [key: string]: string[]
    };
}

export const SuggestionListContext = React.createContext<ISuggestionListContext>({
    portalContainer: null,
    exampleValues: {}
});

export default function SuggestionContainer({ruleId, targetClassUris, onAskDiscardChanges, onClose}) {
    // Loading indicator
    const [loading, setLoading] = useState(false);

    const [warnings, setWarnings] = useState<string[]>([]);

    const [error, setError] = useState<any>({});

    const [data, setData] = useState<ITransformedSuggestion[]>([]);

    const [filteredData, setFilteredData] = useState<ITransformedSuggestion[]>([]);

    const [search, setSearch] = useState('');

    const [isFromDataset, setIsFromDataset] = useState(true);

    const [exampleValues, setExampleValues] = useState({});

    const portalContainerRef = useRef();

    useEffect(() => {
        loadData(isFromDataset);
        loadExampleValues();
    }, []);

    const handleSwapAction = () => {
        setIsFromDataset(!isFromDataset);

        loadData(!isFromDataset);
    };

    const loadData = (matchFromDataset: boolean) => {
        setLoading(true);
        return getSuggestionsAsync({
            targetClassUris,
            ruleId,
            matchFromDataset,
            nrCandidates: 20,
        }).subscribe(
            ({suggestions, warnings}) => {
                setWarnings(
                    warnings.filter(value => !_.isEmpty(value))
                );
                setData(suggestions);
                setFilteredData(suggestions);
                setLoading(false);
            },
            err => {
                setError(err);
                setLoading(false);
            }
        );
    };

    const loadExampleValues = () => {
        return schemaExampleValuesAsync().subscribe(
            (data) => {
                setExampleValues(data);
            },
            err => {
                setError(err)
            }
        );
    };

    const handleAdd = (selectedRows: IAddedSuggestion[]) => {
        setLoading(true);

        const correspondences = selectedRows
            .map(suggestion => {
                const {source, targetUri, type} = suggestion;

                const correspondence = {
                    sourcePath: source,
                    targetProperty: targetUri,
                    type,
                };

                if (!isFromDataset) {
                    correspondence.sourcePath = targetUri;
                    correspondence.targetProperty = source;
                }

                return correspondence
            });

        generateRuleAsync(correspondences, ruleId).subscribe(
            () => onClose(),
            err => {
                // If we have a list of failedRules, we want to show them, otherwise something
                // else failed
                const error = err.failedRules
                    ? err.failedRules
                    : [{error: err}];
                setError(error);
            },
            () => setLoading(false)
        );
    }

    const handleSearch = (value: string) => {
        setSearch(value);
    };

    const handleFilter = () => {
        const filtered = data.filter(o => o.source.includes(search) || o.candidates.some(t => t.uri.includes(search)));
        setFilteredData(filtered);
    };

    return (
        <Section>
            <SectionHeader>
                <Grid>
                    <GridRow>
                        <GridColumn small verticalAlign="center">
                            <TitleMainsection>Mapping Suggestion for {ruleId}</TitleMainsection>
                        </GridColumn>
                    </GridRow>
                    <GridRow>
                        <GridColumn>
                            <Button affirmative onClick={handleFilter}>Find Matches</Button>
                        </GridColumn>
                    </GridRow>
                </Grid>
            </SectionHeader>
            <Divider addSpacing="medium"/>
            <div ref={portalContainerRef}>
                <SuggestionListContext.Provider value={{
                    portalContainer: portalContainerRef.current,
                    exampleValues
                }}>
                    <TableContainer>
                        <SuggestionHeader onSearch={handleSearch}/>
                        <SuggestionList
                            rows={filteredData}
                            onSwapAction={handleSwapAction}
                            onAdd={handleAdd}
                            onAskDiscardChanges={onAskDiscardChanges}
                        />
                    </TableContainer>
                </SuggestionListContext.Provider>
            </div>
        </Section>
    )
}
