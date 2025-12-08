// Store specific to hierarchical mappings, will use silk-store internally

import _ from "lodash";
import rxmq, { Rx } from "ecc-messagebus";
import {
    isRootOrObjectRule,
    MAPPING_RULE_TYPE_COMPLEX,
    MAPPING_RULE_TYPE_COMPLEX_URI,
    MAPPING_RULE_TYPE_DIRECT,
    MAPPING_RULE_TYPE_OBJECT,
    MAPPING_RULE_TYPE_ROOT,
    MAPPING_RULE_TYPE_URI,
    MESSAGES,
} from "./utils/constants";
import EventEmitter from "./utils/EventEmitter";
import React, { useState } from "react";
import silkApi, { HttpResponsePromise } from "../api/silkRestApi";
import { ITransformedSuggestion, SuggestionIssues } from "./containers/SuggestionNew/suggestion.typings";
import {
    CodeAutocompleteFieldPartialAutoCompleteResult,
    CodeAutocompleteFieldValidationResult,
} from "@eccenca/gui-elements/src/components/AutoSuggestion/AutoSuggestion";
import { CONTEXT_PATH } from "../../../../constants/path";
import { TaskContext } from "../../../shared/projectTaskTabView/projectTaskTabView.typing";

export const silkStore = rxmq.channel("silk.api");
export const errorChannel = rxmq.channel("errors");

let rootId = null;

const vocabularyCache = {};

interface IApiDetails {
    project?: string;
    transformTask?: string;
}

let _setApiDetails: React.Dispatch<React.SetStateAction<IApiDetails>> | undefined = undefined;
let _apiDetails: IApiDetails = {};
export const setApiDetails = (data: IApiDetails) => {
    const details = { ...data };
    if (_setApiDetails) {
        _setApiDetails(details);
    }
    _apiDetails = details;
};
export const getApiDetails = (): IApiDetails => _apiDetails;

/** API details hook. Makes sure that a component gets the API details. */
export const useApiDetails = () => {
    const [apiDetails, setApiDetails] = useState<IApiDetails>({});
    _setApiDetails = setApiDetails;
    if (apiDetails.project === undefined && typeof _apiDetails.project === "string") {
        setApiDetails(_apiDetails);
    }
    return apiDetails;
};

/** Make sure all API details are set. */
export function getDefinedApiDetails() {
    const apiDetails = getApiDetails();
    if (!apiDetails.project || !apiDetails.transformTask) {
        throw new Error("Requested API details, but API details are not set!");
    }
    return {
        project: apiDetails.project as string,
        transformTask: apiDetails.transformTask as string,
    };
}

const mapPeakResult = (returned) => {
    return {
        example: returned.body,
    };
};

const editMappingRule = (payload, id, parent) => {
    if (id) {
        return silkStore.request({
            topic: "transform.task.rule.put",
            data: {
                ...getApiDetails(),
                ruleId: id,
                payload,
            },
        });
    }

    return silkStore.request({
        topic: "transform.task.rule.rules.append",
        data: {
            ...getApiDetails(),
            ruleId: parent,
            payload,
        },
    });
};

function findRule(curr, id, isObjectMapping, breadcrumbs) {
    const element = {
        ...curr,
        breadcrumbs,
    };

    if (element.id === id || _.get(element, "rules.uriRule.id") === id) {
        return element;
    } else if (_.has(element, "rules.propertyRules")) {
        let result: any = null;
        const bc = [
            ...breadcrumbs,
            {
                id: element.id,
                type: _.get(element, "rules.typeRules[0].typeUri", false),
                property: _.get(element, "mappingTarget.uri", false),
            },
        ];
        _.forEach(element.rules.propertyRules, (child) => {
            if (result === null) {
                result = findRule(child, id, isObjectMapping, bc);
            }
        });

        if (isObjectMapping && result !== null && !isRootOrObjectRule(result.type)) {
            result = element;
        }

        return result;
    }
    return null;
}

const handleCreatedSelectBoxValue = (data, path): any => {
    if (_.has(data, [path, "value"])) {
        return _.get(data, [path, "value"]);
    }
    // the select boxes return an empty array when the user delete the existing text,
    // instead of returning an empty string
    if (_.isEmpty(_.get(data, [path]))) {
        return "";
    }

    return _.get(data, [path]);
};

