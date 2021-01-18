import React, {useEffect, useRef, useState} from 'react';
import {
    Button,
    Divider,
    Grid,
    GridColumn,
    GridRow,
    Notification,
    Section,
    SectionHeader,
    TitleMainsection,
    TableContainer,
} from "@gui-elements/index";
import SuggestionList from "./SuggestionList";
import SuggestionHeader from "./SuggestionHeader";
import {generateRuleAsync, getSuggestionsAsync, prefixesAsync, schemaExampleValuesAsync} from "../../store";
import {IAddedSuggestion, ITransformedSuggestion} from "./suggestion.typings";

interface ISuggestionListContext {
    // Can be deleted when popup issue gone
    portalContainer: HTMLDivElement;
    // sharing example values for source data
    exampleValues: {
        [key: string]: string[]
    };
    // Table global search
    search: string;
    // indicator shows the swap state, by default it's true source->target
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
        (async function () {
            setLoading(true);
            try {
                await Promise.all([
                    loadData(isFromDataset),
                    loadExampleValues(),
                    loadPrefixes()
                ])
            } catch (e)  {
                setError([
                    ...error,
                    e
                ])
            } finally {
                setLoading(false);
            }
        })()
    }, []);

    const handleSwapAction = async () => {
        setIsFromDataset(!isFromDataset);
        setError([]);
        setLoading(true);
        try {
            await loadData(!isFromDataset);
        } catch (e) {
            setError(e);
        } finally {
            setLoading(false);
        }
    };

    const loadData = (matchFromDataset: boolean) => {
        return new Promise((resolve, reject) => {
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
                        reject(warnings);
                    }
                    setData(suggestions);
                    handleFilter(suggestions);
                    resolve(suggestions);
                },
                (error) => {
                    reject(error);
                }
            )
        });
    };

    const loadExampleValues = () => {
        return new Promise((resolve, reject) => {
            schemaExampleValuesAsync().subscribe(
                (data) => {
                    setExampleValues(data);
                    resolve(data);
                },
                err => {
                    reject(err);
                }
            );
        })

    };

    const loadPrefixes = () => {
        return new Promise((resolve, reject) => {
            prefixesAsync().subscribe(
                data => {
                    const arr = Object.keys(data).map(key => ({
                        key,
                        uri: data[key]
                    }));
                    setPrefixList(arr);
                    resolve(arr);
                },
                err => {
                    reject(err);
                }
            )
        });
    };

    const handleAdd = (selectedRows: IAddedSuggestion[], selectedPrefix?: string) => {
        setLoading(true);

        setError([]);

        const correspondences = selectedRows
            .map(suggestion => {
                const {source, targetUri, type} = suggestion;

                const correspondence = {
                    sourcePath: source,
                    targetProperty: targetUri || undefined,
                    type,
                };

                if (!isFromDataset) {
                    correspondence.sourcePath = targetUri;
                    correspondence.targetProperty = source || undefined;
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
                            <Button affirmative onClick={() => handleFilter(data)} data-test-id={'find_matches'}>Find
                                Matches</Button>
                        </GridColumn>
                    </GridRow>
                </Grid>
            </SectionHeader>
            <Divider addSpacing="medium"/>
            {
                (!loading && !!error.length) && <Notification danger>
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
                            loading={loading}
                        />
                    </SuggestionListContext.Provider>
                </div>
            </TableContainer>

        </Section>
    )
}
