import React from "react";
import DataList from "../../../components/datalist/DataList";
import { ISearchResultsTask } from "@ducks/dashboard/typings";
import MenuItem from "@wrappers/menu-item";
import Menu from "@wrappers/menu";
import Popover from "@wrappers/popover";
import { IconNames, Position } from "@wrappers/constants";
import Icon from "@wrappers/icon";

interface IProps {
    item: ISearchResultsTask;
    searchValue?: string;
    onOpenDeleteModal(item: ISearchResultsTask);
    onOpenDuplicateModal(item: ISearchResultsTask);
    onRowClick();
}

export default function ProjectRow({ item, searchValue, onOpenDeleteModal, onOpenDuplicateModal, onRowClick }: IProps) {
    const { Row, Cell } = DataList;

    const getItemLinkIcons = (label: string) => {
        switch (label) {
            case 'Mapping editor':
                return IconNames.GRAPH;
            case 'Transform evaluation':
                return IconNames.HEAT_GRID;
            case 'Transform execution':
                return IconNames.PLAY;
            default:
                return null;
        }
    };

    /** Escapes strings to match literally.
     *  taken from https://stackoverflow.com/questions/6318710/javascript-equivalent-of-perls-q-e-or-quotemeta
     */
    const escapeRegexWord = (str: string) => {
        return str.replace(/[-[\]{}()*+?.,\\^$|#\s]/g, "\\$&");
    };

    /**
     * Returns a highlighted string according to the words of the search query.
     *
     * @param label The string to highlight.
     */
    const getSearchHighlight = (label: string) => {
        if (searchValue) {
            const searchStringParts = searchValue.split(RegExp('\\s+'));
            const lowerCaseLabel = label.toLowerCase();
            const multiWordRegex = RegExp(searchStringParts.map(word => `${escapeRegexWord(word.toLowerCase())}`).join('|'), 'g');
            let offset = 0;
            const result: Array<string> = [];
            // loop through matches and add unmatched and matched parts to result array
            let matchArray = multiWordRegex.exec(lowerCaseLabel);
            while(matchArray !== null) {
                result.push(label.slice(offset, matchArray.index));
                result.push(`<mark>${matchArray[0]}</mark>`);
                matchArray.index;
                offset = multiWordRegex.lastIndex;
                matchArray = multiWordRegex.exec(lowerCaseLabel);
            }
            // Add remaining unmatched string
            if(offset < label.length) {
                result.push(label.slice(offset));
            }
            return result.join('');
        }
        return label;
    };

    const getRowMenu = (item: any) => {
        const {itemLinks} = item;
        const menuItems = itemLinks.map(link =>
            <MenuItem key={link.path} text={link.label} href={link.path} icon={getItemLinkIcons(link.label)}/>
        );

        menuItems.push(
            <MenuItem key='delete' icon={IconNames.TRASH} onClick={onOpenDeleteModal} text={'Delete'}/>
        );
        return (
            <Menu>{menuItems}</Menu>
        )
    };

    return (
        <Row>
            <Cell onClick={onRowClick}>
                <p dangerouslySetInnerHTML={{
                    __html: getSearchHighlight(item.label || item.id)
                }}/>
                <p>{item.description}</p>
            </Cell>
            <Cell>
                <Icon icon={IconNames.DUPLICATE} onClick={onOpenDuplicateModal} style={{'paddingRight': '10px'}}/>
                <Popover content={getRowMenu(item)} position={Position.BOTTOM_LEFT}>
                    <Icon icon={IconNames.MORE}/>
                </Popover>
            </Cell>
        </Row>
    )
}
