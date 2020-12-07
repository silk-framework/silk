import React, { useEffect, useRef, useState } from 'react';
import {
    Button,
    Divider,
    Grid,
    GridColumn,
    GridRow,
    Section,
    SectionHeader,
    TitleMainsection,
    Notification
} from "@gui-elements/index";
import { TableContainer } from 'carbon-components-react';
import SuggestionList from "./SuggestionList";
import SuggestionHeader from "./SuggestionHeader";
import { generateRuleAsync, getSuggestionsAsync, prefixesAsync, schemaExampleValuesAsync } from "../../store";
import _ from "lodash";
import { IAddedSuggestion, ITransformedSuggestion } from "./suggestion.typings";

interface ISuggestionListContext {
    portalContainer: HTMLDivElement;
    exampleValues: {
        [key: string]: string[]
    };
    search: string;
    isFromDataset: boolean;
}

export const SuggestionListContext = React.createContext<ISuggestionListContext>({
    portalContainer: null,
    exampleValues: {},
    search: '',
    isFromDataset: true,
});

export default function SuggestionContainer({ruleId, targetClassUris, onAskDiscardChanges, onClose}) {
    // Loading indicator
    const [loading, setLoading] = useState(false);

    const [warnings, setWarnings] = useState<string[]>([]);

    const [error, setError] = useState<any[]>([]);

    const [data, setData] = useState<ITransformedSuggestion[]>([]);

    const [filteredData, setFilteredData] = useState<ITransformedSuggestion[]>([]);

    const [search, setSearch] = useState('');

    const [submittedSearch, setSubmittedSearch] = useState('');

    const [isFromDataset, setIsFromDataset] = useState(true);

    const [exampleValues, setExampleValues] = useState({});

    const [prefixList, setPrefixList] = useState([]);

    const portalContainerRef = useRef();

    useEffect(() => {
        loadData(isFromDataset);
        loadExampleValues();
        loadPrefixes();
    }, []);

    const handleSwapAction = () => {
        setIsFromDataset(!isFromDataset);

        setError([]);

        loadData(!isFromDataset);
    };

    const loadData = (matchFromDataset: boolean) => {
        setLoading(true);
        getSuggestionsAsync({
            targetClassUris,
            ruleId,
            matchFromDataset,
            nrCandidates: 20,
        }).subscribe(
            ({suggestions, warnings}) => {
                if (warnings.length) {
                    setError([
                        ...error,
                        ...warnings
                    ]);
                }
                setData(suggestions);
                handleFilter(suggestions);
                setLoading(false);
            },
            () => {
                setLoading(false);
            }
        );
    };

    const loadExampleValues = () => {
        schemaExampleValuesAsync().subscribe(
            (data) => {
                setExampleValues(data);
            },
            err => {
                setError([
                    ...error,
                    err,
                ]);
            }
        );
    };

    const loadPrefixes = () => {
        prefixesAsync().subscribe(
            data => {
                const arr = Object.keys(data).map(key => ({
                    key,
                    uri: data[key]
                }));
                setPrefixList(arr);
            },
            err => {
                setError([
                    ...error,
                    err,
                ]);
            }
        )
    };

    const handleAdd = (selectedRows: IAddedSuggestion[], selectedPrefix?: string) => {
        setLoading(true);

        setError([]);

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

        generateRuleAsync(correspondences, ruleId, selectedPrefix).subscribe(
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

    const handleFilter = (arrayData: ITransformedSuggestion[]) => {
        const filteredFields = ['uri', 'label', 'description'];
        const filtered = arrayData.filter(o =>
            o.source.includes(search) ||
            o.label?.includes(search) ||
            o.description?.includes(search) ||
            o.candidates.some(
                t => filteredFields.some(
                    field => t[field] ? t[field].includes(search) : false
                )
            )
        );
        setFilteredData(filtered);
        setSubmittedSearch(search);
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
                            <Button affirmative onClick={() => handleFilter(data)}>Find Matches</Button>
                        </GridColumn>
                    </GridRow>
                </Grid>
            </SectionHeader>
            <Divider addSpacing="medium"/>
            {
                !!error.length && <Notification danger>
                    <ul>
                    {
                        error.map(err => <>
                            <li key={err.detail}>
                                <h3>{err.title}</h3>
                                <p>{err.detail}</p>
                            </li>
                        </>)
                    }
                    </ul>
                </Notification>
            }
            <TableContainer>
                <div ref={portalContainerRef}>
                    <SuggestionListContext.Provider value={{
                        portalContainer: portalContainerRef.current,
                        exampleValues,
                        search: submittedSearch,
                        isFromDataset,
                    }}>
                        <SuggestionHeader onSearch={handleSearch}/>
                        <SuggestionList
                            rows={filteredData}
                            prefixList={prefixList}
                            onSwapAction={handleSwapAction}
                            onAdd={handleAdd}
                            onAskDiscardChanges={onAskDiscardChanges}
                        />
                    </SuggestionListContext.Provider>
                </div>
            </TableContainer>
        </Section>
    )
}
