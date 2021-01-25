import React, {useContext} from "react";
import {
    Checkbox,
    Highlighter,
    OverviewItem,
    OverviewItemDescription,
    OverviewItemLine,
    OverviewItemActions,
    OverflowText,
    TableCell,
    TableRow,
    Toolbar,
    ToolbarSection,
} from "@gui-elements/index";
import {SuggestionTypeValues} from "../../suggestion.typings";
import {SuggestionListContext} from "../../SuggestionContainer";
import TypesList from "../TypesList";
import {SourceCellData} from "./SourceCellData";
import TargetList from "../TargetList";
import TargetInfoBox from "./TargetInfoBox";
import {ExampleInfoBox} from "./ExampleInfoBox";

export default function STableRow({row, onRowSelect, selected, onModifyTarget}) {
    const context = useContext(SuggestionListContext);
    const {uri, candidates} = row;

    const handleModifyTarget = (uri: string, type?: SuggestionTypeValues) => {
        const modified = candidates.map(target => {
            const isSelected = uri === target.uri;

            return {
                ...target,
                _selected: isSelected,
                type: isSelected ? type || target.type : target.type
            }
        });

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
                    : <>
                        <OverviewItem>
                            <OverviewItemDescription>
                                {row.label && <OverviewItemLine><OverflowText><Highlighter label={row.label} searchValue={search}/></OverflowText></OverviewItemLine>}
                                {row.uri && <OverviewItemLine><OverflowText><Highlighter label={row.uri} searchValue={search}/></OverflowText></OverviewItemLine>}
                                {
                                    row.description &&
                                    <OverviewItemLine><OverflowText><Highlighter label={row.description} searchValue={search}/></OverflowText></OverviewItemLine>
                                }
                            </OverviewItemDescription>
                            <OverviewItemActions>
                                <TargetInfoBox selectedTarget={row} />
                            </OverviewItemActions>
                        </OverviewItem>
                    </>
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
                {
                    context.isFromDataset
                        ? <TargetInfoBox selectedTarget={selectedTarget}/>
                        : <ExampleInfoBox source={selectedTarget.uri}/>

                }
                </ToolbarSection>
            </Toolbar>
        </TableCell>
        <TableCell>
            <TypesList
                onChange={(type) => handleModifyTarget(selectedTarget.uri, type)}
                selected={selectedType}
            />
        </TableCell>
    </TableRow>
}
