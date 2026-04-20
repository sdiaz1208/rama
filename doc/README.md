# PR Cases for a model file

This file lists the practical cases that can occur when a PR touches a file with a configured model extension. The cases are grouped by analysis dimension so they can be used both for implementation logic and for test design.

## Terminology mapping

- **GitHub API:** a pull request exposes `pull_request.head` (the source branch or head branch containing the proposed changes) and `pull_request.base` (the target branch into which the PR is merged). The common ancestor commit of those tips is available via the merge base endpoint (`GET /repos/:owner/:repo/merges/:base...:head`).
- **This document:** whenever we write *source branch* we mean the GitHub head ref and *target branch* means the GitHub base ref; *merge base* maps to the common ancestor commit from the `merges` API and is where we evaluate the origin-relative cases in section 2.
- **EMF Compare:** we usually align the GitHub roles with EMF Compare sides as follows: the target branch tip becomes the *left* or *reference* model, the source branch tip becomes the *right* or *working* model, and the merge base computation provides the *origin* or *ancestor* model that represents their shared history. Use this mapping when interpreting the case tables in terms of EMF comparison inputs (e.g., "target branch" entries refer to "left" in EMF Compare).

## 1. File-level cases

| ID | Dimension | Case | Meaning | Typical handling |
| --- | --- | --- | --- | --- |
| 1.1 | File status | unchanged at PR tip | Source and target tips contain the same file content | Usually skip |
| 1.2 | File status | added in source | File exists only in source branch | Analyze as creation |
| 1.3 | File status | deleted in source | File exists only in target branch | Analyze as deletion |
| 1.4 | File status | modified | File exists in both branches and contents differ | Pairwise compare |
| 1.5 | File status | renamed or moved | Same logical file but path changed | Track rename if possible |
| 1.6 | File status | extension changed into model | File was not previously treated as model, now it is | Analyze as newly relevant |
| 1.7 | File status | extension changed out of model | File was previously model, now it is not | Report loss of relevance if needed |

## 2. Branch-origin cases relative to merge base

| ID | Dimension | Case | Meaning | Why it matters |
| --- | --- | --- | --- | --- |
| 2.1 | Branch origin | source-only changed | Source changed since merge base, target did not | No two-sided syntactic conflict expected |
| 2.2 | Branch origin | target-only changed | Target changed since merge base, source did not | PR context changed; re-analysis needed on PR update |
| 2.3 | Branch origin | both source and target changed | Both branches changed after divergence | Conflict detection becomes relevant |

## 3. Textual change impact cases

| ID | Dimension | Case | Meaning | Examples |
| --- | --- | --- | --- | --- |
| 3.1 | Text impact | textual-only change | Text differs but loaded model information is equivalent | trailing whitespace removal, indentation changes, line ending changes, serializer reordering that preserves syntactics, non-syntactic comments |
| 3.2 | Text impact | syntactic model change | Text differs and the EMF model information changes | object rename, attribute update, reference retargeting, object add/delete, containment move, ordered feature reorder |

## 4. Load and parse cases

| ID | Dimension | Case | Meaning | Typical report |
| --- | --- | --- | --- | --- |
| 4.1 | Parsing | both versions load | Source and target versions can be loaded as models | Continue to syntactic comparison |
| 4.2 | Parsing | source fails to load | Source version is malformed or unresolved | Analysis incomplete / invalid source |
| 4.3 | Parsing | target fails to load | Target version is malformed or unresolved | Analysis incomplete / invalid target |
| 4.4 | Parsing | both fail to load | Neither version can be interpreted | Analysis incomplete |

## 5. Metamodel conformance cases

| ID | Dimension | Case | Meaning | Typical report |
| --- | --- | --- | --- | --- |
| 5.1 | Conformance | both conform | Both versions validate against the resolved metamodel | Continue |
| 5.2 | Conformance | source does not conform | PR source model violates metamodel | Invalid source model |
| 5.3 | Conformance | target does not conform | Target-side version violates metamodel | Invalid target model |
| 5.4 | Conformance | both do not conform | Neither side conforms | Analysis incomplete / invalid model |

## 6. Metamodel dependency cases

| ID | Dimension | Case | Meaning | Example |
| --- | --- | --- | --- | --- |
| 6.1 | Model/metamodel relation | model changed, metamodel unchanged | Normal model-only comparison | rename an element in `.model` |
| 6.2 | Model/metamodel relation | model unchanged syntactically, metamodel changed | Model may still need revalidation | metamodel feature renamed |
| 6.3 | Model/metamodel relation | model and metamodel changed consistently | Combined evolution is coherent | model updated to use renamed feature |
| 6.4 | Model/metamodel relation | model and metamodel changed inconsistently | Model may no longer load or conform | model still uses removed class/feature |

## 7. syntactic-difference cases