export interface IMetaData {
    // A human-readable label
    label: string;
    // An optional description
    description?: string;
}

/** The value type of a target property, e.g. string, int, language tag etc. */
export interface IValueType {
    /** Node type ID, e.g. "uri", "lang", "StringValueType" etc. */
    nodeType: string;
    /** If this is a custom data type, this specifies the URI of the data type. */
    uri?: string;
    /** If this is a language tagged property, this specifies the language. */
    lang?: string;
}

/** The target of a mapping rule. */
export interface IMappingTarget {
    /** Target URI, not necessarily a URI, this depends on the target dataset, e.g. this could be any string when writing to JSON. */
    uri: string;
    /** The value type, e.g. string, int, URI etc. */
    valueType: IValueType;
    /** Special attribute which only has relevance when mapping to XML. If true this will become an attribute. */
    isAttribute?: boolean;
    /** If true the generated property will have a reversed direction. This only applies to graph datasets, i.e. RDF. */
    isBackwardProperty?: boolean;
}

/** The base interface for all mapping rules. */
export interface ITransformRule {
    /** The (unique) ID of the mapping rule. */
    id?: string;
    /** The type of the mapping. */
    type?: MappingType;
    /** Meta data of the mapping rule. */
    metadata: IMetaData;
}

/** Interface of a value mapping. */
export interface IValueMapping extends ITransformRule {
    /** The mapping target. */
    mappingTarget: IMappingTarget;
    /** The source (Silk) path expression. */
    sourcePath?: string;
}

export interface IObjectMapping extends ITransformRule {
    /** The mapping target. */
    mappingTarget: IMappingTarget;
    /** The source (Silk) path expression. */
    sourcePath?: string;
    /** The child mapping rules of this object mapping. */
    rules: any; // FIXME: Improve type
}

interface IProps {
    comment?: string;
    label: string;
    isAttribute: boolean;
    type: MappingType;
    sourceProperty: string;
    id: string;
}

type MappingType = "direct" | "complex" | "object" | "uri" | "complexUri";

/** Construct the payload for a value mapping. */
const prepareValueMappingPayload = (data: IProps) => {
    const payload: IValueMapping = {
        metadata: {
            description: data.comment,
            label: data.label,
        },
        mappingTarget: {
            uri: handleCreatedSelectBoxValue(data, "targetProperty"),
            valueType: handleCreatedSelectBoxValue(data, "valueType"),
            isAttribute: data.isAttribute,
        },
    };

    if (data.type === MAPPING_RULE_TYPE_DIRECT) {
        payload.sourcePath = data.sourceProperty ? handleCreatedSelectBoxValue(data, "sourceProperty") : "";
        payload.type = MAPPING_RULE_TYPE_DIRECT;
    }

    if (!data.id) {
        payload.type = data.type;
    }

    return payload;
};

const prepareObjectMappingPayload = (data) => {
    const typeRules = _.map(data.targetEntityType, (typeRule) => {
        const value = _.get(typeRule, "value", typeRule);

        return {
            type: "type",
            typeUri: value,
        };
    });

    const payload: IObjectMapping = {
        metadata: {
            description: data.comment,
            label: data.label,
        },
        mappingTarget: {
            uri: handleCreatedSelectBoxValue(data, "targetProperty"),
            isBackwardProperty: data.entityConnection,
            valueType: {
                nodeType: "UriValueType",
            },
            isAttribute: data.isAttribute,
        },
        sourcePath: data.sourceProperty ? handleCreatedSelectBoxValue(data, "sourceProperty") : "",
        rules: {
            uriRule: data.pattern
                ? {
                      type: MAPPING_RULE_TYPE_URI,
                      pattern: data.pattern,
                  }
                : // URI pattern should be reset when set to null
                  data.pattern === null
                  ? null
                  : undefined,
            typeRules,
        },
    };

    if (!data.id) {
        payload.type = MAPPING_RULE_TYPE_OBJECT;
        payload.rules.propertyRules = [];
    }

    return payload;
};

