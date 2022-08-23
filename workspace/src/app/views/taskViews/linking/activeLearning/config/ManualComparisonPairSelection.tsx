import { columnStyles, DashedLine } from "../LinkingRuleActiveLearning.shared";
import React from "react";
import { AutoSuggestion, GridColumn, GridRow, Icon, IconButton, Toolbar, ToolbarSection } from "@eccenca/gui-elements";
import { checkValuePathValidity } from "../../../../pages/MappingEditor/HierarchicalMapping/store";
import { partialAutoCompleteLinkingInputPaths } from "../../LinkingRuleEditor.requests";
import useErrorHandler from "../../../../../hooks/useErrorHandler";
import { IPartialAutoCompleteResult } from "@eccenca/gui-elements/src/components/AutoSuggestion/AutoSuggestion";
import { ComparisonPairWithId, TypedPath } from "../LinkingRuleActiveLearning.typings";
import { useTranslation } from "react-i18next";

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

    return (
        <GridRow style={{ maxWidth: "100%", minWidth: "100%", paddingLeft: "10px" }}>
            <PathAutoCompletion
                projectId={projectId}
                linkingTaskId={linkingTaskId}
                isTarget={false}
                changeManualPath={changeManualSourcePath}
            />
            <GridColumn style={columnStyles.centerColumnStyle}>
                <Toolbar style={{ height: "100%" }}>
                    <ToolbarSection canGrow={true}>
                        <DashedLine />
                    </ToolbarSection>
                    <ToolbarSection>
                        <IconButton
                            name={"item-add-artefact"}
                            disabled={!hasValidPath}
                            title={hasValidPath ? "Add" : "At least one paths is not valid"}
                            onClick={addManuallyChosenPair}
                        />
                    </ToolbarSection>
                    <ToolbarSection canGrow={true}>
                        <DashedLine />
                    </ToolbarSection>
                </Toolbar>
            </GridColumn>
            <PathAutoCompletion
                projectId={projectId}
                linkingTaskId={linkingTaskId}
                isTarget={true}
                changeManualPath={changeManualTargetPath}
            />
        </GridRow>
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

    const updateState = () => {
        if (path.current != null && isValid.current) {
            changeManualPath(path.current);
        }
    };

    const fetchAutoCompletionResult =
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
        };

    return (
        <GridColumn style={{ ...columnStyles.mainColumnStyle, textAlign: "left" }}>
            <AutoSuggestion
                label={t("ActiveLearning.config.manualSelection." + (isTarget ? "targetPath" : "sourcePath"))}
                leftElement={
                    <Icon name={"operation-search"} tooltipText={"Allows to construct complex input paths."} />
                }
                initialValue={path.current ?? ""}
                onChange={(value) => {
                    path.current = value;
                }}
                fetchSuggestions={fetchAutoCompletionResult(isTarget)}
                placeholder={"Enter an input path"}
                checkInput={(value) => checkValuePathValidity(value, projectId)}
                onFocusChange={(hasFocus) => {
                    if (!hasFocus) {
                        updateState();
                    }
                }}
                validationErrorText={t("ActiveLearning.config.errors.invalidPath")}
                onInputChecked={(valid: boolean) => (isValid.current = valid)}
            />
        </GridColumn>
    );
};
