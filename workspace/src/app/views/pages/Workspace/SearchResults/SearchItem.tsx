import React from "react";
import DataList from "../../../components/Datalist";
import { ISearchResultsServer } from "@ducks/workspace/typings";
import Popover from "@wrappers/blueprint/popover";
import { IconNames, Position } from "@wrappers/blueprint/constants";
import {
    Menu,
    MenuItem,
    MenuDivider,
    Icon,
} from "@wrappers/index";

interface IProps {
    item: ISearchResultsServer;
    searchValue?: string;

    onOpenDeleteModal(item: ISearchResultsServer);

    onOpenDuplicateModal(item: ISearchResultsServer);

    onRowClick?();
}

export default function SearchItem({item, searchValue, onOpenDeleteModal, onOpenDuplicateModal, onRowClick = () => {}}: IProps) {
    const {ListRow, Cell} = DataList;
    const getItemLinkIcons = (label: string) => {
        switch (label) {
            case 'Mapping editor':
                return 'application-mapping';
            case 'Transform evaluation':
                return 'item-evaluation';
            case 'Transform execution':
                return 'item-execution';
            default:
                return null;
        }
    };

    /** Escapes strings to match literally.
     *  taken from https://stackoverflow.com/questions/6318710/javascript-equivalent-of-perls-q-e-or-quotemeta
     */
    const escapeRegexWord = (str: string) => {
        return str.toLowerCase().replace(/[-[\]{}()*+?.,\\^$|#\s]/g, "\\$&");
    };

    /**
     * Returns a highlighted string according to the words of the search query.
     * @param label The string to highlight.
     */
    const getSearchHighlight = (label: string) => {
        if (!searchValue) {
            return label
        }
        const searchStringParts = searchValue.split(RegExp('\\s+'));
        const lowerCaseLabel = label.toLowerCase();
        const validString = searchStringParts.map(escapeRegexWord).join('|');
        const multiWordRegex = RegExp(validString, 'g');
        const result = [];

        let offset = 0;
        // loop through matches and add unmatched and matched parts to result array
        let matchArray = multiWordRegex.exec(lowerCaseLabel);
        while (matchArray !== null) {
            result.push(label.slice(offset, matchArray.index));
            result.push(`<mark>${matchArray[0]}</mark>`);
            offset = multiWordRegex.lastIndex;
            matchArray = multiWordRegex.exec(lowerCaseLabel);
        }
        // Add remaining unmatched string
        result.push(label.slice(offset));
        return result.join('');

    };

    const getContextMenuItems = (item: any) => {
        const {itemLinks} = item;
        return itemLinks.map(link =>
            <MenuItem key={link.path} text={link.label} href={link.path} icon={getItemLinkIcons(link.label)} target={'_blank'}/>
        );
    };

    return (
        <ListRow>
            <Cell>
                <Icon name='artefact-project' large />
            </Cell>
            <Cell onClick={onRowClick}>
                <p dangerouslySetInnerHTML={{
                    __html: getSearchHighlight(item.label || item.id)
                }}/>
                <p>{item.description}</p>
            </Cell>
            <Cell>
                <Icon
                    data-test-id={'open-duplicate-modal'}
                    name='item-clone'
                    onClick={onOpenDuplicateModal}
                />
                {
                    !!item.itemLinks.length &&
                    <a href={item.itemLinks[0].path}>
                        <Icon name='item-viewdetails' />
                    </a>
                }
                <Popover position={Position.BOTTOM_LEFT}>
                    <Icon name='item-moremenu' />
                    <Menu>
                        {getContextMenuItems(item)}
                        <MenuDivider />
                        <MenuItem key='delete' icon={'item-remove'} onClick={onOpenDeleteModal} text={'Delete'}/>
                    </Menu>
                </Popover>
            </Cell>
        </ListRow>
    )
}
