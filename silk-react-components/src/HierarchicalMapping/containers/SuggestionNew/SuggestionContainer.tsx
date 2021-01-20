import React, {useEffect, useRef, useState} from 'react';
import {
    Button,
    Card,
    CardTitle,
    CardHeader,
    CardContent,
    CardOptions,
    Divider,
    Spacing,
    Grid,
    GridColumn,
    GridRow,
    Notification,
    Section,
    SectionHeader,
    TitleMainsection,
    TableContainer, Spacing,
} from "@gui-elements/index";
import SuggestionList from "./SuggestionList";
import SuggestionHeader from "./SuggestionHeader";
import {
    generateRuleAsync,
    getApiDetails,
    getSuggestionsAsync,
    prefixesAsync,
    schemaExampleValuesAsync
} from "../../store";
import {IAddedSuggestion, ITransformedSuggestion, IVocabularyInfo} from "./suggestion.typings";
import silkApi from "../../../api/silkRestApi";
import VocabularyMatchingDialog from "./VocabularyMatchingDialog";

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

    const [vocabularies, setVocabularies] = useState<IVocabularyInfo[] | undefined>(undefined)

    const [showMatchingDialog, setShowMatchingDialog] = useState<boolean>(false)

    const [error, setError] = useState<any[]>([]);

    const [data, setData] = useState<ITransformedSuggestion[]>([]);

    const [filteredData, setFilteredData] = useState<ITransformedSuggestion[]>([]);

    const [search, setSearch] = useState('');

    const [submittedSearch, setSubmittedSearch] = useState('');

    const [isFromDataset, setIsFromDataset] = useState(true);

    const [exampleValues, setExampleValues] = useState({});

    const [prefixList, setPrefixList] = useState([]);

    const portalContainerRef = useRef();

    const vocabsAvailable = vocabularies && vocabularies.length > 0

    useEffect(() => {
        fetchVocabularyInfos()
    }, [])

    // Fetch vocabulary information for the transform task, i.e. the available vocabs.
    const fetchVocabularyInfos = () => {
        const {baseUrl, project, transformTask} = getApiDetails()
        silkApi.retrieveTransformVocabularyInfos(baseUrl, project, transformTask)
            .then(({ data }) => {
                setVocabularies(data.vocabularies)
            })
            .catch(err => {
                // TODO: error handling
                setVocabularies([])
            })
    }

    useEffect(() => {
        if(vocabularies) {
            (async function () {
                setLoading(true);
                try {
                    await Promise.all([
                        loadVocabularyMatches(isFromDataset, false, vocabsAvailable),
                        loadExampleValues(),
                        loadPrefixes()
                    ])
                } catch (e) {
                    setError([
                        ...error,
                        e
                    ])
                } finally {
                    setLoading(false);
                }
            })()
        }
    }, [vocabularies]);

    const handleSwapAction = async () => {
        setIsFromDataset(!isFromDataset);
        setError([]);
        try {
            await loadVocabularyMatches(!isFromDataset, true, vocabsAvailable);
        } catch (e) {
            setError(e);
        }
    };

    const loadVocabularyMatches = (matchFromDataset: boolean, setLoader: boolean, executeMatching: boolean, selectedVocabularies?: string[]) => {
        return new Promise((resolve, reject) => {
            setLoader && setLoading(true)
            getSuggestionsAsync(
                {
                    targetClassUris,
                    ruleId,
                    matchFromDataset,
                    nrCandidates: 20,
                    targetVocabularies: selectedVocabularies && selectedVocabularies.length > 0 ? selectedVocabularies : undefined,
                },
                executeMatching
            ).subscribe(
                ({suggestions, warnings}) => {
                    try {
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
                    } finally {
                        setLoader && setLoading(false)
                    }
                },
                (error) => {
                    setLoader && setLoading(false)
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
        <Card>
            <CardHeader>
                <CardTitle>
                   Mapping Suggestions
                </CardTitle>
                <CardOptions>
                    { vocabsAvailable && (
                        <Button affirmative onClick={() => setShowMatchingDialog(true)} data-test-id={'find_matches'}>
                            Find Matches
                        </Button>
                    )}
                </CardOptions>
            </CardHeader>
            <Divider />
            <CardContent>
                {
                    (!loading && !!error.length) && <>
                        <Notification danger>
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
                        <Spacing size="small" />
                    </>
                }
                {
                    (loading || !error.length) && <div ref={portalContainerRef}>
                        <SuggestionListContext.Provider value={{
                            portalContainer: portalContainerRef.current,
                            exampleValues,
                            search: submittedSearch,
                            isFromDataset,
                        }}>
                            <SuggestionHeader onSearch={handleSearch} />
                            <Spacing size="tiny" />
                            <TableContainer>
                                <SuggestionList
                                    rows={filteredData}
                                    prefixList={prefixList}
                                    onSwapAction={handleSwapAction}
                                    onAdd={handleAdd}
                                    onAskDiscardChanges={onAskDiscardChanges}
                                    loading={loading}
                                />
                            </TableContainer>
                            { showMatchingDialog && vocabsAvailable && <VocabularyMatchingDialog
                                availableVocabularies={vocabularies}
                                onClose={() => setShowMatchingDialog(false)}
                                executeMatching={(vocabs) => loadVocabularyMatches(isFromDataset, true, true, vocabs)}
                            /> }
                        </SuggestionListContext.Provider>
                    </div>
                }
            </CardContent>
        </Card>
    )
}
