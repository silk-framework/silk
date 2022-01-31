import React from "react";
import { Tag as TagType } from "./Metadatatypings";
import { MultiSelect } from "@blueprintjs/select";
import { Button, Highlighter, MenuItem, OverflowText } from "gui-elements";

interface SelectedParamsType {
    newlySelected: TagType;
    selectedTags: TagType[];
    createdTags: Partial<TagType>[];
}

interface TagMultiSelectProps {
    items: TagType[];
    onSelection: (params: SelectedParamsType) => void;
}

const TagMultiSelect: React.FC<TagMultiSelectProps> = ({ items, onSelection }) => {
    const [createdTags, setCreatedTags] = React.useState<Array<Partial<TagType>>>([]);
    const [itemsCopy, setItemsCopy] = React.useState<Array<TagType>>([...items]);
    const [filteredTagList, setFilteredTagList] = React.useState<Array<TagType>>([]);
    const [selectedTags, setSelectedTags] = React.useState<Array<TagType>>([...items]);
    const [searchQuery, setSearchQuery] = React.useState<string | undefined>(undefined);

    React.useEffect(() => {
        onSelection({
            newlySelected: selectedTags.slice(-1)[0],
            createdTags,
            selectedTags,
        });
    }, [selectedTags.map((t) => t.uri).join("|"), createdTags.map((t) => t.uri).join("|")]);

    const tagsExists = (uri: string) => {
        return !!selectedTags.find((tag) => tag.uri === uri);
    };

    const handleClear = () => {
        setSelectedTags([]);
        setFilteredTagList(itemsCopy);
    };

    const clearButton =
        selectedTags.length > 0 ? (
            <Button icon="operation-clear" data-test-id="clear-all-vocabs" minimal={true} onClick={handleClear} />
        ) : undefined;

    // Renders the entries of the (search) options list
    const optionRenderer = (label: string) => {
        return <Highlighter label={label} searchValue={searchQuery} />;
    };

    const removeTagFromSelectionViaIndex = (label: string, index: number) => {
        setSelectedTags([...selectedTags.slice(0, index), ...selectedTags.slice(index + 1)]);
        setCreatedTags((tags) => tags.filter((t) => t.label !== label));
        // setItemsCopy((tags) => tags.filter((t) => t.label !== label));
    };

    const onItemRenderer = (tag: TagType, { handleClick, modifiers }) => {
        if (!modifiers.matchesPredicate) {
            return null;
        }
        return (
            <MenuItem
                active={modifiers.active}
                key={tag.uri}
                icon={tagsExists(tag.uri) ? "state-checked" : "state-unchecked"}
                onClick={handleClick}
                text={optionRenderer(tag.label)}
                shouldDismissPopover={false}
            />
        );
    };

    const onQueryChange = (query: string) => {
        setSearchQuery(query);
        setFilteredTagList(() => (query.length ? itemsCopy.filter((t) => t.label.includes(query)) : itemsCopy));
    };

    const removeSelection = (uri) => {
        setSelectedTags((tags) => tags.filter((t) => t.uri !== uri));
    };

    const onItemSelect = (tag: TagType) => {
        if (tagsExists(tag.uri)) {
            removeSelection(tag.uri);
        } else {
            setSelectedTags((tags) => [...tags, tag]);
        }
    };

    const newItemRenderer = (label: string, active: boolean, handleClick) => {
        const clickHandler = (e) => {
            const newTag = {
                uri: label,
                label,
            };
            //set new tags
            setCreatedTags((tags) => [...tags, newTag]);
            setSearchQuery("");
            itemsCopy.push(newTag);
            handleClick(e);
        };
        return (
            <MenuItem
                id={"new-tag"}
                icon="item-add-artefact"
                active={active}
                key={label}
                onClick={clickHandler}
                text={<OverflowText>{`Add new tag '${label}'`}</OverflowText>}
            />
        );
    };

    return (
        <MultiSelect
            className="di__dataset__metadata-tag"
            createNewItemPosition="first"
            itemsEqual={(a, b) => a.label === b.label}
            items={filteredTagList}
            fill
            onItemSelect={onItemSelect}
            selectedItems={selectedTags}
            noResults={<MenuItem disabled={true} text="No results." />}
            itemRenderer={onItemRenderer}
            initialContent={() => (items.length ? <MenuItem label="has x content" /> : undefined)}
            tagRenderer={(tag) => tag.label}
            openOnKeyDown={true}
            createNewItemRenderer={newItemRenderer}
            onQueryChange={onQueryChange}
            query={searchQuery}
            createNewItemFromQuery={(query) => ({
                uri: query,
                label: query,
            })}
            tagInputProps={{
                inputProps: {
                    id: "tag",
                    autoComplete: "off",
                },
                onRemove: removeTagFromSelectionViaIndex,
                rightElement: clearButton,
                tagProps: { minimal: true },
            }}
            popoverProps={{
                minimal: true,
                fill: true,
                position: "bottom-left",
            }}
        />
    );
};

export default TagMultiSelect;
