# Logical rating test fixtures

- `corpus.csv`: 420 puzzles with expected tiers, effort scores, final-pass move counts,
  and move/effect totals across all passes of the current logical evaluator.
- `technique-states.csv`: 40 synthetic candidate states and expected evidence signatures
  for 20 advanced techniques, in original and transposed orientations.

Tests verify uniqueness with an independent solver, replay logical steps, and check
concurrent determinism. Expected results are assertions, never solver input. Changes to
rating behavior require reviewing the corpus and the rating version together; tests do
not regenerate their own expected results.

Run:

```bash
./gradlew :app:testFossDebugUnitTest --tests 'com.kaajjo.libresudoku.generation.validation.*'
```
