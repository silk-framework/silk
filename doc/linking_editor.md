The Linkage Rule Editor allows users to edit linkage rules in graphical way. Linkage rules are created as an operator tree by dragging and dropping the rule elements.

The editor is divided in two parts:
The left pane contains the most frequent used \[\[Inputs|property paths\]\] for the given data sets and restrictions. It also contains a list of all available operators (\[\[transformation|transformations\]\], \[\[comparison|comparators\]\] and \[\[aggregation|aggregators\]\]) as draggable elements.
The right part (editor pane) allows for drawing the flow chart by combining the elements chosen.

\[\[image:silk\_editor.png\]\]

Editing
=======

-   Drag elements from the left pane to the editor pane.
-   Connect the elements by drawing connections from and to the element endpoints (dots to the left and right of the element box).
-   Build a flow chart by connecting the elements, ending in one single element (either a comparison or aggregation).

The editor will guide the user in building the flow chart by highlighting connectable elements when drawing a new connection line.

Property Paths
==============

Property paths for the two data sources to be interlinked are loaded on the left pane and added in the order of their frequency in the data source.
Users can also add custom paths by dragging the `(custom path)` element to the editor pane and \[\[Inputs|editing the path\]\].

Operators
=========

The following operator panes are shown below the property paths:

-   \[\[Transformations|Transformations\]\]
-   \[\[Comparisons|Comparators\]\]
-   \[\[Aggregations|Aggregators\]\]

Hovering over the operator elements will show you more information on them.

Threshold
=========

`Threshold` defines the maximum distance between two data items which is required to generate a link between them.

Link Limit
==========

`Link Limit` defines the number of links originating from a single data item. Please choose between 1 and n (unlimited).