const generateRule = (rule, parentId) =>
    createGeneratedMappingAsync({
        ...rule,
        parentId,
    }).catch((e) => Rx.Observable.return({ error: e, rule }));

const createGeneratedRules = ({ rules, parentId }) =>
    Rx.Observable.from(rules)
        .flatMapWithMaxConcurrent(5, (rule) => Rx.Observable.defer(() => generateRule(rule, parentId)))
        .reduce((all, result, idx) => {
            const total = _.size(rules);
            const count = idx + 1;
            EventEmitter.emit(MESSAGES.RULE.SUGGESTIONS.PROGRESS, {
                progressNumber: _.round((count / total) * 100, 0),
                lastUpdate: `Saved ${count} of ${total} rules.`,
            });

            all.push(result);

            return all;
        }, [])
        .map((createdRules) => {
            const failedRules = _.filter(createdRules, "error");

            if (_.size(failedRules)) {
                const error: Error & { failedRules?: any[] } = new Error("Could not create rules.");
                error.failedRules = failedRules;
                throw error;
            }
            return createdRules;
        });

// PUBLIC API
export const orderRulesAsync = ({ id, childrenRules }) => {
    silkStore
        .request({
            topic: "transform.task.rule.rules.reorder",
            data: { id, childrenRules, ...getApiDetails() },
        })
        .map(() => {
            return EventEmitter.emit(MESSAGES.RELOAD);
        });
};

export const generateRuleAsync = (correspondences, parentId, uriPrefix) => {
    return silkStore
        .request({
            topic: "transform.task.rule.generate",
            data: { ...getApiDetails(), correspondences, parentId, uriPrefix },
        })
        .map((returned) => {
            return {
                rules: _.get(returned, ["body"], []),
                parentId,
            };
        })
        .flatMap(createGeneratedRules)
        .map(() => {
            EventEmitter.emit(MESSAGES.RULE_VIEW.CLOSE, { id: 0 });
            return EventEmitter.emit(MESSAGES.RELOAD, true);
        });
};

/** Updates an entry in the vocabulary cache, but only if it exists. */
export const updateVocabularyCacheEntry = (uri: string, label: string | undefined, description: string | undefined) => {
    if (vocabularyCache[uri]) {
        vocabularyCache[uri].label = label;
        vocabularyCache[uri].description = description;
    }
};

export const getVocabInfoAsync = (uri, field) => {
    const path = [uri, field];

    if (_.has(vocabularyCache, path)) {
        return Rx.Observable.just({
            info: _.get(vocabularyCache, path),
        });
    }
    return silkStore
        .request({
            topic: "transform.task.targetVocabulary.typeOrProperty",
            data: { ...getApiDetails(), uri },
        })
        .catch(() => Rx.Observable.just({}))
        .map((returned) => {
            const info = _.get(returned, ["body", "genericInfo", field], null);

            _.set(vocabularyCache, path, info);

            return {
                info,
            };
        });
};

interface ISuggestAsyncProps {
    // Restrict matching by a list of target class URIs
    targetClassUris: string[];
    // (Root / object) rule ID this matching is done for
    ruleId: string;
    // If the matching should be done from source view, else it will be from vocabulary view
    matchFromDataset: boolean;
    // The max. number of returned candidates per source path / property. Defaults to 1.
    nrCandidates?: number;
    // Optional list of target vocabulary URIs / IDs to restrict the vocabularies to match against.
    targetVocabularies?: string[];
}

// Fetches vocabulary matching results from the DI matchVocabularyClassDataset endpoint
const fetchVocabularyMatchingResults = (data: ISuggestAsyncProps) => {
    return silkStore
        .request({
            topic: "transform.task.rule.suggestions",
            data: { ...getApiDetails(), ...data },
        })
        .catch((err) => {
            // It comes always {title: "Not Found", detail: "Not Found"} when the endpoint is not found.
            // see: SilkErrorHandler.scala
            const errorBody = _.get(err, "response.body");

            if (err.status === 404 && errorBody.title === "Not Found" && errorBody.detail === "Not Found") {
                return Rx.Observable.return(null);
            }
            errorBody.code = err.status;
            return Rx.Observable.return({ error: errorBody });
        })
        .map((returned) => {
            const data = _.get(returned, "body", {});
            const error = _.get(returned, "error", []);

            if (error) {
                return {
                    error,
                };
            }
            return {
                data,
            };
        });
};

