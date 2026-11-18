# Changelog

## 1.1.0 (not released yet)
- New config file `config/create_dt_fix-common.toml`: one on/off switch per fix, all on by default.
- Create-update safety: if a Create update changes code this mod needs, only that fix turns off (with a line in the log). The game no longer crashes at launch.
- If Create ships its own Dynamic Trees saw fix, this mod's saw fix turns off and the other fixes stay on.
- Works with any Create 6.0.x from 6.0.10 (was locked to 6.0.10).

## 1.0.1 (2026-11-17)
- New hand-made mod-list logo (replaces the AI-drawn logo from 1.0.0). No code changes.

## 1.0.0 (2026-11-16)
First release. Minecraft 1.21.1, NeoForge, Create 6.0.10, Dynamic Trees 1.7+.
- Re-enable Create's Mechanical Saw compat for Dynamic Trees (drops into storage).
- Drill, plough and add-on breakers that use Create's breaker code cut Dynamic Trees with drops.
- Trees fall away from the machine (animation only).
- Deployer with an axe: drops go to the deployer.
- Saws cut Dynamic Trees surface roots; axes cut roots at log speed.
- Create's vanilla tree cutter ignores Dynamic Trees branches (fixes trees that vanished with no drops).
- Break cracks always show on branches.
- Server-side only.
- Vibe coded: code by Claude (Anthropic AI), tested in game by the author.
