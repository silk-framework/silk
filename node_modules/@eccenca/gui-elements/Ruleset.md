## CSS Theming

This part describes how we use and produce the style sheets for DW3.

### Pre-Compiler

* we use **pure** Sass with SCSS syntax
* pre-compiling is done by grunt tasks
* Sass: http://sass-lang.com/
* SCSS: http://www.sitepoint.com/whats-difference-sass-scss/

### Framework Abstraction

* we use our own abstraction layout that can include SCSS modules from one or
  more other front end frameworks, e.g. Twitter Bootstrap or  Zurb Foundation.
  Advantages are:
  * we can use our own variable names
  * we can use our own naming conventions for CSS classes and elements
  * we can mix existing frameworks (and exchange parts later)
  * we can add own components without touching any framework stuff

### Theme vs. Skin

* we do not mix theme layout (position, size, margins, borders, sizes, weight),
  skin layout (font-faces, color schemes) and pictograms (icon fonts)

### Core vs. Components

* we provide styles for global GUI elements in core layout
* component authors need to provide layout of unique component elements in their
  own component SCSS as part of the component
* all core components are provided as single modules to simplify re-use

### Definition Rules

* all rules are done for CSS classes, not HTML element tags
* typography for bigger text blocks is encapsulated as core module

### Naming Conventions

* we use BEM naming conventions (Block - Element - Modifier) for css classes,
  adding a prefix to differ between core and component classes
* we don't use abbrevations for block and element names, .e.g. ``button``, not
  ``btn``
* we use nouns for blocks and elements, and adjectives for modifiers
* Syntax: {prefix-core-component}-{component-module-name}-{block-name}__{element-name}--{modifier-name}
* Prefixes for core and component: ``g`` and ``c``
* Name of "global core application module": ``elds``
* Exceptions:
  * class name for GUI element states, eg. "is selected": .is-{attribute-name}
  * class name for small layout helpers, eg. "bold font weight": .u-{layouthelper-name}
* Examples:
  * Button element: ``g-elds-button``
  * Large button: ``g-elds-button--large``
  * Button symbol: ``g-elds-button__symbol``
  * element state: ``is-selected``
  * bold font weight: ``u-boldfontweight``
* Links:
  * BEM: http://bem.info/method/definitions/
  * http://csswizardry.com/2013/01/mindbemding-getting-your-head-round-bem-syntax/

### Code Conventions

* we use code formating rules from Idomatic CSS
* https://github.com/necolas/idiomatic-css/blob/master/README.md

### Dev Rules

* we use multi class patter in templates:
  ``<button class="g-elds-button g-elds-button--large">Label</button>``

In SCSS this means:

````
.g-elds-button {
    /* global button rules */
    font-size: 1em;
    
    // states
    
    &:hover {
    }
    
    &.is-selected {
    }
}

.g-elds-button--large {
    font-size: 2em;
}
````


