import React from "react";
import { RuleEditorContext } from "../../contexts/RuleEditorContext";
import { Grid, GridColumn, GridRow, Spacing } from "gui-elements";
import Loading from "../../../Loading";
import { RuleOperatorList } from "./RuleOperatorList";
import { IRuleOperator } from "../../RuleEditor.typings";
import { extractSearchWords, matchesAllWords } from "gui-elements/src/components/Typography/Highlighter";
import { SidebarSearchField } from "./SidebarSearchField";
import { partitionArray, sortLexically } from "../../../../../utils/basicUtils";

/** Contains the list of operators that can be dragged and dropped onto the editor canvas and supports filtering. */
export const RuleEditorOperatorSidebar = () => {
    const editorContext = React.useContext(RuleEditorContext);
    const [filteredOperators, setFilteredOperators] = React.useState<IRuleOperator[]>([]);
    // The query that was input in the search field. This won't get immediately active.
    const [textQuery, setTextQuery] = React.useState<string>("");
    const searchWords = extractSearchWords(textQuery);

    // Filter operator list when active query or filters change
    React.useEffect(() => {
        if (editorContext.operatorList) {
            if (searchWords.length > 0) {
                setFilteredOperators(filterAndSortOperators(editorContext.operatorList, searchWords));
            } else {
                setFilteredOperators(editorContext.operatorList);
            }
        }
    }, [textQuery, editorContext.operatorList]);

    return editorContext.operatorListLoading ? (
        <Loading />
    ) : (
        <Grid data-test-id={"rule-editor-sidebar"} verticalStretchable={true} useAbsoluteSpace={true}>
            <GridRow>
                <GridColumn full style={{ paddingTop: "3px" }}>
                    <SidebarSearchField onQueryChange={setTextQuery} />
                    <Spacing size={"small"} />
                </GridColumn>
            </GridRow>
            <GridRow verticalStretched={true}>
                <GridColumn full style={{ paddingTop: "3px" }}>
                    <RuleOperatorList ruleOperatorList={filteredOperators} textQuery={textQuery} />
                </GridColumn>
            </GridRow>
        </Grid>
    );
};

// Filter the operators by search query and sort them
const filterAndSortOperators = (operators: IRuleOperator[], searchWords: string[]): IRuleOperator[] => {
    const textToSearchIn = (ruleOperator: IRuleOperator): string => {
        return `${ruleOperator.label} ${ruleOperator.description ?? ""} ${(ruleOperator.categories ?? []).join(
            " "
        )}`.toLowerCase();
    };
    const filtered = operators.filter((op) => {
        return matchesAllWords(textToSearchIn(op), searchWords);
    });
    const { matches, nonMatches } = partitionArray(
        filtered,
        (op) => !!searchWords.find((w) => op.label.toLowerCase().includes(w))
    );
    // Sort label and other matches independently
    const byLabel = (op: IRuleOperator) => op.label;
    const sorted = [...sortLexically(matches, byLabel), ...sortLexically(nonMatches, byLabel)];
    return sorted;
};
