import React from 'react';
import {
    Grid,
    GridColumn,
    GridRow,
    Section,
    SearchField,
    SectionHeader,
    TitleMainsection,
    Toolbar,
    ToolbarSection,
    Divider
} from "@gui-elements/index";

export default function SuggestionContainer({ruleId}) {
    return (
        <Section>
            <SectionHeader>
                <Grid>
                    <GridRow>
                        <GridColumn small verticalAlign="center">
                            <TitleMainsection>Mapping Suggestion for {ruleId}</TitleMainsection>
                        </GridColumn>
                    </GridRow>
                </Grid>
            </SectionHeader>
            <Divider addSpacing="medium"/>
            <Grid>
                <GridRow>
                    <GridColumn full>
                        <Toolbar>
                            <ToolbarSection canGrow>
                                <SearchField
                                    data-test-id={"search-bar-suggestion"}
                                    autoFocus={() => {
                                    }}
                                    onChange={() => {
                                    }}
                                    onKeyDown={() => {
                                    }}
                                    onClearanceHandler={() => {
                                    }}
                                    emptySearchInputMessage={"Search Suggestions"}
                                />
                            </ToolbarSection>
                        </Toolbar>
                    </GridColumn>
                </GridRow>
            </Grid>
        </Section>
    )
}
