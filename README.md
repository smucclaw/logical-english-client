# le-webform-glue

## Dependencies

- java
- [pnpm](https://pnpm.io/installation)

This project is developed with JDK LTS 21 and nodejs LTS 20.10.0.

## Usage
### Setup
```shell
  pnpm install
```

### Demo
- Run the following command to start a local dev server in the `public` directory:

  ```shell
    pnpm start
  ```

- Go to <http://localhost:8000> in your browser.
  You should see a justification tree visualised by
  [guifier](https://guifier.com/).

- If you edit and save a file, it will recompile the code and reload the
  browser to show the updated version.

### Compile an optimized version

```shell
  pnpm build:prod
```