# logical-english-client

This Clojurescript project implements a client for interacting with
[Logical English](https://github.com/smucclaw/LogicalEnglish).
For context, this was originally written to enable
[L4 web forms](https://l4-documentation.readthedocs.io/en/latest/docs/webform.html)
to query the Logical English backend with JSON form data.

## API

While this project is written in Clojurescript, it compiles to an ES Module
(ESM) which exports
an API that can be used from Javascript (and any other language that compiles
to Javascript).
There are two main functions of interest here, namely:
- `query_le_js`

  This queries a server running the Logical English code base using:
  - A JSON instance
  - A Logical English program
  - A Logical English query string 

  It returns, in the form of JSON, an explanation tree from the
  Logical English Prolog engine.

- `render_le_resp_with_guifier`

  This uses [guifier](https://guifier.com/) to display the JSON
  explanation tree obtained from the above `query_le_js` function in HTML.

The [public](public) directory contains a demo project with
[index.js](public/index.js) which shows how one can use these functions,
along with others which are exported by the ESM API.
For details on how to run the demo, see the
[Demo section below](#running-the-demo).

Other functions exported by the API can be found in
[shadow-cljs.edn](shadow-cljs.edn), the configuration file for the
Clojurescript [shadow-cljs](https://github.com/thheller/shadow-cljs)
tool, which we use to compile this project to ESM.

For more details about the Clojurescript code base, see the
[Project Structure and Details section](#project-structure-and-details)

## Dependencies

- java
- [pnpm](https://pnpm.io/installation)

This project is developed with JDK LTS 21 and nodejs LTS 20.10.0.

## Usage
### Setup
```shell
  pnpm install
```

### Running the demo
- Make sure to [setup](#setup) the project first.

- Run the following command to start a local dev server in the `public` directory:

  ```shell
    pnpm start
  ```

- Go to <http://localhost:8000> in your browser.
  You should see a justification tree visualised by
  [guifier](https://guifier.com/).

- If you edit and save a file, it will recompile the code and reload the
  browser to show the updated version.

### Compile an optimised version of the library

Run the following command:

```shell
  pnpm build:prod
```

This compiles an optimised, production-ready version of the library to
`public/js/main.js`.

## Project Structure and Details
Recall from the [introduction section](#logical-english-client)
that this project allows one to querying a Logical English backend server
with JSON form data.
As mentioned in the [API section](#api),
this project is written in Clojurescript, and compiles to ESM via the
[shadow-cljs](https://github.com/thheller/shadow-cljs)
tool.
Configuration details, along with all the functions exported by the ESM API can
be found in the [shadow-cljs configuration file](shadow-cljs.edn).

This project is structured as a pipeline composed from 3 namespaces:
1. [webform-facts-to-le.core](src/logical_english_client/webform_facts_to_le/core.cljs)

    The first step of the pipeline is to transform
    JSON data into a Logical English scenario.
    Conceptually, this transforms nested JSON instances into
    [RDF](https://www.oxfordsemantic.tech/faqs/what-is-rdf)
    subject-predicate-object
    (or alternatively, entity-attribute-value)
    triples which is commonly used by graph and Datalog databases,
    as described in
    [this section](https://l4-documentation.readthedocs.io/en/latest/docs/webform.html#reasoning-about-instances-of-classes-with-constitutive-rules)
    of the L4 web form documentation.

    `data->le-scenario` performs this transformation, relying on
    [Asami](https://github.com/quoll/asami)
    for much of the heavy lifting.
    - Asami is a schema-less in-memory graph database for Clojure(script),
      which comes with facilities for transforming nested JSON instances into
      RDF triples.
    - We then transform these entity-attribute-value triples output by Asami
      into a Logical English scenario.
      This is in turn achieved by transforming each triple to a
      term corresponding to the following template (ie. predicate signature):
      ```
        *a entity*'s *a attribute* is *a value*
      ```

1. [le-api-client](src/logical_english_client/le_api_client)

    Given a Logical English scenario, such as one obtained from a
    JSON instance as output by `data->le-scenario` in the above
    step,
    the next step of the pipeline is to combine this with a Logical English
    program and query string, and then query a Logical English backend.

    We support multiple Logical English server backends, and this namespace
    comprises multiple sub namespaces, each allowing one to query
    a different backend.
    Currently, we have 2 functions, each for querying a different backend, namely:
    1. The `query-le!` function in the
        [le-api-client.pengines.core](src/logical_english_client/le_api_client/pengines/core.cljs)
        namespace is used to query a server running the
        [Logical English](https://github.com/smucclaw/LogicalEnglish)
        code base, like the
        [pre-packaged docker builds](https://github.com/smucclaw/LogicalEnglish#using-pre-packaged-docker).
        A publicly available server instance maintained by CCLAW can be found at
        [this url](https://le.dev.cclaw.legalese.com/).
     
        This utilises the
        [Pengines API](https://www.swi-prolog.org/pldoc/doc_for?object=section(%27packages/pengines.html%27)) as implemented in
        the [pengines Javascript client library](packages/pengines/pengines.js)
        to query the Logical English server.
        - The design here is based on what is implemented in the
          [runPengine function](https://github.com/smucclaw/LogicalEnglish/blob/1e4c2bf9e3baaa02e76fca714117001ce82dc9d0/le-ui/extension.js#L224)
          used in the Logical English vscode extension.

    1.  The `query-le-wasm!` function in the
        [le-api-client.swipl-wasm.core](src/logical_english_client/le_api_client/swipl_wasm/core.cljs)
        namespace is used to query
        [SWI-Prolog running on Wasm](https://github.com/SWI-Prolog/npm-swipl-wasm).

        This loads Logical English into the Wasm version of SWI-Prolog
        via the [le.qlf](public/le.qlf) file.
        - This [quick load file](https://www.swi-prolog.org/pldoc/man?section=qlf)
          was obtained from compiling the [le.pl](https://github.com/smucclaw/LogicalEnglish/blob/qlf-wasm/le.pl)
          file and functions as a tarball bundling it all together.
        - Note that the `le.pl` Prolog file is found in the `qlf-wasm` branch of
          the Logical English code base, and that this branch differs from `main`
          in that it has been modified to work on the Wasm version of SWI-Prolog.
          The technical reason is that
          `main` does not work well on Wasm as it utilises some file IO
          and [SWISH](https://github.com/SWI-Prolog/swish)
          related functionality that are not supported there.
          These have been removed in the `qlf-wasm` branch.

    These functions all return a HTML string
    (wrapped in a [Promise](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Promise))
    representing the explanation tree obtained from querying Logical English.
    The structure of these HTML strings is documented in
    [this document](src/logical_english_client/le_expln_to_json/le_html_doc.md).

    - More technically, these HTML strings represent execution trees
      of the backward chaining computations performed by the meta-interpreter
      in the Logical English Prolog codebase.

1. [le-expln-to-json.core](src/logical_english_client/le_expln_to_json/core.cljs)

    Given a HTML string obtained from Logical English, representing a HTML
    tree, as obtained from one of the functions in the above step,
    the last step is to transform it into JSON.
    `le-html-str->clj` is responsible for:
    - Transforming the HTML strings into a tree in the form of nested
      Clojure maps (which can then be transformed into JSON),
      stripping HTML related tags and metadata in the process.
      Internally, it uses `hiccup->map`, which transforms HTML in Hiccup format
      to an internal AST, stripping formatting related metadata in the process.

    - Performing some post-processing on the explanation tree to improve the
      readability of the output via `post-process-le-expln-map`.

    These transformations rely heavily on
    [meander](https://github.com/noprompt/meander),
    a term rewriting library for Clojure(script),
    to recursively traverse and rewrite subtrees.
    Meander lets us conveniently define transformations with symbolic rewriting rules,
    much the same way that one defines
    [small-step semantics](https://people.csail.mit.edu/feser/pld-s23/semantics2.html).
    It has combinators like
    [top-down](https://cljdoc.org/d/meander/epsilon/0.0.650/api/meander.strategy.epsilon#top-down),
    [bottom-up](https://cljdoc.org/d/meander/epsilon/0.0.650/api/meander.strategy.epsilon#bottom-up),
    and [cata](https://cljdoc.org/d/meander/epsilon/0.0.650/api/meander.epsilon#cata)(morphism)
    which eliminate the pain of manually recursively traversing and
    identifying redexes to rewrite.