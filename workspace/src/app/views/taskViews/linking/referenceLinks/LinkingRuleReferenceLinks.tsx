import {
    Button,
    Card,
    Checkbox,
    Divider,
    IconButton,
    OverviewItem,
    OverviewItemDescription,
    OverviewItemLine,
    Spacing,
    Toolbar,
    ToolbarSection,
    WhiteSpaceContainer,
} from "@eccenca/gui-elements";
import React from "react";
import { useTranslation } from "react-i18next";

export const LinkingRuleReferenceLinks = () => {
    const [showLinksList, setShowLinksList] = React.useState(false);
    const [showOnlyMismatches, setShowOnlyMismatches] = React.useState(false);
    // Show uncertain links instead of decision history.
    const [showUncertainLinks, setShowUncertainLinks] = React.useState(false);
    const [showConfirmed, setShowConfirmed] = React.useState(true);
    const [showDeclined, setShowDeclined] = React.useState(true);
    const { t } = useTranslation();

    const ReferenceLinksHeader = () => {
        return (
            <Toolbar onClick={() => setShowLinksList((prev) => !prev)}>
                <ToolbarSection>
                    <Spacing vertical={true} size={"tiny"} />
                    <OverviewItem>
                        <OverviewItemDescription>
                            <OverviewItemLine large={true}>
                                {t("ActiveLearning.referenceLinks.title") /** TODO: Make configurable from outside; */}
                            </OverviewItemLine>
                        </OverviewItemDescription>
                    </OverviewItem>
                </ToolbarSection>
                <ToolbarSection canGrow={true} />
                <ToolbarSection onClick={(e) => e.stopPropagation()} style={{ verticalAlign: "center" }}>
                    {!showUncertainLinks ? (
                        <>
                            <Button
                                data-test-id={"reference-links-show-confirmed-links"}
                                elevated={showConfirmed}
                                onClick={() => setShowConfirmed(!showConfirmed)}
                            >
                                Confirmed
                            </Button>
                            <Button
                                data-test-id={"reference-links-show-declined-links"}
                                elevated={showDeclined}
                                onClick={() => setShowDeclined(!showDeclined)}
                            >
                                Declined
                            </Button>
                            <Spacing vertical={true} />
                        </>
                    ) : null}
                    <Button
                        data-test-id={"reference-links-show-certain-links"}
                        elevated={!showUncertainLinks}
                        onClick={() => setShowUncertainLinks(!showUncertainLinks)}
                    >
                        Certain
                    </Button>
                    <Button
                        data-test-id={"reference-links-show-uncertain-links"}
                        elevated={showUncertainLinks}
                        onClick={() => setShowUncertainLinks(!showUncertainLinks)}
                    >
                        Uncertain
                    </Button>
                    <Spacing vertical={true} />
                    <Checkbox
                        data-test-id={"reference-links-show-mismatches"}
                        checked={showOnlyMismatches}
                        onChange={() => setShowOnlyMismatches((prev) => !prev)}
                        style={{ verticalAlign: "center" }}
                    >
                        {t("ReferenceLinks.mismatchCheckboxTitle", { nrMismatches: "TODO: add nr" })}
                    </Checkbox>
                    <Spacing vertical={true} />
                    <IconButton
                        data-test-id={"reference-links-toggler-btn"}
                        onClick={() => setShowLinksList((prev) => !prev)}
                        name={showLinksList ? "toggler-showless" : "toggler-showmore"}
                        text={showLinksList ? t("ReferenceLinks.hideList") : t("ReferenceLinks.showList")}
                    />
                </ToolbarSection>
            </Toolbar>
        );
    };

    return (
        <Card isOnlyLayout elevation={0} data-test-id={"best-learned-rule-visual"}>
            <ReferenceLinksHeader />
            {showLinksList && (
                <>
                    <Divider />
                    <WhiteSpaceContainer paddingTop="small" paddingRight="tiny" paddingLeft="tiny">
                        TODO: Display rule visually
                    </WhiteSpaceContainer>
                </>
            )}
        </Card>
    );
};
