# Claude Code CLI Search — Model & Effort Testing Plan

## Objective

Determine the optimal model + effort combination for search queries. We need fast, accurate responses without overspending tokens.

## Phase 1: Baseline Testing

Run the test script in the Arch Linux chroot to measure raw CLI performance.

### Setup

```bash
# In the chroot as maplestar
mkdir -p ~/claude-search-tests
cd ~/claude-search-tests
```

### Test Script

```bash
#!/bin/bash
# claude-search-benchmark.sh

RESULTS_FILE="benchmark-results.csv"
echo "model,effort,query,time_seconds,output_lines,tokens_approx" > "$RESULTS_FILE"

MODELS=("claude-haiku-4-5-latest" "claude-sonnet-4-5-latest")
# Skip opus for initial testing — likely too slow for search

QUERIES=(
    "list 5 most recent photos in /android-root/sdcard/DCIM"
    "find files larger than 50MB in /android-root/sdcard"
    "read /android-root/system/build.prop and tell me the android version"
    "count how many apps are installed in /android-root/data/app"
    "find all pdf files in /android-root/sdcard/Download"
)

for model in "${MODELS[@]}"; do
    for query in "${QUERIES[@]}"; do
        echo "Testing: $model | $query"
        
        start=$(date +%s%N)
        output=$(claude -p "$query" \
            --model "$model" \
            --output-format json \
            2>/dev/null)
        end=$(date +%s%N)
        
        elapsed=$(echo "scale=2; ($end - $start) / 1000000000" | bc)
        lines=$(echo "$output" | wc -l)
        
        echo "$model,default,$query,$elapsed,$lines" >> "$RESULTS_FILE"
        echo "  → ${elapsed}s, ${lines} lines"
    done
done

echo
echo "Results saved to $RESULTS_FILE"
column -t -s',' "$RESULTS_FILE"
```

## Phase 2: Effort Comparison

If haiku is fast but inaccurate, test with higher effort. If sonnet is too slow, test with lower effort.

### Effort Flags

```bash
# Claude Code CLI doesn't have --reasoning-effort directly
# But we can control via system prompt and max tokens

# "Quick" mode — short, direct answers
claude -p "query" --model claude-haiku-4-5-latest --max-turns 1

# "Thorough" mode — allow tool use and exploration  
claude -p "query" --model claude-sonnet-4-5-latest
```

## Phase 3: Response Format Testing

Test what output format works best for parsing into Kvaesitso results.

### Structured Prompt Template

```bash
claude -p 'Answer this search query in JSON format.
Query: "find recent photos"
Response format: {"results": [{"type": "file|text|action", "title": "...", "description": "...", "path": "...", "uri": "..."}]}
Only return the JSON, no explanation.' \
    --model claude-haiku-4-5-latest \
    --output-format json
```

### What We're Measuring

| Metric | How | Target |
|--------|-----|--------|
| Latency | `time` command | < 3s for haiku, < 5s for sonnet |
| Accuracy | Manual review | Correct results 80%+ |
| Parse-ability | JSON validation | Valid JSON 95%+ |
| Token count | Output size proxy | < 1KB per response |
| Reliability | 10 runs same query | Consistent format 90%+ |

## Phase 4: Decision Matrix

After testing, fill in:

| Model | Avg Latency | Accuracy | Reliability | Recommendation |
|-------|-------------|----------|-------------|----------------|
| haiku | TBD | TBD | TBD | |
| sonnet | TBD | TBD | TBD | |

### Expected Outcome

- **haiku** — fast enough for search (< 2s), good for simple file queries, may struggle with complex natural language
- **sonnet** — better understanding, 2-4s range, good for "show me cat photos" type queries
- **Likely winner** — haiku for file/system queries, sonnet as fallback for complex queries

### Adaptive Model Selection

If testing confirms the above, implement adaptive selection:

```kotlin
fun selectModel(query: String): String {
    // Simple patterns → haiku (fast)
    if (query.matchesFilePattern()) return "claude-haiku-4-5-latest"
    
    // Complex natural language → sonnet (smart)
    return "claude-sonnet-4-5-latest"
}
```

## Next Steps

1. Run Phase 1 benchmark in chroot
2. Analyze results
3. Run Phase 2 if needed
4. Document findings
5. Implement `ClaudeCodeCLIRepository` with optimal config
