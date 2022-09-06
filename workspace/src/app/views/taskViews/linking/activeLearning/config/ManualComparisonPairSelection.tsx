import React from "react";
import ConnectionAvailable from "./../components/ConnectionAvailable";
import {
    ComparisionDataContainer,
    ComparisionDataBody,
    ComparisionDataRow,
    ComparisionDataCell,
    ComparisionDataConnection,
} from "./../components/ComparisionData";
import {
    AutoSuggestion,
    Card,
    CardContent,
    CardHeader,
    CardTitle,
    Divider,
    IconButton,
    Notification,
    Spacing,
    Tag,
} from "@eccenca/gui-elements";
import { checkValuePathValidity } from "../../../../pages/MappingEditor/HierarchicalMapping/store";
import { partialAutoCompleteLinkingInputPaths } from "../../LinkingRuleEditor.requests";
import useErrorHandler from "../../../../../hooks/useErrorHandler";
import { IPartialAutoCompleteResult } from "@eccenca/gui-elements/src/components/AutoSuggestion/AutoSuggestion";
import { ComparisonPairWithId, TypedPath } from "../LinkingRuleActiveLearning.typings";
import { useTranslation } from "react-i18next";
import { fetchPathExampleValues } from "../LinkingRuleActiveLearning.requests";

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
    const manualTargetPath = React.useRef<TypedPath | undefined>(undefined);
    const [hasValidPath, setHasValidPath] = React.useState(false);

    const checkPathValidity = () => {
        if (manualSourcePath.current && manualTargetPath.current) {
            setHasValidPath(true);
        } else {
            setHasValidPath(false);
        }
    };

    const changeManualSourcePath = (value: string, label?: string) => {
        // TODO: How to fetch label, example values and other meta data?
        manualSourcePath.current = value
            ? { path: value, valueType: "StringValueType", label: label ?? value }
            : undefined;
        checkPathValidity();
    };

    const changeManualTargetPath = (value: string, label?: string) => {
        // TODO: How to fetch example values and other meta data?
        manualTargetPath.current = value
            ? { path: value, valueType: "StringValueType", label: label ?? value }
            : undefined;
        checkPathValidity();
    };

    const reset = () => {
        changeManualSourcePath("");
        changeManualTargetPath("");
    };

    const addManuallyChosenPair = async () => {
        if (manualSourcePath.current && manualTargetPath.current) {
            const added = await addComparisonPair({
                pairId: `manually chosen: ${nextId()}`,
                source: manualSourcePath.current,
                target: manualTargetPath.current,
                // TODO: where to get examples from?
                sourceExamples: [],
                targetExamples: [],
            });
            if (added) {
                reset();
            }
        }
    };

    const PathSelectionSubHeader = () => {
        return <Notification message={"Specify property paths to be compared."} iconName={null} />;
    };

    return (
        <Card elevation={0}>
            <CardHeader>
                <CardTitle>Add property paths pair</CardTitle>
            </CardHeader>
            <Divider />
            <CardContent>
                <PathSelectionSubHeader />
                <Spacing />
                <ComparisionDataContainer>
                    <ComparisionDataBody>
                        <ComparisionDataRow>
                            <PathAutoCompletion
                                projectId={projectId}
                                linkingTaskId={linkingTaskId}
                                isTarget={false}
                                changeManualPath={changeManualSourcePath}
                            />
                            <ComparisionDataConnection>
                                <ConnectionAvailable
                                    actions={
                                        <IconButton
                                            name={"item-add-artefact"}
                                            disabled={!hasValidPath}
                                            title={hasValidPath ? "Add" : "At least one paths is not valid"}
                                            onClick={addManuallyChosenPair}
                                        />
                                    }
                                />
                            </ComparisionDataConnection>
                            <PathAutoCompletion
                                projectId={projectId}
                                linkingTaskId={linkingTaskId}
                                isTarget={true}
                                changeManualPath={changeManualTargetPath}
                            />
                        </ComparisionDataRow>
                    </ComparisionDataBody>
                </ComparisionDataContainer>
            </CardContent>
        </Card>
    );
};

interface PathAutoCompletionProps {
    projectId: string;
    linkingTaskId: string;
    isTarget: boolean;
    /** Called when the path has been changed. Only called when input looses focus. */
    changeManualPath: (path: string) => any;
}

const PathAutoCompletion = ({ projectId, linkingTaskId, isTarget, changeManualPath }: PathAutoCompletionProps) => {
    const { registerError } = useErrorHandler();
    /** Stores the path. Often updated. */
    const path = React.useRef<string | undefined>(undefined);
    const isValid = React.useRef<boolean>(false);
    const [t] = useTranslation();
    const [exampleValues, setExampleValues] = React.useState<string[] | undefined>(undefined);
    const exampleValuesRequestId = React.useRef<string>("");

    const updateState = () => {
        if (path.current != null) {
            if (isValid.current) {
                fetchExampleValues();
                changeManualPath(path.current);
            } else {
                changeManualPath("");
            }
        } else {
            setExampleValues(undefined);
        }
    };

    const onValidation = React.useCallback((valid: boolean) => {
        isValid.current = valid;
        updateState();
    }, []);

    const onChange = React.useCallback((value) => {
        path.current = value;
        updateState();
    }, []);

    const fetchExampleValues = async () => {
        const requestId = `${path.current}__${isValid.current}`;
        if (path.current && requestId !== exampleValuesRequestId.current) {
            setExampleValues(undefined);
            exampleValuesRequestId.current = requestId;
            try {
                const result = await fetchPathExampleValues(projectId, linkingTaskId, isTarget, path.current);
                // Make sure that only the most recent request is used
                if (requestId === exampleValuesRequestId.current) {
                    setExampleValues(result.data.exampleValues);
                }
            } catch (ex) {
                registerError("PathAutoCompletion.fetchExampleValues", "Could not fetch example value for path.", ex);
            }
        }
    };

    const onFocusChange = React.useCallback((hasFocus: boolean) => {
        if (!hasFocus) {
            updateState();
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

    const exampleTitle = (exampleValues ?? []).join(" | ");

    return (
        <ComparisionDataCell>
            <AutoSuggestion
                label={t("ActiveLearning.config.manualSelection." + (isTarget ? "targetPath" : "sourcePath"))}
                initialValue={""}
                onChange={onChange}
                fetchSuggestions={fetchAutoCompletionResult(isTarget)}
                placeholder={"Enter an input path"}
                checkInput={(value) => checkValuePathValidity(value, projectId)}
                onFocusChange={onFocusChange}
                validationErrorText={t("ActiveLearning.config.errors.invalidPath")}
                onInputChecked={onValidation}
                autoCompletionRequestDelay={500}
            />
            {exampleValues && exampleValues.length > 0 ? (
                <div>
                    {exampleValues.map((value) => (
                        <Tag
                            key={value}
                            small={true}
                            minimal={true}
                            round={true}
                            style={{ marginRight: "0.25rem" }}
                            htmlTitle={exampleTitle}
                        >
                            {value}
                        </Tag>
                    ))}
                </div>
            ) : null}
        </ComparisionDataCell>
    );
};
