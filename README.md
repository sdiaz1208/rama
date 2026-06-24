# RAMA - Request Augmentation for Model Awareness

Automated analysis of model and metamodel changes in GitHub Pull Requests.

This project provides a **GitHub Actions-based toolchain** that detects, analyzes, and reports changes in software metamodels and models (e.g., `.ecore`, `.model`, or custom extensions) using **EMF Compare**, directly inside PRs.

## Configuration

RAMA can be configured per target repository. The target repository is the repository where the GitHub Action runs and whose pull requests RAMA analyzes. For a pull request, RAMA reads the version of `rama.json` from the source revision.

If the source revision of a pull request does not contain a `rama.json` file, or the file is empty or invalid JSON, RAMA uses its packaged default configuration and adds a configuration warning to the pull-request comment. If you want RAMA to analyze extensions or metamodels other than the defaults, add a valid `rama.json` file at the root of the repository.

The file must use this structure. The following example shows RAMA's default configuration:

```json
{
  "model_extensions": [".model"],
  "metamodel_extensions": [".ecore"],
  "metamodels": []
}
```

- `model_extensions`: file extensions that RAMA should treat as model files.
- `metamodel_extensions`: file extensions that RAMA should treat as metamodel files.
- `metamodels`: paths of metamodel files used by RAMA when analyzing model files.

The metamodel files must be listed in the `metamodels` field using paths relative to the repository root. RAMA retrieves each configured metamodel from the source, target, and merge-base revisions, so a pull request can add a metamodel and a conforming model together.


## Automated system testing

RAMA uses [`reprogit`](https://github.com/alfonsodelavega/reprogit) to generate a deterministic Git repository for automated system tests. `reprogit` builds repository history from ordered fixture folders, allowing tests to exercise commits, branches, pull requests, and history-dependent workflows.

`tests/system/fixture/` contains this test fixture. Its files and commit history are test data: preserve both unless a test intentionally requires a change to the generated repository history. See the `reprogit` repository (https://github.com/alfonsodelavega/reprogit) for fixture format and usage details.