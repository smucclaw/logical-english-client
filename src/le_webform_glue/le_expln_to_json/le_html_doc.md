# Documenting the format of explanations output by the LE backend

This document describes the format of the explanation trees
obtained when using `pengines` to query the LE swish server.

## Summary of trees formatted as HTML strings
These trees come in the form of strings containing structured
HTML data.
Fortunately, the format of these HTML strings is relatively simple
and easy to post-process.
In a nutshell, they have a recursive structure, representing trees, where:
- **Nodes** contain a goal that was proven, or failed to be proven.
- **Edges** from say node A to B represent the `A because B` relation.

## Leaf nodes
More concretely, leaf nodes have the following form:

### Goals that Prolog succeeded in proving:

```html
<li title="Rule inference">
  <span class="Leaf"> </span>
  <b> [goal text] </b>
</li>
```

- Goals that Prolog failed to prove:

```html
<li title="Failed goal">
  <span class="Leaf"> </span>
  <span style="color:red">It cannot be proved that</span>
  <b> [goal text] </b>
</li>
```

## Non-leaf nodes
Non-leaf nodes, ie those with children, have the following form:

### Goals that Prolog succeeded in proving:

```html
<li title="Rule inference">
  <span class="Box"> </span>
  <b> [goal text] </b>
  <ul class="nested">
    because
      [child 0]
      and
      ...
      and
      [child n]
  </ul>
</li>
```

### Goals that Prolog failed to prove:
```html
<li title="Failed goal">
  <span class="Leaf"> </span>
  <span style="color:red">It cannot be proved that</span>
  <b> [goal text] </b>
  <ul class="nested">
    because
      [child 0]
      and
      ...
      and
      [child n]
  </ul>
</li>
```

## Technical details
The main entry point exposed by LE to the swish server is the
[parse_and_query_and_explanation/5](https://github.com/smucclaw/LogicalEnglish/blob/e6436818834b6a9132e692f6fea1cd0cb1e0e325/le_answer.pl#L917C10-L917C10)
predicate, which is used to evaluate queries and produce explanation trees.
The [answer/4](https://github.com/smucclaw/LogicalEnglish/blob/e6436818834b6a9132e692f6fea1cd0cb1e0e325/le_answer.pl#L234C1-L234C1)
is used to generate a Prolog term representing the explanation tree, and 
[produce_html_explanation/2](https://github.com/smucclaw/LogicalEnglish/blob/e6436818834b6a9132e692f6fea1cd0cb1e0e325/le_answer.pl#L982)
is then used to turn that into a HTML string, the format of which is documented
above.

Note that there is no other structured format like json that is involved in
the LE backend.
While it may have been possible to work directly with the Prolog term itself,
that would require extending the LE codebase with additional Prolog predicates
that did that.
I (Joe) do not understand the relevant parts of the codebase responsible for
manipulating the Prolog terms and such have opted not to do so.
In any case, the HTML strings are well structured and easy to transform, and
working directly with the underlying Prolog terms may prove no easier than doing
that.