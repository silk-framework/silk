import React from "react";
import ConnectionAvailable from "./../components/ConnectionAvailable";
import {
    ComparisonDataBody,
    ComparisonDataCell,
    ComparisonDataConnection,
    ComparisonDataContainer,
    ComparisonDataRow,
} from "./../components/ComparisionData";
import {
    Card,
    CardContent,
    CardHeader,
    CardOptions,
    CardTitle,
    CodeAutocompleteField,
    Divider,
    Icon,
    IconButton,
    Notification,
    OverflowText,
    Spacing,
    Spinner,
} from "@eccenca/gui-elements";
import { checkValuePathValidity } from "../../../../pages/MappingEditor/HierarchicalMapping/store";
import { partialAutoCompleteLinkingInputPaths } from "../../LinkingRuleEditor.requests";
import useErrorHandler from "../../../../../hooks/useErrorHandler";
import { IPartialAutoCompleteResult } from "@eccenca/gui-elements/src/components/AutoSuggestion/AutoSuggestion";
import { ComparisonPairWithId, TypedPath } from "../LinkingRuleActiveLearning.typings";
import { useTranslation } from "react-i18next";
import { fetchPathExampleValues } from "../LinkingRuleActiveLearning.requests";
import { ActiveLearningValueExamples } from "../shared/ActiveLearningValueExamples";

interface Props {
    projectId: string;
    linkingTaskId: string;
    /** Add a single comparison pair. */
    addComparisonPair: (pair: ComparisonPairWithId) => Promise<boolean>;
}

let randomId = 0;
const nextId = () => {
    randomId += 1;
    return randomId;
};

/** Allows to manually select a comparison property pair. */
export const ManualComparisonPairSelection = ({ projectId, linkingTaskId, addComparisonPair }: Props) => {
    const manualSourcePath = React.useRef<TypedPath | undefined>(undefined);
    const sourceExampleValues = React.useRef<string[]>([]);
    const manualTargetPath = React.useRef<TypedPath | undefined>(undefined);
    const targetExampleValues = React.useRef<string[]>([]);
    const [t] = useTranslation();
    const [hasValidPath, setHasValidPath] = React.useState(false);
    const [showInfo, setShowInfo] = React.useState(false);
    const [resetting, setResetting] = React.useState(false);

    const checkPathValidity = () => {
        if (manualSourcePath.current && manualTargetPath.current) {
            setHasValidPath(true);
        } else {
            setHasValidPath(false);
        }
    };

    React.useEffect(() => {
        if (resetting) {
            setResetting(false);
        }
    }, [resetting]);

    const changeManualSourcePath = (value: string, label: string | undefined, exampleValues: string[]) => {
        // FIXME: How to fetch label and other meta data?
        manualSourcePath.current = value
            ? { path: value, valueType: "StringValueType", label: label ?? value }
            : undefined;
        sourceExampleValues.current = [...exampleValues];
        checkPathValidity();
    };

    const changeManualTargetPath = (value: string, label: string | undefined, exampleValues: string[]) => {
        // FIXME: How to fetch label and other meta data?
        manualTargetPath.current = value
            ? { path: value, valueType: "StringValueType", label: label ?? value }
            : undefined;
        targetExampleValues.current = [...exampleValues];
        checkPathValidity();
    };

    const reset = () => {
        changeManualSourcePath("", undefined, []);
        changeManualTargetPath("", undefined, []);
        setResetting(true);
    };

    const addManuallyChosenPair = async () => {
        if (manualSourcePath.current && manualTargetPath.current) {
            const added = await addComparisonPair({
                pairId: `manually chosen: ${nextId()}`,
                source: manualSourcePath.current,
                target: manualTargetPath.current,
                sourceExamples: sourceExampleValues.current,
                targetExamples: targetExampleValues.current,
            });
            if (added) {
                reset();
            }
        }
    };

    return (
        <Card elevation={0} data-test-id={"manual-comparison-selection"}>
            <CardHeader>
                <CardTitle>{t("ActiveLearning.config.manualSelection.title")}</CardTitle>
                <CardOptions>
                    <IconButton
                        name={"item-question"}
                        text={t("ActiveLearning.config.buttons.showInfo")}
                        onClick={() => setShowInfo(!showInfo)}
                    />
                </CardOptions>
            </CardHeader>
            <Divider />
            <CardContent>
                {showInfo && (
                    <>
                        <Notification
                            neutral
                            icon={<Icon name="item-question" />}
                            message={t("ActiveLearning.config.manualSelection.info")}
                            actions={
                                <IconButton
                                    name="navigation-close"
                                    text={t("ActiveLearning.config.buttons.closeInfo")}
                                    onClick={() => setShowInfo(false)}
                                />
                            }
                        />
                        <Spacing />
                    </>
                )}
                <ComparisonDataContainer>
                    <ComparisonDataBody>
                        <ComparisonDataRow>
                            <PathAutoCompletion
                                projectId={projectId}
                                linkingTaskId={linkingTaskId}
                                resetting={resetting}
                                isTarget={false}
                                changeManualPath={changeManualSourcePath}
                            />
                            <ComparisonDataConnection>
                                <ConnectionAvailable
                                    actions={
                                        <IconButton
                                            name={"item-add-artefact"}
                                            disabled={!hasValidPath}
                                            title={
                                                hasValidPath
                                                    ? t("common.action.add")
                                                    : t("ActiveLearning.config.manualSelection.cannotAdd")
                                            }
                                            onClick={addManuallyChosenPair}
                                        />
                                    }
                                />
                            </ComparisonDataConnection>
                            <PathAutoCompletion
                                projectId={projectId}
                                linkingTaskId={linkingTaskId}
                                resetting={resetting}
                                isTarget={true}
                                changeManualPath={changeManualTargetPath}
                            />
                        </ComparisonDataRow>
                    </ComparisonDataBody>
                </ComparisonDataContainer>
            </CardContent>
        </Card>
    );
};

