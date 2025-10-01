import React, { useEffect, useState } from "react";
import {
    Button,
    Card,
    CardContent,
    CardHeader,
    CardOptions,
    CardTitle,
    Divider,
    Markdown,
    Notification,
    Spacing,
    TableContainer,
    highlighterUtils,
} from "@eccenca/gui-elements";
import SuggestionList from "./SuggestionList";
import SuggestionHeader from "./SuggestionHeader";
import {
    generateRuleAsync,
    getSuggestionsAsync,
    prefixesAsync,
    schemaExampleValuesAsync,
    useApiDetails,
} from "../../store";
import {
    IAddedSuggestion,
    IPrefix,
    ISuggestionCandidate,
    ITransformedSuggestion,
    IVocabularyInfo,
    SuggestionIssues,
} from "./suggestion.typings";
import silkApi from "../../../api/silkRestApi";
import VocabularyMatchingDialog from "./VocabularyMatchingDialog";
import { useInitFrontend } from "../../../api/silkRestApi.hooks";
import ErrorView from "../../components/ErrorView";
import _ from "lodash";
import { useTranslation } from "react-i18next";
import { GlobalMappingEditorContext } from "../../../contexts/GlobalMappingEditorContext";
import {IInitFrontend} from "@ducks/common/typings";

interface ISuggestionListContext {
    // Can be deleted when popup issue gone
    portalContainer: HTMLElement | undefined;
    // sharing example values for source data
    exampleValues: {
        [key: string]: string[];
    };
    // Table global search
    search: string;
    // indicator shows the swap state, by default it's true source->target
    isFromDataset: boolean;
    // Needed to create DM links
    frontendInitData?: IInitFrontend;
    // Flag if vocabularies are available to match against
    vocabulariesAvailable: boolean;
    // Fetch target property suggestions from the vocabulary cache
    fetchTargetPropertySuggestions?: (textQuery: string) => Promise<ISuggestionCandidate[]>;
}

export const SuggestionListContext = React.createContext<ISuggestionListContext>({
    portalContainer: undefined,
    exampleValues: {},
    search: "",
    isFromDataset: true,
    vocabulariesAvailable: false,
});

interface IProps {
    ruleId: string;
    targetClassUris: string[];
    onAskDiscardChanges: (ruleId: string) => any;
    onClose: () => any;
    selectedVocabs: string[];
    setSelectedVocabs: (vocabs: string[]) => any;
}

