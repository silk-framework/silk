import React, { useEffect, useState } from 'react';
import { Divider, Grid, GridColumn, GridRow, Section, SectionHeader, TitleMainsection } from "@gui-elements/index";
import { DataTable, Table, TableContainer } from 'carbon-components-react';
import SuggestionList from "./SuggestionList";
import SuggestionHeader from "./SuggestionHeader";
import { getSuggestionsAsync } from "../../store";
import _ from "lodash";
import { SUGGESTION_TYPES } from "../../utils/constants";

export default function SuggestionContainer({ruleId, targetClassUris}) {
    // Loading indicator
    const [loading, setLoading] = useState(false);

    const [warnings, setWarnings] = useState<string[]>([]);

    const [error, setError] = useState<any>({});

    const [data, setData] = useState([]);

    const [headers, setHeaders] = useState(
        [
            {header: 'Source data', key: 'sourcePath'},
            {header: null, key: 'swapAction'},
            {header: 'Target data', key: 'targetProperty'},
            {header: 'Mapping type', key: 'type'}
        ]
    );

    useEffect(() => {
        setLoading(true);
        loadData();
    }, []);

    const handleSwapAction = () => {
        const temp = headers[0];

        headers[0] = headers[2];
        headers[2] = temp;
        setHeaders(headers);

        loadData(temp.key === 'targetProperty');
    };

    const loadData = (matchFromDataset: boolean = true) => {
        getSuggestionsAsync({
            targetClassUris,
            ruleId,
            matchFromDataset,
            nrCandidates: 20,
        }).subscribe(
            ({suggestions, warnings}) => {
                const rawData = suggestions.map(value => ({
                    ...value,
                    checked: false,
                    type: value.type || SUGGESTION_TYPES[0],
                }));
                setWarnings(
                    warnings.filter(value => !_.isEmpty(value))
                );
                setLoading(false);
                setData(rawData);
            },
            err => {
                setLoading(false);
                setError(err)
            }
        );
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
                </Grid>
            </SectionHeader>
            <Divider addSpacing="medium"/>
            <DataTable
                isSortable={true}
                rows={data}
                headers={headers}
                render={({
                             rows, headers, getHeaderProps, getTableProps, getRowProps,
                             getSelectionProps, getBatchActionProps, onInputChange, selectedRows
                         }) => (
                    <TableContainer>
                        <Table {...getTableProps()}>

                            <SuggestionHeader
                                getBatchActionProps={getBatchActionProps}
                                onInputChange={onInputChange}
                            />
                            <SuggestionList
                                rows={rows}
                                headers={headers}
                                onSwapAction={handleSwapAction}
                                getSelectionProps={getSelectionProps}
                                getRowProps={getRowProps}
                                getHeaderProps={getHeaderProps}
                            />
                        </Table>
                    </TableContainer>
                )}
            />
        </Section>
    )
}
