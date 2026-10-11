# Contributing to Apoapsis

Thanks for helping. Apoapsis is open source end to end, and these rules keep it that way.

## Licences

- Code and data files (recipes, worldgen, quests, configs) are licensed **MIT**. Docs, art, models and sounds are licensed **CC BY 4.0**. Contributions use the same licence as the project (inbound = outbound).
- Sign off every commit (`git commit -s`) to certify the [Developer Certificate of Origin](https://developercertificate.org/).

## Clean-room rules

Game mechanics and ideas can't be owned. Code, art, sounds and written text can. So:

1. **Never copy** code, assets or text from a project that isn't open source. That includes Sable, Create's assets, Space Engineers and KSP game files, SevTech quest text, and FTB Quests data.
2. **Build from the design docs** and public descriptions of how things behave (gameplay, videos, wikis), never from closed source code. **Don't decompile closed mods.**
3. **If you've studied a non-open codebase in depth, don't write the matching Apoapsis module from memory.** Pick a different area.
4. **Use original names.** Generic engineering words (gyroscope, jump drive, merge block) are fine. Brand terms from other games are not.
5. **Use original art from day one.** Placeholders are flat colours or your own sketches.
6. **Look different, not just code different.** Our own art style, models and UI.
7. **No Minecraft code in the repo.** Don't paste vanilla methods into mixins (`@Overwrite` copies, "copy then modify" from the 26.x sources). Inject or wrap instead, and re-implement behaviour from scratch. Mojang's code is proprietary and can't be MIT.
8. **No edited vanilla textures or shaders.** Draw every texture from scratch (matching the *style* is fine; deriving from the *pixels* isn't), and write our own shaders.
9. **Sounds, music and fonts:** use only original, CC0 or CC BY sounds and music (no NC or Sampling+ licences), and OFL fonts. Record the source of anything you didn't make.
10. **AI-assisted work:** AI-assisted code and art get human review like any other contribution. AI art comes through the art pipeline as reproducible source (scripts, Blockbench JSON, SVG) and is labelled in its licence metadata ([16 §16.2](docs/design/16-production-and-polish.md)). Never prompt with, or trace, another game's or mod's art.

Full detail: [docs/design/09-open-source-and-dependencies.md](docs/design/09-open-source-and-dependencies.md) §9.3.
