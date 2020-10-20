import React, { useEffect, useState } from 'react';
import { Divider, Grid, GridColumn, GridRow, Section, SectionHeader, TitleMainsection } from "@gui-elements/index";
import { Table, TableContainer } from 'carbon-components-react';
import SuggestionList  from "./SuggestionList";
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
            },
            err => {
                setLoading(false);
                setError(err)
            }
        );
    };

    const handleAdd = (selectedRows) => {
        setLoading(true);
        const correspondences = selectedRows
            .map(suggestion => ({
                sourcePath: suggestion.source,
                targetProperty: suggestion.target[0].uri,
                type: suggestion.target[0].type,
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
    return (
        <Section>
            <SectionHeader>
                <Grid>
                    <GridRow>
                        <GridColumn small verticalAlign="center">
                            <TitleMainsection>Mapping Suggestion for {ruleId}</TitleMainsection>
                        </GridColumn>
                    </GridRow>
                </Grid>
            </SectionHeader>
            <Divider addSpacing="medium"/>

            <TableContainer>
                <Table>
                    <SuggestionHeader/>
                    <SuggestionList
                        rows={data}
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