/** Fetches (unused) source value paths to prevent showing matches of already mapped source paths. */
const fetchValueSourcePaths = (data: ISuggestAsyncProps) => {
    return silkStore
        .request({
            // call the silk endpoint valueSourcePathsInfo
            topic: "transform.task.rule.valueSourcePathsInfo",
            data: { ...getApiDetails(), ...data },
        })
        .catch((err) => {
            let errorBody = _.get(err, "response.body");
            if (errorBody) {
                errorBody.code = err.status;
            } else if (err.detail && !err.status) {
                errorBody = { title: "There has been a connection problem.", detail: err.detail, cause: null };
            } else {
                return Rx.Observable.return({});
            }
            return Rx.Observable.return({ error: errorBody });
        })
        .map((returned) => {
            const data = _.get(returned, "body", []);
            const error = _.get(returned, "error", []);
            if (error) {
                return {
                    error,
                };
            }
            return {
                data,
            };
        });
};

/** Empty matching results in case no matching is executed. */
const emptyMatchResult = new Promise((resolve) => resolve({ data: [] }));

/** Fetches all suggestions. */
export const getSuggestionsAsync = (data: ISuggestAsyncProps, executeVocabularyMatching: boolean = true) => {
    const vocabularyMatches = executeVocabularyMatching ? fetchVocabularyMatchingResults(data) : emptyMatchResult;
    return Rx.Observable.forkJoin(
        vocabularyMatches,
        fetchValueSourcePaths(data),
        (vocabDatasetsResponse, sourcePaths) => {
            const suggestions: ITransformedSuggestion[] = [];
            let suggestionIssues: SuggestionIssues | undefined = undefined;
            if (vocabDatasetsResponse.data) {
                const { matches, issues } = vocabDatasetsResponse.data;
                suggestionIssues = issues;
                if (matches) {
                    matches.map((match) => {
                        const { uri: sourceUri, description, label, candidates, graph } = match;
                        return suggestions.push({
                            uri: sourceUri,
                            candidates,
                            description,
                            label,
                            graph,
                        });
                    });
                }
            }

            if (data.matchFromDataset && sourcePaths.data) {
                sourcePaths.data.forEach((sourcePath) => {
                    const existingSuggestion = suggestions.find((suggestion) => suggestion.uri === sourcePath.path);
                    if (!existingSuggestion) {
                        suggestions.push({
                            uri: sourcePath.path,
                            candidates: [],
                            alreadyMapped: sourcePath.alreadyMapped,
                            pathType: sourcePath.pathType,
                            objectInfo: sourcePath.objectInfo,
                        });
                    } else {
                        existingSuggestion.pathType = sourcePath.pathType;
                        existingSuggestion.alreadyMapped = sourcePath.alreadyMapped;
                    }
                });
            }
            return {
                suggestions,
                suggestionIssues,
                warnings: _.filter([vocabDatasetsResponse.error, sourcePaths.error], (e) => !_.isUndefined(e)),
            };
        },
    );
};

export const childExampleAsync = (data) => {
    const { ruleType, rawRule, id, objectPath } = data;
    const getRule = (rawRule, type) => {
        switch (type) {
            case MAPPING_RULE_TYPE_DIRECT:
            case MAPPING_RULE_TYPE_COMPLEX:
                return prepareValueMappingPayload(rawRule);
            case MAPPING_RULE_TYPE_OBJECT:
                return prepareObjectMappingPayload(rawRule);
            case MAPPING_RULE_TYPE_URI:
            case MAPPING_RULE_TYPE_COMPLEX_URI:
                return rawRule;
            default:
                throw new Error(
                    'Rule send to rule.child.example type must be in ("value","object","uri","complexURI")',
                );
        }
    };

    const rule = getRule(rawRule, ruleType);

    if (rule && id) {
        return silkStore
            .request({
                topic: "transform.task.rule.child.peak",
                data: { ...getApiDetails(), id, rule, objectPath },
            })
            .map(mapPeakResult);
    }

    return Rx.Observable();
};

