# Benchmark Results — Claude Code CLI Search

## Test Environment

- Device: Google Pixel 8 Pro (Tensor G4, 9 cores)
- Android 14, rooted via KernelSU
- Arch Linux chroot via chroot-distro (sabamdarif)
- Claude Code CLI v2.1.91
- Subscription: Claude Max
- Available models: haiku, sonnet, opus (via aliases)
- Note: `--bare` flag breaks OAuth auth — do not use

## Final Benchmark (haiku vs sonnet, with system prompt)

| Model | Query | Wall Time | API Time | Turns | Cost |
|-------|-------|-----------|----------|-------|------|
| haiku | List 3 recent photos | 12s | 7.3s | 2 | $0.013 |
| haiku | Find files >50MB | 12s | 7.4s | 2 | $0.012 |
| haiku | Read build.prop | 9s | 6.0s | 2 | $0.014 |
| sonnet | List 3 recent photos | 11s | 7.5s | 2 | $0.029 |
| sonnet | Find files >50MB | 10s | 6.7s | 2 | $0.024 |
| sonnet | Read build.prop | 10s | 6.9s | 2 | $0.033 |

## Key Findings

### Speed
- Haiku and sonnet are nearly identical in speed (~10-12s wall, ~7s API)
- Both consistently hit 2 turns with the system prompt
- ~5s overhead from CLI startup + chroot exec (wall minus API time)

### System Prompt Impact
- Without system prompt: 6 turns, 24s (first benchmark)
- With system prompt: 2 turns, 9-12s
- **System prompt halves latency** by giving Claude filesystem context upfront

### Cost
- Haiku: ~$0.013/query
- Sonnet: ~$0.029/query (~2x haiku)
- Same speed and turn count — haiku is better value

### Startup Overhead
- ~5s of every query is CLI startup + chroot login
- This is the main optimization target for future work
- Possible optimizations:
  - Keep a warm claude process running (daemon mode)
  - Use the Agent SDK instead of CLI for direct API calls
  - Pre-authenticated long-running process

## Decisions

- **Default model:** haiku (same speed as sonnet, half cost)
- **System prompt:** Always include filesystem context
- **No --bare flag:** Breaks OAuth authentication
- **No --max-turns:** Let Claude run as many tool loops as needed
- **Model aliases:** Use `haiku`/`sonnet`/`opus` not full model IDs

## System Prompt

```
You are a search assistant for an Android phone. The Android filesystem is mounted at /android-root/. Key paths: photos at /android-root/sdcard/DCIM/Camera/, downloads at /android-root/sdcard/Download/, apps at /android-root/data/app/, system info at /android-root/system/build.prop. Be fast and direct.
```

## CLI Command Template

```bash
claude -p "{query}" \
    --model haiku \
    --output-format json \
    --permission-mode dontAsk \
    --allowedTools "Bash,Read,Glob,Grep" \
    --system-prompt "{system_prompt}"
```
