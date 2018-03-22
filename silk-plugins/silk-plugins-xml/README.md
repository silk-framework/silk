# XML Dataset

## Internal Specification

- It understands URI value typed paths and String value typed paths, and handles them differently
- URI value typed paths will always return the default entity URI of the referenced XML node
- String value typed paths will return the string value of an attribute or an XML element with only a Text object inside it and no other children,
  in all other cases it will return no value.