export const ruleExampleAsync = (data) => {
    const { id } = data;
    if (id) {
        return silkStore
            .request({
                topic: "transform.task.rule.peak",
                data: { ...getApiDetails(), id },
            })
            .map(mapPeakResult);
    }
    return Rx.Observable();
};

export const getHierarchyAsync = () => {
    return silkStore
        .request({
            topic: "transform.task.rules.get",
            data: {
                ...getApiDetails(),
            },
        })
        .map((returned) => {
            const rules = returned.body;

            if (!_.isString(rootId)) {
                rootId = rules.id;
            }

            return {
                hierarchy: rules,
            };
        });
};

export const getEditorHref = (ruleId) => {
    const { transformTask, project } = getApiDetails();
    const inlineView = window.location !== window.parent.location ? "true" : "false";
    return ruleId
        ? `${CONTEXT_PATH}/transform/${project}/${transformTask}/editor/${ruleId}?inlineView=${inlineView}`
        : null;
};

export const getRuleAsync = (id, isObjectMapping = false) => {
    return silkStore
        .request({
            topic: "transform.task.rules.get",
            data: { ...getApiDetails() },
        })
        .map((returned) => {
            const rules = returned.body;
            const searchId = id || rules.id;
            if (!_.isString(rootId)) {
                rootId = rules.id;
            }
            const rule = findRule(_.cloneDeep(rules), searchId, isObjectMapping, []);
            return { rule: rule || rules };
        });
};

interface AutoCompleteParams {
    entity?: string;
    input: string;
    ruleId?: string;
    taskContext?: TaskContext;
}

export const autocompleteAsync = (data: AutoCompleteParams) => {
    const { entity, input, ruleId = rootId, taskContext } = data;

    let channel = "transform.task.rule.completions.";
    switch (entity) {
        case "propertyType":
            channel += "valueTypes";
            break;
        case "targetProperty":
            channel += "targetProperties";
            break;
        case "targetEntityType":
            channel += "targetTypes";
            break;
        case "sourcePath":
            channel += "sourcePaths";
            break;
        default:
            console.warn("No auto-complete route defined for " + entity);
    }

    return silkStore
        .request({
            topic: channel,
            data: { ...getApiDetails(), term: input, ruleId, taskContext },
        })
        .map((returned) => {
            return { options: returned.body };
        });
};

export const createMappingAsync = (data, isObject = false) => {
    const payload = isObject ? prepareObjectMappingPayload(data) : prepareValueMappingPayload(data);
    return editMappingRule(payload, data.id, data.parentId || rootId);
};

export const updateObjectMappingAsync = (data) => {
    return editMappingRule(data, data.id, data.parentId || rootId);
};

export const createGeneratedMappingAsync = (data) => {
    return editMappingRule(data, false, data.parentId || rootId);
};

export const ruleRemoveAsync = (id) => {
    return silkStore
        .request({
            topic: "transform.task.rule.delete",
            data: {
                ...getApiDetails(),
                ruleId: id,
            },
        })
        .map(
            () => {
                return EventEmitter.emit(MESSAGES.RELOAD, true);
            },
            (err) => {
                // TODO: When mapping and workspace code bases are merged, add error handling
            },
        );
};

export const copyRuleAsync = (data) => {
    const { project, transformTask } = getApiDetails();
    return silkStore
        .request({
            topic: "transform.task.rule.copy",
            data: {
                project,
                transformTask,
                id: data.id || MAPPING_RULE_TYPE_ROOT,
                queryParameters: data.queryParameters,
                appendTo: data.id, // the rule the copied rule should be appended to
            },
        })
        .map((returned) => returned.body.id);
};

export const schemaExampleValuesAsync = (ruleId: string) => {
    const { project, transformTask } = getApiDetails();
    return silkStore
        .request({
            topic: "transform.task.rule.example",
            data: {
                CONTEXT_PATH,
                project,
                transformTask,
                ruleId,
            },
        })
        .map((returned) => returned.body);
};

