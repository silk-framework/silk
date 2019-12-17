import { Icon, Popover, Position, Menu } from "@blueprintjs/core";
import React from "react";
import DataList from "../../../components/datalist/DataList";
import { ISearchResultsTask } from "../../../../state/ducks/dashboard/typings";

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
            <Menu.Item key={link.path} text={link.label} href={link.path} target={'_blank'}/>
        );
        menuItems.push(
            <Menu.Item key='delete' icon={'trash'} onClick={() => onOpenDeleteModal(item)} text={'Delete'}/>
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