| ID | Dimension | Case | Meaning | Examples |
| --- | --- | --- | --- | --- |
| 7.1 | syntactic diff | no syntactic differences | Only textual or serialization noise | formatting-only changes |
| 7.2 | syntactic diff | additions only | New model elements or values appear | new object or reference |
| 7.3 | syntactic diff | deletions only | Existing model elements or values disappear | delete object or reference |
| 7.4 | syntactic diff | updates only | Existing elements keep identity but properties change | rename, attribute value change |
| 7.5 | syntactic diff | moves only | Element location/containment changes | containment relocation |
| 7.6 | syntactic diff | mixed changes | Combination of add/delete/update/move | typical real PR |

## 8. Conflict cases when both branches changed

| ID | Dimension | Case | Meaning | Examples |
| --- | --- | --- | --- | --- |
| 8.1 | Conflict | no conflict | Both sides changed but changes are compatible | edits on disjoint elements/features |
| 8.2 | Conflict | same effective change | Both sides made the same syntactic change | both renamed object to same value |
| 8.3 | Conflict | textual conflict but not syntactic conflict | Serialization differs but loaded models are equivalent | different pretty-printing or ordering noise |
| 8.4 | Conflict | same feature changed differently | Same property modified incompatibly | object renamed to different values |
| 8.5 | Conflict | delete-vs-update | One side deletes an element the other updates | target deletes object, source renames it |
| 8.6 | Conflict | incompatible reference change | Same reference retargeted differently | different parent/container/reference target |
| 8.7 | Conflict | incompatible ordered change | Ordered feature changed incompatibly | list reordered in conflicting ways |
| 8.8 | Conflict | incompatible add with same identity/key | Both sides add elements that cannot coexist | duplicate logical identifier |
| 8.9 | Conflict | metamodel-induced conflict | A model change becomes incompatible because of metamodel evolution | source uses feature removed in target metamodel |

## 9. Report outcome cases

| ID | Outcome | Meaning | When to use it |
| --- | --- | --- | --- |
| 9.1 | no relevant model/metamodel files detected | PR touched no configured model/metamodel files | Repository-level outcome |
| 9.2 | relevant file but no syntactic model change | Relevant file changed only textually | formatting-only or serializer-only diff |
| 9.3 | syntactic changes without conflicts | Model information changed and no conflict was found | source-only syntactic edit or compatible two-sided edit |
| 9.4 | syntactic changes with conflicts | Model information changed and syntactic conflict was found | incompatible two-sided change |
| 9.5 | analysis incomplete due to parse/load failure | One or both versions could not be loaded | malformed file, missing dependency |
| 9.6 | analysis incomplete due to conformance failure | Model loads but violates metamodel | invalid model state |
| 9.7 | analysis incomplete due to metamodel resolution failure | Required metamodel could not be resolved | missing or incompatible metamodel |

## 10. Minimal per-file decision flow

1. Is the file relevant according to configured model extensions?
2. Is it added, deleted, renamed, or modified?
3. Did the source branch, target branch, or both change it since merge base?
4. Can both versions be loaded?
5. Do both conform to the resolved metamodel?
6. Is the difference only textual, or syntactic at model level?
7. If both branches changed syntactically, is there a conflict?
8. Which report outcome should be emitted?


## Question

### Prompt
The pr-model-file-cases-all.puml file contains a UML activity diagram that details the possible cases for a file change in a PR. I'm interested in studying this for my model change analysis tool. However, I'm not entirely sure how the status of the file (added/deleted/modified) is useful.

### Answer (OpenAI Codex v0.118.0 - model: gpt-5.4 high)
In this diagram set, file status is useful, but only as an early routing decision, not as the main analytical result. That role is visible in the early routing branch in `doc/pr-model-file-cases-all.puml` and in the tables in this `doc/README.md`.

What it is good for:
- added means there is no source-side predecessor to compare against, so treat it as model creation.
- deleted means there is no source-side file anymore, so treat it as model deletion.
- modified means both sides exist, so do a pairwise syntactic comparison.
- renamed/moved matters because path changed, but the logical model may be the same file identity.

What it is not good for:
- It does not tell you whether the change is syntactic or textual-only. That comes later in the flow (see `doc/2-way-comparison.puml` and `doc/3-way-comparison.puml`).
- It does not tell you whether there is a conflict. That depends on merge-base analysis and both-side syntactic changes (see `doc/3-way-comparison.puml` and section 8 in this `doc/README.md`).
- It does not capture the meaningful model-level categories like add/delete/update/move of elements, which are listed in section 7 in this `doc/README.md`.

So for your tool, I would treat file status as input-shaping metadata:

- useful for deciding how to load and compare versions,
- useful for reporting creation/deletion/rename at file level,
- not sufficient for model change analysis by itself.

If your goal is “model change analysis”, the dimensions that matter more than file status are:

- branch origin relative to merge base,
- parse/load success,
- metamodel conformance,
- syntactic diff kind,
- conflict detection,
- model/metamodel co-evolution.

In short: file status tells you how to start analysis; syntactic diff tells you what actually changed.