interface PathAutoCompletionProps {
    projectId: string;
    linkingTaskId: string;
    resetting: boolean;
    isTarget: boolean;
    /** Called when the path has been changed. */
    changeManualPath: (path: string, label: string | undefined, exampleValues: string[]) => any;
}

/** Path auto-completion. */
const PathAutoCompletion = ({
    projectId,
    linkingTaskId,
    resetting,
    isTarget,
    changeManualPath,
}: PathAutoCompletionProps) => {
    const { registerError } = useErrorHandler();
    /** Stores the path. Often updated. */
    const path = React.useRef<string | undefined>(undefined);
    const isValid = React.useRef<boolean>(false);
    const exampleValues = React.useRef<string[]>([]);
    const [t] = useTranslation();
    const [showExampleValues, setShowExampleValues] = React.useState<boolean>(false);
    const [exampleValuesLoading, setExampleValuesLoading] = React.useState(false);
    const exampleValuesRequestId = React.useRef<string>("");

    React.useEffect(() => {
        if (resetting) {
            path.current = undefined;
            isValid.current = false;
            exampleValues.current = [];
            setShowExampleValues(false);
        }
    }, [resetting]);

    const updateState = (fetchValues: boolean = true) => {
        if (!!path.current) {
            if (isValid.current) {
                if (fetchValues) {
                    const currentPath = path.current;
                    setTimeout(() => fetchExampleValues(currentPath), 1000);
                }
                changeManualPath(path.current, undefined, exampleValues.current ?? []);
            } else {
                changeManualPath("", undefined, []);
            }
        } else {
            exampleValues.current = [];
            setShowExampleValues(false);
        }
    };

    React.useEffect(() => {
        updateState(false);
    }, [exampleValues]);

    const onValidation = React.useCallback((valid: boolean) => {
        isValid.current = valid;
        updateState();
    }, []);

    const onChange = React.useCallback((value) => {
        path.current = value;
        updateState();
    }, []);

    const fetchExampleValues = async (forPath: string) => {
        if (path.current !== forPath) {
            // Path has changed in the meantime, do not fetch.
            return;
        }
        const requestId = `${path.current}__${isValid.current}`;
        if (path.current && requestId !== exampleValuesRequestId.current) {
            exampleValues.current = [];
            setShowExampleValues(false);
            setExampleValuesLoading(true);
            exampleValuesRequestId.current = requestId;
            try {
                const result = await fetchPathExampleValues(projectId, linkingTaskId, isTarget, path.current);
                // Make sure that only the most recent request is used
                if (requestId === exampleValuesRequestId.current) {
                    exampleValues.current = result.data.exampleValues;
                    setShowExampleValues(true);
                }
            } catch (ex) {
                if (ex.isFetchError && ex.httpStatus === 400) {
                    // ignore errors because of invalid paths
                    return;
                }
                registerError("PathAutoCompletion.fetchExampleValues", "Could not fetch example value for path.", ex);
            } finally {
                setExampleValuesLoading(false);
            }
        }
    };

    const onFocusChange = React.useCallback((hasFocus: boolean) => {
        if (!hasFocus) {
            updateState(false);
        }
    }, []);

    const fetchAutoCompletionResult = React.useCallback(
        (isTarget: boolean) =>
            async (inputString: string, cursorPosition: number): Promise<IPartialAutoCompleteResult | undefined> => {
                try {
                    const result = await partialAutoCompleteLinkingInputPaths(
                        projectId,
                        linkingTaskId,
                        isTarget ? "target" : "source",
                        inputString,
                        cursorPosition,
                        200
                    );
                    return result.data;
                } catch (err) {
                    registerError(
                        "ActiveLearning.fetchAutoCompletionResult",
                        t("ActiveLearning.config.errors.fetchAutoCompletionResult"),
                        err
                    );
                }
            },
        []
    );

    return (
        <ComparisonDataCell className="diapp-linking-learningdata__pathselection">
            {resetting ? null : (
                <CodeAutocompleteField
                    label={t("ActiveLearning.config.manualSelection." + (isTarget ? "targetPath" : "sourcePath"))}
                    initialValue={""}
                    onChange={onChange}
                    fetchSuggestions={fetchAutoCompletionResult(isTarget)}
                    placeholder={t("ActiveLearning.config.manualSelection.insertPath")}
                    checkInput={(value) => checkValuePathValidity(value, projectId)}
                    onFocusChange={onFocusChange}
                    validationErrorText={t("ActiveLearning.config.errors.invalidPath")}
                    onInputChecked={onValidation}
                    autoCompletionRequestDelay={500}
                    validationRequestDelay={250}
                />
            )}
            {exampleValuesLoading && (
                <div>
                    <Spinner position={"inline"} size={"tiny"} delay={1000} />
                </div>
            )}
            {showExampleValues && exampleValues.current.length > 0 ? (
                <OverflowText>
                    <ActiveLearningValueExamples exampleValues={exampleValues.current} />
                </OverflowText>
            ) : null}
        </ComparisonDataCell>
    );
};
