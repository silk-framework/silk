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
    onRowClick();
}

export default function ProjectRow({ item, searchValue, onOpenDeleteModal, onRowClick }: IProps) {
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

    const getSearchHighlight = (label: string) => {
        if (searchValue) {
            const searchStringParts = searchValue.split(' ');
            return searchStringParts.map(word => {
                const regExp = RegExp(word, 'gi');
                return label.replace(regExp, `<mark>${word}</mark>`)
            }).join('')
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
                <Icon icon={IconNames.DUPLICATE} onClick={() => {}} style={{'paddingRight': '10px'}}/>
                <Popover content={getRowMenu(item)} position={Position.BOTTOM_LEFT}>
                    <Icon icon={IconNames.MORE}/>
                </Popover>
            </Cell>
        </Row>
    )
}
