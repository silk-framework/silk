import React, {useContext} from "react";
import {
    Checkbox,
    Highlighter,
    OverviewItem,
    OverviewItemDescription,
    OverviewItemLine,
    OverflowText,
    Spacing,
    TableCell,
    TableRow,
    Toolbar,
    ToolbarSection,
} from "@gui-elements/index";
import {IPageSuggestion, ITargetWithSelected, SuggestionTypeValues} from "../../suggestion.typings";
import {SuggestionListContext} from "../../SuggestionContainer";
import TypesList from "../TypesList";
import {SourceCellData} from "./SourceCellData";
import TargetList from "../TargetList";
import TargetInfoBox from "./TargetInfoBox";
import {SourcePathInfoBox} from "./SourcePathInfoBox";

interface IProps {
    // A single suggestion row
    row: IPageSuggestion
    // Triggered when a row is selected (via checkbox)
    onRowSelect: (pageSuggestion: IPageSuggestion) => void
    // If the row is selected
    selected: boolean
    // Triggered when the target selection changes with changed elements
    onModifyTarget: (targets: ITargetWithSelected[]) => void
}

/** A single suggestion row of the table. */
export default function STableRow({row, onRowSelect, selected, onModifyTarget}: IProps) {

    const context = useContext(SuggestionListContext);
    const {uri, candidates} = row;

    const handleModifyTarget = (selectedTarget: ITargetWithSelected, type?: SuggestionTypeValues) => {
        let targetAlreadyExists = false
        const modified = candidates.map(target => {
            const isSelected = selectedTarget.uri === target.uri;
            targetAlreadyExists = targetAlreadyExists || isSelected

            return {
                ...target,
                _selected: isSelected,
                type: isSelected ? type || target.type : target.type
            }
        });
        if(!targetAlreadyExists) {
            modified.unshift({
                ...selectedTarget,
                _selected: true
            })
        }

        onModifyTarget(modified);
    };

    const selectedTarget = candidates.find(t => t._selected);
    const selectedType = selectedTarget ? selectedTarget.type : 'value';

    const {search} = context;
    return <TableRow>
        <TableCell>
            <Checkbox onChange={() => onRowSelect(row)} checked={!!selected}/>
        </TableCell>
        <TableCell>
            {
                context.isFromDataset
                    ? <SourceCellData label={uri} search={search}/>
                    : <Toolbar noWrap={true}>
                        <ToolbarSection canShrink={true}>
                            <OverviewItem>
                                <OverviewItemDescription>
                                    {row.label && <OverviewItemLine><OverflowText><Highlighter label={row.label} searchValue={search}/></OverflowText></OverviewItemLine>}
                                    {row.uri && <OverviewItemLine small={true}><OverflowText ellipsis={"reverse"}><Highlighter label={row.uri} searchValue={search}/></OverflowText></OverviewItemLine>}
                                    {
                                        row.description &&
                                        <OverviewItemLine small={true}><OverflowText><Highlighter label={row.description} searchValue={search}/></OverflowText></OverviewItemLine>
                                    }
                                </OverviewItemDescription>
                            </OverviewItem>
                        </ToolbarSection>
                        <ToolbarSection>
                            <TargetInfoBox selectedTarget={row} />
                        </ToolbarSection>
                    </Toolbar>
            }
        </TableCell>
        <TableCell>
            <div/>
        </TableCell>
        <TableCell>
            <Toolbar noWrap={true}>
                <ToolbarSection canShrink={true}>
                    <TargetList targets={candidates} onChange={handleModifyTarget}/>
                </ToolbarSection>
                <ToolbarSection>
                    <Spacing vertical={true} size="tiny" />
                    {
                        context.isFromDataset
                            ? <TargetInfoBox selectedTarget={selectedTarget}/>
                            : <SourcePathInfoBox source={selectedTarget.uri}/>

                    }
                </ToolbarSection>
            </Toolbar>
        </TableCell>
        <TableCell>
            <TypesList
                onChange={(type) => handleModifyTarget(selectedTarget, type)}
                selected={selectedType}
            />
        </TableCell>
    </TableRow>
}