export const prefixesAsync = () => {
    const { project } = getApiDetails();
    return silkStore
        .request({
            topic: "transform.task.prefixes",
            data: {
                CONTEXT_PATH,
                project,
            },
        })
        .map((returned) => returned.body);
};

const getValuePathSuggestion = (
    ruleId: string,
    inputString: string,
    cursorPosition: number,
    isObjectPath: boolean,
    taskContext?: TaskContext,
): HttpResponsePromise<CodeAutocompleteFieldPartialAutoCompleteResult> => {
    const { transformTask, project } = getDefinedApiDetails();
    return silkApi.getSuggestionsForAutoCompletion(
        project,
        transformTask,
        ruleId,
        inputString,
        cursorPosition,
        isObjectPath,
        taskContext,
    );
};

/** Fetches (partial) auto-complete suggestions for the value path
 *
 * @param ruleId         The transform rule ID
 * @param inputString    The current path input string.
 * @param cursorPosition The cursor position inside the input string.
 * @param isObjectPath   If the suggestions are for an object path, i.e. the value path of an object mapping.
 * @param taskContext    Optional task context that defines another input task as the configured one.
 */
export const fetchValuePathSuggestions = (
    ruleId: string | undefined,
    inputString: string,
    cursorPosition: number,
    isObjectPath: boolean,
    taskContext?: TaskContext,
): Promise<CodeAutocompleteFieldPartialAutoCompleteResult | undefined> => {
    return new Promise((resolve, reject) => {
        if (!ruleId) {
            resolve(undefined);
        } else {
            getValuePathSuggestion(ruleId, inputString, cursorPosition, isObjectPath, taskContext)
                .then((suggestions) => resolve(suggestions?.data))
                .catch((err) => reject(err));
        }
    });
};

// Fetches (partial) auto-complete suggestions for a path expression inside a URI pattern
export const fetchUriPatternAutoCompletions = (
    ruleId: string | undefined,
    inputString: string,
    cursorPosition: number,
    objectContextPath?: string,
): Promise<CodeAutocompleteFieldPartialAutoCompleteResult | undefined> => {
    return new Promise((resolve, reject) => {
        if (!ruleId) {
            resolve(undefined);
        } else {
            const { transformTask, project } = getDefinedApiDetails();
            silkApi
                .getUriTemplateSuggestionsForAutoCompletion(
                    project,
                    transformTask,
                    ruleId,
                    inputString,
                    cursorPosition,
                    objectContextPath,
                )
                .then((suggestions) => resolve(suggestions?.data))
                .catch((err) => reject(err));
        }
    });
};

const pathValidation = (inputString: string, projectId?: string) => {
    const { project } = projectId ? { project: projectId } : getDefinedApiDetails();
    return silkApi.validatePathExpression(project, inputString);
};

const uriPatternValidation = (inputString: string) => {
    const { project } = getDefinedApiDetails();
    return silkApi.validateUriPattern(project, inputString);
};

// Checks if the value path syntax is valid
export const checkValuePathValidity = (
    inputString,
    projectId?: string,
): Promise<CodeAutocompleteFieldValidationResult | undefined> => {
    return new Promise((resolve, reject) => {
        if (!inputString) {
            // Empty string is considered valid
            resolve({ valid: true });
        } else {
            pathValidation(inputString, projectId)
                .then((response) => {
                    const payload = response?.data;
                    resolve(payload);
                })
                .catch((err) => {
                    reject(err);
                });
        }
    });
};

// Checks if the value path syntax is valid
export const checkUriPatternValidity = (
    uriPattern: string,
): Promise<CodeAutocompleteFieldValidationResult | undefined> => {
    return new Promise((resolve, reject) => {
        uriPatternValidation(uriPattern)
            .then((response) => {
                const payload = response?.data;
                resolve(payload);
            })
            .catch((err) => {
                reject(err);
            });
    });
};

const exportFunctions = {
    getHierarchyAsync,
    getRuleAsync,
    createMappingAsync,
    getApiDetails,
    setApiDetails,
};

export default exportFunctions;
