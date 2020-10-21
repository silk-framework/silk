import React, { useEffect, useState } from 'react';
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
import { Table, TableContainer } from 'carbon-components-react';
import SuggestionList, { ISelectedSuggestion } from "./SuggestionList";
import SuggestionHeader from "./SuggestionHeader";
import { generateRuleAsync, getSuggestionsAsync } from "../../store";
import _ from "lodash";
import { ITransformedSuggestion } from "./suggestion.typings";

export default function SuggestionContainer({ruleId, targetClassUris, onAskDiscardChanges, onClose}) {
    // Loading indicator
    const [loading, setLoading] = useState(false);

    const [warnings, setWarnings] = useState<string[]>([]);

    const [error, setError] = useState<any>({});

    const [data, setData] = useState<ITransformedSuggestion[]>([]);

    const [filteredData, setFilteredData] = useState<ITransformedSuggestion[]>([]);

    const [search, setSearch] = useState('');

    const [headers, setHeaders] = useState(
        [
            {header: 'Source data', key: 'source'},
            {header: null, key: 'swapAction'},
            {header: 'Target data', key: 'target'},
            {header: 'Mapping type', key: 'type'}
        ]
    );

    const [isFromDataset, setIsFromDataset] = useState(true);

    useEffect(() => {
        setLoading(true);
        loadData(isFromDataset);
    }, []);

    const handleSwapAction = () => {
        setIsFromDataset(!isFromDataset);
        loadData(!isFromDataset);
    };

    const loadData = (matchFromDataset: boolean) => {
        getSuggestionsAsync({
            targetClassUris,
            ruleId,
            matchFromDataset,
            nrCandidates: 20,
        }).subscribe(
            ({suggestions, warnings}) => {
                setWarnings(
                    warnings.filter(value => !_.isEmpty(value))
                );
                setLoading(false);
                setData(suggestions);
                setFilteredData(suggestions);
            },
            err => {
                setLoading(false);
                setError(err)
            }
        );
    };

    const handleAdd = (selectedRows: ISelectedSuggestion[]) => {
        setLoading(true);
        const correspondences = selectedRows
            .map(suggestion => ({
                sourcePath: suggestion.source,
                targetProperty: suggestion.targetUri,
                type: suggestion.type,
            }));

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
        const filtered = data.filter(o => o.source.includes(search) || o.target.some(t => t.uri.includes(search) || t.type.includes(search)));
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

            <TableContainer>
                <Table>
                    <SuggestionHeader onSearch={handleSearch}/>
                    <SuggestionList
                        rows={filteredData}
                        headers={headers}
                        onSwapAction={handleSwapAction}
                        onAdd={handleAdd}
                        onAskDiscardChanges={onAskDiscardChanges}
                    />
                </Table>
            </TableContainer>
        </Section>
    )
}
