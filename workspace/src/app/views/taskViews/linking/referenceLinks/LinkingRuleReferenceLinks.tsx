import {
    Button,
    Card,
    Checkbox,
    Divider,
    Icon,
    IconButton,
    OverviewItem,
    OverviewItemDescription,
    OverviewItemLine,
    Spacing,
    Spinner,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
    Toolbar,
    ToolbarSection,
    WhiteSpaceContainer,
} from "@eccenca/gui-elements";
import { usePagination } from "@eccenca/gui-elements/src/components/Pagination/Pagination";
import React from "react";
import { useTranslation } from "react-i18next";
import { IEntityLink, ReferenceLinks } from "../linking.types";
import { EntityLink, LabelProperties } from "./LinkingRuleReferenceLinks.typing";
import referenceLinksUtils from "./LinkingRuleReferenceLinks.utils";
import { Property } from "csstype";
import { ActiveLearningDecisions } from "../activeLearning/LinkingRuleActiveLearning.typings";

interface LinkingRuleReferenceLinksProps {
    /** If the reference links are being updated/loaded from the backend. */
    loading?: boolean;
    /** Reference links to display. */
    referenceLinks?: ReferenceLinks;
    /** The paths whose values should be displayed as entity labels. If missing the first value is shown.of each entity. */
    labelPaths?: LabelProperties;
    /** Remove a link from the list. */
    removeLink: (link: EntityLink) => any;
}

type AnnotatedReferenceLink = IEntityLink & {
    misMatch: boolean;
    type: ActiveLearningDecisions;
};

/** Displays reference links of a linking rule. */
export const LinkingRuleReferenceLinks = ({
    loading,
    referenceLinks,
    labelPaths,
    removeLink,
}: LinkingRuleReferenceLinksProps) => {
    const [showLinksList, setShowLinksList] = React.useState(false);
    const [showOnlyMismatches, setShowOnlyMismatches] = React.useState(false);
    // Show uncertain links instead of decision history.
    const [showUncertainLinks, setShowUncertainLinks] = React.useState(false);
    const [showConfirmed, setShowConfirmed] = React.useState(true);
    const [showDeclined, setShowDeclined] = React.useState(true);
    const [pagination, paginationElement, onTotalChange] = usePagination({
        initialPageSize: 10,
        pageSizes: [5, 10, 20, 50],
        presentation: { hideInfoText: true },
    });
    const [referenceLinksFiltered, setReferenceLinksFiltered] = React.useState<AnnotatedReferenceLink[] | undefined>(
        undefined
    );
    const { t } = useTranslation();
    const misMatches = (referenceLinksFiltered ?? []).filter((link) => link.misMatch).length;

    // Annotate and filter reference links
    React.useEffect(() => {
        const positiveAnnotatedLinks: AnnotatedReferenceLink[] = (referenceLinks?.positive ?? []).map((l) => ({
            ...l,
            type: "positive",
            misMatch: l.confidence ? l.confidence < 0 : false,
        }));
        const negativeAnnotatedLinks: AnnotatedReferenceLink[] = (referenceLinks?.negative ?? []).map((l) => ({
            ...l,
            type: "negative",
            misMatch: l.confidence ? l.confidence >= 0 : false,
        }));
        const referenceLinksAnnotated: AnnotatedReferenceLink[] = [
            ...positiveAnnotatedLinks,
            ...negativeAnnotatedLinks,
        ];
        setReferenceLinksFiltered(
            referenceLinksAnnotated.filter((link) => {
                const typeFiltered =
                    (showDeclined && link.type === "negative") || (showConfirmed && link.type === "positive");
                const misMatchFiltered = !showOnlyMismatches || link.misMatch;
                return typeFiltered && misMatchFiltered;
            })
        );
    }, [referenceLinks, showOnlyMismatches, showConfirmed, showDeclined]);

    if (referenceLinksFiltered && pagination.total !== referenceLinksFiltered.length) {
        onTotalChange(referenceLinksFiltered.length);
    }

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
                                Confirmed ({referenceLinks?.positive.length ?? "-"})
                            </Button>
                            <Button
                                data-test-id={"reference-links-show-declined-links"}
                                elevated={showDeclined}
                                onClick={() => setShowDeclined(!showDeclined)}
                            >
                                Declined ({referenceLinks?.negative.length ?? "-"})
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
                        {t("ReferenceLinks.mismatchCheckboxTitle", { nrMismatches: misMatches })}
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

    const ReferenceLinksTable = () => {
        return (
            <>
                <Table>
                    <TableHead>
                        <TableRow>
                            <TableHeader key={"marker-column"} style={{ width: "1px" }}>
                                &nbsp;
                            </TableHeader>
                            <TableHeader>Dataset 1</TableHeader>
                            <TableHeader>Dataset 2</TableHeader>
                            <TableHeader key={"actions-column"} style={{ width: "1px" }}>
                                &nbsp;
                            </TableHeader>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {referenceLinksFiltered
                            ? referenceLinksFiltered
                                  .slice(
                                      (pagination.current - 1) * pagination.limit,
                                      pagination.current * pagination.limit
                                  )
                                  .map((link, rowIdx) => {
                                      const entityLink = referenceLinksUtils.toReferenceEntityLink(link);
                                      const label = entityLink?.label ?? "unlabeled";
                                      let color: Property.BackgroundColor | undefined = undefined;
                                      if (label === "positive") {
                                          color = "green";
                                      }
                                      if (label === "negative") {
                                          color = "red";
                                      }
                                      if (entityLink) {
                                          const sourceLabel = referenceLinksUtils.entityValuesConcatenated(
                                              entityLink.source,
                                              labelPaths?.sourceProperties
                                          );
                                          const targetLabel = referenceLinksUtils.entityValuesConcatenated(
                                              entityLink.target,
                                              labelPaths?.targetProperties
                                          );
                                          return (
                                              <TableRow key={rowIdx} style={{ backgroundColor: color }}>
                                                  <TableCell>
                                                      {/** TODO: Set icon */}
                                                      <Icon name={"state-checked"} color={"green"} />
                                                  </TableCell>
                                                  <TableCell>{sourceLabel}</TableCell>
                                                  <TableCell>{targetLabel}</TableCell>
                                                  <TableCell>
                                                      <Toolbar>
                                                          <ToolbarSection>
                                                              <IconButton
                                                                  name={"item-remove"}
                                                                  disruptive={true}
                                                                  onClick={() => removeLink(entityLink)}
                                                              />
                                                              <IconButton
                                                                  name={"item-viewdetails"}
                                                                  onClick={() => {
                                                                      /** TODO: Load link into feedback component via callback */
                                                                  }}
                                                              />
                                                          </ToolbarSection>
                                                      </Toolbar>
                                                  </TableCell>
                                              </TableRow>
                                          );
                                      } else {
                                          return null;
                                      }
                                  })
                            : null}
                    </TableBody>
                </Table>
                {paginationElement}
            </>
        );
    };

    return (
        <Card isOnlyLayout elevation={0} data-test-id={"best-learned-rule-visual"}>
            <ReferenceLinksHeader />
            {showLinksList && (
                <>
                    <Divider />
                    <WhiteSpaceContainer paddingTop="small" paddingRight="tiny" paddingLeft="tiny">
                        {loading ? <Spinner /> : <ReferenceLinksTable />}
                    </WhiteSpaceContainer>
                </>
            )}
        </Card>
    );
};
