import React from "react";
import DataList from "../../../components/datalist/DataList";
import { ISearchResultsTask } from "../../../../state/ducks/dashboard/typings";
import MenuItem from "@wrappers/menu-item";
import Menu from "@wrappers/menu";
import Popover from "@wrappers/popover";
import { Position } from "@wrappers/constants";
import Icon from "@wrappers/icon";

interface IProps {
    item: ISearchResultsTask;
    searchValue: string;
    onOpenDeleteModal(item: ISearchResultsTask);
}

export default function ProjectRow({ item, searchValue, onOpenDeleteModal }: IProps) {
    const { Row, Cell } = DataList;

    const getSearchHighlight = (label: string) => {
        if (searchValue) {
            const regExp = RegExp(searchValue, 'g');
            return label.toString().replace(regExp, `<mark>${searchValue}</mark>`)
        }
        return label;
    };

    const getRowMenu = (item: any) => {
        const {itemLinks} = item;
        const menuItems = itemLinks.map(link =>
            <MenuItem key={link.path} text={link.label} href={link.path} target={'_blank'}/>
        );
        menuItems.push(
            <MenuItem key='delete' icon={'trash'} onClick={() => onOpenDeleteModal(item)} text={'Delete'}/>
        );

        return (
            <Menu>{menuItems}</Menu>
        )
    };

    return (
        <Row>
            <Cell>
                <p dangerouslySetInnerHTML={{
                    __html: getSearchHighlight(item.label || item.id)
                }}
                />
                <p>{item.description}</p>
            </Cell>
            <Cell>
                <Popover content={getRowMenu(item)} position={Position.BOTTOM_LEFT}>
                    <Icon icon="more"/>
                </Popover>
            </Cell>
        </Row>
    )
}
