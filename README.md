# RAMA - Request Augmentation for Model Awareness

Automated analysis of model and metamodel changes in GitHub Pull Requests.

This project provides a **GitHub Actions-based toolchain** that detects, analyzes, and reports changes in software metamodels and models (e.g., `.ecore`, `.model`, or custom extensions) using **EMF Compare**, directly inside PRs.

## Configuration

RAMA can be configured per target repository. The target repository is the repository where the GitHub Action runs and whose pull requests RAMA analyzes.

If the target repository does not contain a `rama.json` file, RAMA uses its packaged default configuration. If you want RAMA to analyze extensions or metamodels other than the defaults, add a `rama.json` file at the root of the target repository.

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
- `metamodels`: metamodel paths or identifiers used by RAMA when analyzing model files.

For metamodels other than Ecore, the metamodel files must be present in the target repository and listed in the `metamodels` field using paths relative to the repository root.
