import React from "react";
import computeScrollIntoView from "compute-scroll-into-view";
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
    Tooltip,
} from "@gui-elements/index";
import { ISuggestionWithReplacementInfo } from "./AutoSuggestion";

export interface IDropdownProps {
    // The options of the drop down
    options: Array<ISuggestionWithReplacementInfo>;
    // Called when an item has been selected from the drop down
    onItemSelectionChange: (item: ISuggestionWithReplacementInfo) => any;
    // If the drop down is visible
    isOpen: boolean;
    // If the drop down should show a loading state
    loading?: boolean;
    left?: number;
    // The item from the drop down that is active
    currentlyFocusedIndex: number;
    // Callback indicating what item should currently being highlighted, i.e. is either active or is hovered over
    itemToHighlight: (item: ISuggestionWithReplacementInfo | undefined) => any;
}

const RawItem = ({ item }, ref) => {
    const rawitem = (
        <OverviewItem densityHigh={true}>
            <OverviewItemDescription>
                <OverviewItemLine>
                    <OverflowText ellipsis="reverse">
                        <Highlighter
                            label={item.value}
                            searchValue={item.query}
                        ></Highlighter>
                    </OverflowText>
                </OverviewItemLine>
                {item.description ? (
                    <OverviewItemLine small={true}>
                        <OverflowText>
                            <Highlighter
                                label={item.description}
                                searchValue={item.query}
                            />
                        </OverflowText>
                    </OverviewItemLine>
                ) : null}
            </OverviewItemDescription>
        </OverviewItem>
    );

    return (
        <div ref={ref}>
            {!!item.description && item.description.length > 50 ? (
                <Tooltip content={item.description}>{rawitem}</Tooltip>
            ) : (
                <>{rawitem}</>
            )}
        </div>
    );
};

const Item = React.forwardRef(RawItem);

export const Dropdown = ({
    isOpen,
    options,
    loading,
    onItemSelectionChange,
    left,
    currentlyFocusedIndex,
    itemToHighlight,
}: IDropdownProps) => {
    const [hoveredItem, setHoveredItem] = React.useState<
        ISuggestionWithReplacementInfo | undefined
    >(undefined);
    const dropdownRef = React.useRef<HTMLDivElement>(null);
    const refs = {};
    const generateRef = (index) => {
        if (!refs.hasOwnProperty(`${index}`)) {
            refs[`${index}`] = React.createRef();
        }
        return refs[`${index}`];
    };

    React.useEffect(() => {
        const listIndexNode = refs[currentlyFocusedIndex];
        if (dropdownRef?.current && listIndexNode?.current) {
            const actions = computeScrollIntoView(listIndexNode.current, {
                boundary: dropdownRef.current,
                block: "nearest",
                scrollMode: "if-needed",
            });
            actions.forEach(({ el, top, left }) => {
                el.scrollTop = top;
                el.scrollLeft = left;
            });
        }
    }, [currentlyFocusedIndex]);

    // Decide which item to highlight
    React.useEffect(() => {
        const item = options[currentlyFocusedIndex];
        itemToHighlight(!isOpen ? undefined : hoveredItem || item);
    }, [
        currentlyFocusedIndex,
        options.map((o) => o.value + o.from).join("|"),
        isOpen,
        hoveredItem?.value,
    ]);

    const Loader = (
        <OverviewItem hasSpacing>
            <OverviewItemLine>Fetching suggestions</OverviewItemLine>
            <Spacing size="tiny" vertical={true} />
            <Spinner position="inline" description="" />
        </OverviewItem>
    );

    const loadingOrHasSuggestions = loading || options.length;
    if (!loadingOrHasSuggestions || !isOpen) return null;
    return (
        <div
            className="ecc-auto-suggestion-box__dropdown"
            style={{ left }}
            ref={dropdownRef}
        >
            {loading ? (
                Loader
            ) : (
                <Menu>
                    {options.map((item, index) => (
                        <MenuItem
                            key={index}
                            internalProps={{
                                active: currentlyFocusedIndex === index,
                                onMouseDown: (e) => e.preventDefault(),
                                onClick: () => onItemSelectionChange(item),
                                text: (
                                    <Item
                                        ref={generateRef(index)}
                                        item={item}
                                    />
                                ),
                                onMouseEnter: () => setHoveredItem(item),
                                onMouseLeave: () => setHoveredItem(undefined),
                                onMouseOver: () => {
                                    if (item.value != hoveredItem?.value) {
                                        setHoveredItem(item);
                                    }
                                },
                            }}
                        />
                    ))}
                </Menu>
            )}
        </div>
    );
};
