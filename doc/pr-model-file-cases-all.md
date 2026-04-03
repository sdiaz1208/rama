# PR Cases for a `.model` File

This file lists the practical cases that can occur when a PR touches a file with a configured model extension. The cases are grouped by analysis dimension so they can be used both for implementation logic and for test design.

## 1. File-level cases

| Dimension | Case | Meaning | Typical handling |
| --- | --- | --- | --- |
| File status | unchanged at PR tip | Source and target tips contain the same file content | Usually skip |
| File status | added in source | File exists only in source branch | Analyze as creation |
| File status | deleted in source | File exists only in target branch | Analyze as deletion |
| File status | modified | File exists in both branches and contents differ | Pairwise compare |
| File status | renamed or moved | Same logical file but path changed | Track rename if possible |
| File status | extension changed into model | File was not previously treated as model, now it is | Analyze as newly relevant |
| File status | extension changed out of model | File was previously model, now it is not | Report loss of relevance if needed |

## 2. Branch-origin cases relative to merge base

| Dimension | Case | Meaning | Why it matters |
| --- | --- | --- | --- |
| Branch origin | source-only changed | Source changed since merge base, target did not | No two-sided semantic conflict expected |
| Branch origin | target-only changed | Target changed since merge base, source did not | PR context changed; re-analysis needed on PR update |
| Branch origin | both source and target changed | Both branches changed after divergence | Conflict detection becomes relevant |

## 3. Textual change impact cases

| Dimension | Case | Meaning | Examples |
| --- | --- | --- | --- |
| Text impact | textual-only change | Text differs but loaded model information is equivalent | trailing whitespace removal, indentation changes, line ending changes, serializer reordering that preserves semantics, non-semantic comments |
| Text impact | semantic model change | Text differs and the EMF model information changes | object rename, attribute update, reference retargeting, object add/delete, containment move, ordered feature reorder |

## 4. Load and parse cases

| Dimension | Case | Meaning | Typical report |
| --- | --- | --- | --- |
| Parsing | both versions load | Source and target versions can be loaded as models | Continue to semantic comparison |
| Parsing | source fails to load | Source version is malformed or unresolved | Analysis incomplete / invalid source |
| Parsing | target fails to load | Target version is malformed or unresolved | Analysis incomplete / invalid target |
| Parsing | both fail to load | Neither version can be interpreted | Analysis incomplete |

## 5. Metamodel conformance cases

| Dimension | Case | Meaning | Typical report |
| --- | --- | --- | --- |
| Conformance | both conform | Both versions validate against the resolved metamodel | Continue |
| Conformance | source does not conform | PR source model violates metamodel | Invalid source model |
| Conformance | target does not conform | Target-side version violates metamodel | Invalid target model |
| Conformance | both do not conform | Neither side conforms | Analysis incomplete / invalid model |

## 6. Metamodel dependency cases

| Dimension | Case | Meaning | Example |
| --- | --- | --- | --- |
| Model/metamodel relation | model changed, metamodel unchanged | Normal model-only comparison | rename an element in `.model` |
| Model/metamodel relation | model unchanged semantically, metamodel changed | Model may still need revalidation | metamodel feature renamed |
| Model/metamodel relation | model and metamodel changed consistently | Combined evolution is coherent | model updated to use renamed feature |
| Model/metamodel relation | model and metamodel changed inconsistently | Model may no longer load or conform | model still uses removed class/feature |

## 7. Semantic-difference cases

| Dimension | Case | Meaning | Examples |
| --- | --- | --- | --- |
| Semantic diff | no semantic differences | Only textual or serialization noise | formatting-only changes |
| Semantic diff | additions only | New model elements or values appear | new object or reference |
| Semantic diff | deletions only | Existing model elements or values disappear | delete object or reference |
| Semantic diff | updates only | Existing elements keep identity but properties change | rename, attribute value change |
| Semantic diff | moves only | Element location/containment changes | containment relocation |
| Semantic diff | mixed changes | Combination of add/delete/update/move | typical real PR |

## 8. Conflict cases when both branches changed