/** The mapping suggestion widget */
export default function SuggestionContainer({
    ruleId,
    targetClassUris,
    onAskDiscardChanges,
    onClose,
    selectedVocabs,
    setSelectedVocabs,
}: IProps) {
    const mappingEditorContext = React.useContext(GlobalMappingEditorContext);
    // Loading indicator
    const [loading, setLoading] = useState(false);

    const [vocabularies, setVocabularies] = useState<IVocabularyInfo[] | undefined>(undefined);

    const [showMatchingDialog, setShowMatchingDialog] = useState<boolean>(false);

    const [error, setError] = useState<any[]>([]);

    const [data, setData] = useState<ITransformedSuggestion[]>([]);

    const [suggestionsIssues, setSuggestionIssues] = useState<SuggestionIssues | undefined>(undefined);

    const [filteredData, setFilteredData] = useState<ITransformedSuggestion[]>([]);

    const [search, setSearch] = useState("");

    const [submittedSearch, setSubmittedSearch] = useState("");

    const [isFromDataset, setIsFromDataset] = useState(true);

    const [exampleValues, setExampleValues] = useState({});

    const [prefixList, setPrefixList] = useState<IPrefix[]>([]);

    const [t] = useTranslation();

    const vocabulariesAvailable = vocabularies !== undefined && vocabularies.length > 0;

    const noVocabsAvailable = vocabularies && vocabularies.length === 0;

    const frontendInitData = useInitFrontend();
    const apiDetails = useApiDetails();

    // Updates the current error array depending on the type of the added error object
    const setErrorSafe = (newErrors: any | any[], keepOldErrors: boolean = true) => {
        // Convert different error formats
        const convertError = (err: any) => err?.error ?? err;
        if (Array.isArray(newErrors)) {
            setError((oldErrors) => [
                ...(keepOldErrors && oldErrors ? oldErrors : []),
                ...newErrors.map((err) => convertError(err)),
            ]);
        } else if (typeof newErrors === "object") {
            setError((oldErrors) => [...(keepOldErrors ? oldErrors : []), convertError(newErrors)]);
        }
    };

    const { project, transformTask } = apiDetails;

    useEffect(() => {
        fetchVocabularyInfos();
    }, []);

    const handleSelectedVocabs = (selected: IVocabularyInfo[]) => {
        setSelectedVocabs(selected.map((v) => v.uri));
    };

    // Fetch vocabulary information for the transform task, i.e. the available vocabs.
    const fetchVocabularyInfos = () => {
        silkApi
            .retrieveTransformVocabularyInfos(project as string, transformTask as string)
            .then(({ data }) => {
                setVocabularies(data.vocabularies);
            })
            .catch((err) => {
                // TODO: When mapping and workspace code bases are merged, add error handling
                setVocabularies([]);
            });
    };

    // Fetch target properties from the available target properties based on a text query
    const fetchTargetPropertySuggestions = async (textQuery: string): Promise<ISuggestionCandidate[]> => {
        const maxResults = 20;
        try {
            const { data } = await silkApi.retrieveTransformTargetProperties(
                project as string,
                transformTask as string,
                ruleId,
                textQuery,
                maxResults,
                selectedVocabs,
                true,
                mappingEditorContext.taskContext
            );
            if (Array.isArray(data)) {
                return data.map((tp) => {
                    return {
                        uri: tp.value,
                        label: tp.label,
                        confidence: 0,
                        type: tp.extra.type,
                        graph: tp.extra.graph, // TODO: This might be prefixed
                        description: tp.description,
                    };
                });
            } else {
                return [];
            }
        } catch (err) {
            // TODO: When mapping and workspace code bases are merged, add error handling
            return [];
        }
    };

    // React to search input
    useEffect(() => {
        if (data.length > 0) {
            handleFilter(data);
        }
    }, [search, data]);

    // As soon as the initial vocabularies are loaded, fetch the actual data, i.e. source paths, matchings etc.
    useEffect(() => {
        if (vocabularies) {
            (async function () {
                setLoading(true);
                try {
                    await Promise.all([
                        loadVocabularyMatches(isFromDataset, false, vocabulariesAvailable),
                        loadExampleValues(),
                        loadPrefixes(),
                    ]);
                } catch (e) {
                    setErrorSafe(e);
                } finally {
                    setLoading(false);
                }
            })();
        }
    }, [vocabularies]);

    // Swapping between source and target (vocabulary) view
    const handleSwapAction = async () => {
        setIsFromDataset(!isFromDataset);
        setError([]);
        try {
            await loadVocabularyMatches(!isFromDataset, true, vocabulariesAvailable);
        } catch (e) {
            setErrorSafe(e, false);
        }
    };

    // Fetches necessary data to generate the mapping suggestions
    const loadVocabularyMatches = (
        matchFromDataset: boolean,
        setLoader: boolean,
        executeMatching: boolean,
        selectedVocabularies?: string[]
    ) => {
        const vocabs = selectedVocabularies ? selectedVocabularies : selectedVocabs;
        setData([]);
        setError([]);
        setSuggestionIssues(undefined);
        return new Promise((resolve, reject) => {
            setLoader && setLoading(true);
            getSuggestionsAsync(
                {
                    targetClassUris,
                    ruleId,
                    matchFromDataset,
                    nrCandidates: 20,
                    targetVocabularies: vocabs && vocabs.length > 0 ? vocabs : undefined,
                },
                executeMatching
            ).subscribe(
                ({ suggestions, warnings, suggestionIssues }) => {
                    try {
                        if (warnings.length) {
                            reject(warnings);
                        }
                        setData(suggestions);
                        setSuggestionIssues(suggestionIssues);
                        handleFilter(suggestions);
                        resolve(suggestions);
                    } finally {
                        setLoader && setLoading(false);
                    }
                },
                (error) => {
                    setLoader && setLoading(false);
                    reject(error);
                }
            );
        });
    };

    // Load example values for the source paths. This will be shown in an info box / tooltip
    const loadExampleValues = () => {
        return new Promise((resolve, reject) => {
            schemaExampleValuesAsync(ruleId).subscribe(
                (data) => {
                    setExampleValues(data);
                    resolve(data);
                },
                (err) => {
                    reject(err);
                }
            );
        });
    };

    // Load prefixes. These are needed for generating target properties automatically based on the the source name.
    const loadPrefixes = () => {
        return new Promise((resolve, reject) => {
            prefixesAsync().subscribe(
                (data) => {
                    const arr: any[] = Object.keys(data).map((key) => {
                        return {
                            key,
                            uri: data[key],
                        };
                    });
                    setPrefixList(arr);
                    resolve(arr);
                },
                (err) => {
                    reject(err);
                }
            );
        });
    };

    // Add mapping suggestions, i.e. generate mapping rules from selected mapping suggestions.
    const handleAdd = (selectedRows: IAddedSuggestion[], selectedPrefix?: string) => {
        setLoading(true);

        setError([]);

        const correspondences = selectedRows.map((suggestion) => {
            const { source, targetUri, type } = suggestion;

            const correspondence = {
                sourcePath: source,
                targetProperty: targetUri || undefined,
                type,
            };

            if (!isFromDataset) {
                correspondence.sourcePath = targetUri;
                correspondence.targetProperty = source || undefined;
            }

            return correspondence;
        });

        generateRuleAsync(correspondences, ruleId, selectedPrefix).subscribe(
            () => onClose(),
            (err) => {
                // If we have a list of failedRules, we want to show them, otherwise something
                // else failed
                const errorBody = _.get(err, "response.body");
                const error: any[] = err.failedRules ? err.failedRules : [{ error: { ...errorBody, ...err } }];
                error
                    .filter((err) => err?.error)
                    .forEach(
                        (err) => (err.error.titlePrefix = "There has been a problem generating the mapping rules: ")
                    );
                setErrorSafe(error);
                setLoading(false);
            },
            () => setLoading(false)
        );
    };

    // Search value submitted for filtering the mapping suggestion table based on a multi-word text query
    const handleSearch = (value: string) => {
        setSearch(value);
    };

    // Extracts the relevant text of a source or target item used for text filtering
    const itemText = (item: ITransformedSuggestion | ISuggestionCandidate): string => {
        const title = item.label ? `${item.label} ${item.uri}` : item.uri;
        const description = item.description || "";
        return `${title} ${description}`;
    };

    /** Filters the table based on the search query. */
    const handleFilter = (inputMappingSuggestions: ITransformedSuggestion[]) => {
        let filtered: ITransformedSuggestion[] = inputMappingSuggestions;
        if (search.trim() !== "") {
            filtered = [];
            const searchWords = highlighterUtils.extractSearchWords(search, true);
            inputMappingSuggestions.forEach((suggestion) => {
                const sourceText = itemText(suggestion);
                let targetCandidate = suggestion.candidates.length > 0 && suggestion.candidates[0];
                const targetCandidateText = targetCandidate ? itemText(targetCandidate) : "";
                const matchText = `${sourceText} ${targetCandidateText}`.toLowerCase();
                if (highlighterUtils.matchesAllWords(matchText, searchWords)) {
                    filtered.push(suggestion);
                }
            });
        }
        setFilteredData(filtered);
        setSubmittedSearch(search);
    };

    // Actions regarding vocabulary matching, e.g. matching dialog
    const mappingOptions = (
        <CardOptions>
            {vocabulariesAvailable && (
                <Button onClick={() => setShowMatchingDialog(true)} data-test-id={"find_matches"}>
                    {selectedVocabs.length > 0 && vocabularies
                        ? `Selected vocabularies (${selectedVocabs.length}/${vocabularies.length})`
                        : "Select vocabularies"}
                </Button>
            )}
        </CardOptions>
    );

    // Error widget that displays errors that have occurred
    const errorWidget = !loading && !!error.length && (
        <>
            {error.map((err) => (
                <ErrorView {...err} />
            ))}
            <Spacing size="small" />
        </>
    );

    // Widget that shows infos about the vocabularies, e.g. that no vocab is loaded.
    const vocabularyInfoNotification = noVocabsAvailable && (
        <>
            <Notification>
                There is currently no vocabulary loaded for this transformation. Vocabulary matching is not available.
            </Notification>
            <Spacing size="small" />
        </>
    );

    const SuggestionIssues = () => {
        if (suggestionsIssues && suggestionsIssues.notFoundClasses.length > 0) {
            const noClassFound =
                targetClassUris.length > 0 && suggestionsIssues.notFoundClasses.length === targetClassUris.length;
            const classesString = suggestionsIssues.notFoundClasses.map((c) => `\`${c}\``).join(", ");
            let message: string = t("MappingSuggestion.suggestionIssues.notAllClassesFound", {
                classesString,
                numberOfFoundClasses: targetClassUris.length - suggestionsIssues.notFoundClasses.length,
                numberOfAllClasses: targetClassUris.length,
            });
            if (noClassFound) {
                message = t("MappingSuggestion.suggestionIssues.noFoundClass");
            }
            return (
                <>
                    <Notification intent="warning">
                        <Markdown data-test-id={"mapping-suggestion-issues"}>{message}</Markdown>
                    </Notification>
                    <Spacing />
                </>
            );
        } else {
            return null;
        }
    };

    // Execute vocabulary matching from vocabulary matching dialog
    const executeVocabMatchingFromDialog = async (vocabs) => {
        try {
            await loadVocabularyMatches(isFromDataset, true, true, vocabs);
        } catch (e) {
            setErrorSafe(e, false);
        }
    };

    const askForDiscardFn = () => onAskDiscardChanges(ruleId);

    return (
        <Card>
            <CardHeader>
                <CardTitle>Mapping Suggestions</CardTitle>
                {mappingOptions}
            </CardHeader>
            <Divider />
            <CardContent>
                {errorWidget}
                {vocabularyInfoNotification}
                <SuggestionIssues />
                {
                    <SuggestionListContext.Provider
                        value={{
                            portalContainer: document.body,
                            exampleValues,
                            search: submittedSearch,
                            isFromDataset,
                            frontendInitData,
                            vocabulariesAvailable,
                            fetchTargetPropertySuggestions,
                        }}
                    >
                        <SuggestionHeader onSearch={handleSearch} />
                        <Spacing size="tiny" />
                        <TableContainer>
                            <SuggestionList
                                rows={filteredData}
                                prefixList={prefixList}
                                onSwapAction={handleSwapAction}
                                onAdd={handleAdd}
                                onClose={onClose}
                                onAskDiscardChanges={askForDiscardFn}
                                loading={loading}
                            />
                        </TableContainer>
                        {showMatchingDialog && vocabulariesAvailable && (
                            <VocabularyMatchingDialog
                                availableVocabularies={vocabularies as IVocabularyInfo[]}
                                onClose={() => setShowMatchingDialog(false)}
                                executeMatching={executeVocabMatchingFromDialog}
                                onSelection={handleSelectedVocabs}
                                preselection={selectedVocabs}
                            />
                        )}
                    </SuggestionListContext.Provider>
                }
            </CardContent>
        </Card>
    );
}
