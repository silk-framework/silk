import React from "react";
import {
    Menu,
    MenuItem,
    Highlighter,
    OverviewItem,
    OverviewItemDescription,
    OverviewItemLine,
    OverflowText,
    Spinner,
    Spacing,
} from "@gui-elements/index";

interface IDropdownProps {
    options: Array<any>;
    onItemSelectionChange: (item) => void;
    onMouseOverItem: (value: string) => void;
    isOpen: boolean;
    query?: string;
    loading?: boolean;
}

const Item = ({ item, query }) => {
    return (
        <OverviewItem>
            <OverviewItemDescription>
                <OverviewItemLine>
                    <OverflowText ellipsis="reverse">
                        <Highlighter
                            label={item.value}
                            searchValue={query}
                        ></Highlighter>
                    </OverflowText>
                </OverviewItemLine>
                {item.description ? (
                    <OverviewItemLine small={true}>
                        <OverflowText ellipsis="reverse">
                            <Highlighter
                                label={item.description}
                                searchValue={query} />
                        </OverflowText>
                    </OverviewItemLine>
                ) : null}
            </OverviewItemDescription>
        </OverviewItem>
    );
};

const Dropdown: React.FC<IDropdownProps> = ({
    options,
    loading,
    onItemSelectionChange,
    isOpen = true,
    query,
    onMouseOverItem,
}) => {
    if (!isOpen) return null;
    if (loading)
        return (
            <OverviewItem hasSpacing>
                <OverviewItemLine>Fetching suggestions</OverviewItemLine>
                <Spacing size="tiny" vertical={true} />
                <Spinner position="inline" description="" />
            </OverviewItem>
        );
    return (
        <Menu>
            {options.map(
                (item: { value: string; description?: string }, index) => (
                    <MenuItem
                        key={index}
                        onClick={() => onItemSelectionChange(item.value)}
                        text={<Item item={item} query={query} />}
                        onMouseEnter={() => onMouseOverItem(item.value)}
                    />
                )
            )}
        </Menu>
    );
};

export default React.memo(Dropdown);