| Dimension | Case | Meaning | Examples |
| --- | --- | --- | --- |
| Conflict | no conflict | Both sides changed but changes are compatible | edits on disjoint elements/features |
| Conflict | same effective change | Both sides made the same semantic change | both renamed object to same value |
| Conflict | textual conflict but not semantic conflict | Serialization differs but loaded models are equivalent | different pretty-printing or ordering noise |
| Conflict | same feature changed differently | Same property modified incompatibly | object renamed to different values |
| Conflict | delete-vs-update | One side deletes an element the other updates | target deletes object, source renames it |
| Conflict | incompatible reference change | Same reference retargeted differently | different parent/container/reference target |
| Conflict | incompatible ordered change | Ordered feature changed incompatibly | list reordered in conflicting ways |
| Conflict | incompatible add with same identity/key | Both sides add elements that cannot coexist | duplicate logical identifier |
| Conflict | metamodel-induced conflict | A model change becomes incompatible because of metamodel evolution | source uses feature removed in target metamodel |

## 9. Report outcome cases

| Outcome | Meaning | When to use it |
| --- | --- | --- |
| no relevant model/metamodel files detected | PR touched no configured model/metamodel files | Repository-level outcome |
| relevant file but no semantic model change | Relevant file changed only textually | formatting-only or serializer-only diff |
| semantic changes without conflicts | Model information changed and no conflict was found | source-only semantic edit or compatible two-sided edit |
| semantic changes with conflicts | Model information changed and semantic conflict was found | incompatible two-sided change |
| analysis incomplete due to parse/load failure | One or both versions could not be loaded | malformed file, missing dependency |
| analysis incomplete due to conformance failure | Model loads but violates metamodel | invalid model state |
| analysis incomplete due to metamodel resolution failure | Required metamodel could not be resolved | missing or incompatible metamodel |

## 10. Minimal per-file decision flow

1. Is the file relevant according to configured model extensions?
2. Is it added, deleted, renamed, or modified?
3. Did the source branch, target branch, or both change it since merge base?
4. Can both versions be loaded?
5. Do both conform to the resolved metamodel?
6. Is the difference only textual, or semantic at model level?
7. If both branches changed semantically, is there a conflict?
8. Which report outcome should be emitted?


## Question

### Prompt
The pr-model-file-cases.puml file contains a UML activity diagram that details the possible cases for a file change in a PR. I'm interested in studying this for my model change analysis tool. However, I'm not entirely sure how the status of the file (added/deleted/modified) is useful.

### Answer (OpenAI Codex v0.118.0 - model: gpt-5.4 high)
In this diagram, file status is useful, but only as an early routing decision, not as the main analytical result. That role is visible in the first branch of the activity flow at doc/pr-model-file-cases.puml:8 and in the markdown summary at doc/pr-model-file-cases.md:9.

What it is good for:
- added means there is no source-side predecessor to compare against, so treat it as model creation.
- deleted means there is no source-side file anymore, so treat it as model deletion.
- modified means both sides exist, so do a pairwise semantic comparison.
- renamed/moved matters because path changed, but the logical model may be the same file identity.

What it is not good for:
- It does not tell you whether the change is semantic or textual-only. That comes later in the flow at doc/pr-model-file-cases.puml:29.
- It does not tell you whether there is a conflict. That depends on merge-base analysis and both-side semantic changes at doc/pr-model-file-cases.puml:19 and doc/pr-model-file-cases.puml:35.
- It does not capture the meaningful model-level categories like add/delete/update/move of elements, which are listed at doc/pr-model-file-cases.md:63.

So for your tool, I would treat file status as input-shaping metadata:

- useful for deciding how to load and compare versions,
- useful for reporting creation/deletion/rename at file level,
- not sufficient for model change analysis by itself.

If your goal is “model change analysis”, the dimensions that matter more than file status are:

- branch origin relative to merge base,
- parse/load success,
- metamodel conformance,
- semantic diff kind,
- conflict detection,
- model/metamodel co-evolution.

In short: file status tells you how to start analysis; semantic diff tells you what actually changed.
