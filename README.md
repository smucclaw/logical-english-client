# logical-english-client

This Clojurescript project implements a client for interacting with a
[Logical English server](https://github.com/smucclaw/LogicalEnglish).
For context, this was originally written to enable
[L4 web forms](https://github.com/smucclaw/documentation/blob/20231116-resume-docs/docs/webform.rst)
to query the Logical English backend with their JSON form data.

## API

While this project is written in Clojurescript, it compiles to an ES Module
(ESM) which exports
an API that can be used from Javascript (and any other language that compiles
to Javascript).
There are two main functions of interest here, namely:
- `query_le_js`

  This queries a Logical English server using:
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

## Project Structure
TODO