# RAMA - Request Augmentation for Model Awareness

Automated analysis of model and metamodel changes in GitHub Pull Requests.

This project provides a **GitHub Actions-based toolchain** that detects, analyzes, and reports changes in software metamodels and models (e.g., `.ecore`, `.model`, or custom extensions) using **EMF Compare**, directly inside PRs.

## Quick start

RAMA is designed to run in the repository whose pull requests it analyzes.

1. Add a `rama.json` file to the root of the target repository. Start with the [configuration](#configuration) below and adapt its extensions and metamodel paths.
2. Copy [`rama.example.yml`](rama.example.yml) to `.github/workflows/rama.yml` in that repository.
3. Open or update a pull request that modifies a configured model or metamodel file. The workflow builds RAMA, retrieves the changed files through the GitHub API, and posts its report as a pull-request comment.

The example workflow listens to `opened`, `synchronize`, and `reopened` events and requires these permissions:

```yaml
permissions:
  contents: read
  pull-requests: write
```

It uses `pull_request_target` deliberately. This gives RAMA permission to read the target repository and write its report while the checked-out workspace remains on the trusted target branch. Do not change the workflow to check out or execute arbitrary code from an untrusted pull-request branch.

The workflow template clones this repository from `https://github.com/sdiaz1208/rama.git`. If you use a fork or a fixed revision of RAMA, replace that URL (and preferably pin the revision) in `.github/workflows/rama.yml`.

## Configuration

RAMA can be configured per target repository. The target repository is the repository where the GitHub Action runs and whose pull requests RAMA analyzes. The workflow reads `rama.json` from the checked-out target branch, so configuration changes made only in a pull request take effect after they are merged.

If the target-branch revision does not contain a `rama.json` file, or the file is empty or invalid JSON, RAMA uses its packaged default configuration and adds a configuration warning to the pull-request comment. If you want RAMA to analyze extensions or metamodels other than the defaults, add a valid `rama.json` file at the root of the repository.

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

Use extensions including the leading dot (for example, `.ecore`) and repository-relative paths for `metamodels`:

```json
{
  "model_extensions": [".xmi"],
  "metamodel_extensions": [".ecore"],
  "metamodels": ["metamodels/domain.ecore"]
}
```

Configured metamodels are loaded from the GitHub Actions workspace, which is the target branch when the supplied `pull_request_target` workflow is used. Consequently, a pull request that adds a model and the metamodel it depends on cannot yet be fully loaded by RAMA: the new metamodel is not available in that workspace. Merge the metamodel first, or expect RAMA to report a loading failure for the model. Loading metamodels directly from the pull-request revision is a planned enhancement.

## Automated system testing

RAMA uses [`reprogit`](https://github.com/alfonsodelavega/reprogit) to generate a deterministic Git repository for automated system tests. `reprogit` builds repository history from ordered fixture folders, allowing tests to exercise commits, branches, pull requests, and history-dependent workflows.

`tests/system/fixture/` contains this test fixture. Its files and commit history are test data: preserve both unless a test intentionally requires a change to the generated repository history. See the `reprogit` repository (https://github.com/alfonsodelavega/reprogit) for fixture format and usage details.
