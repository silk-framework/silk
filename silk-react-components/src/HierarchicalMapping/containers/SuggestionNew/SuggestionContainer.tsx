import React, { useEffect, useState } from 'react';
import { Divider, Grid, GridColumn, GridRow, Section, SectionHeader, TitleMainsection } from "@gui-elements/index";
import { DataTable, Table, TableContainer } from 'carbon-components-react';
import SuggestionList from "./SuggestionList";
import SuggestionHeader from "./SuggestionHeader";
import { getSuggestionsAsync } from "../../store";
import _ from "lodash";

export default function SuggestionContainer({ruleId, targetClassUris}) {
    // Loading indicator
    const [loading, setLoading] = useState(false);

    const [warnings, setWarnings] = useState<string[]>([]);

    const [error, setError] = useState<any>({});

    const [data, setData] = useState({});

    const [headers, setHeaders] = useState(
        [
            {header: 'Source data', key: 'mapFrom'},
            {header: null, key: 'swapAction'},
            {header: 'Target data', key: 'mapTo'},
            {header: 'Mapping type', key: 'type'}
        ]
    );

    const [isFromDataset, setIsFromDataset] = useState(true);

    useEffect(() => {
        setLoading(true);
        loadData(isFromDataset);
    }, []);

    const handleSwapAction = () => {
        // const temp = headers[0];
        //
        // headers[0] = headers[2];
        // headers[2] = temp;
        // setHeaders(headers);
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
                            <SuggestionHeader />
                            <SuggestionList
                                rows={data}
                                headers={headers}
                                onSwapAction={handleSwapAction}
                            />
                        </Table>
                    </TableContainer>
        </Section>
    )
}
