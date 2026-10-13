# Attribution

JixOne's native Android app is a fork of **Metrolist**
(<https://github.com/metrolistgroup/metrolist>), which is itself descended from
InnerTune. Metrolist is licensed under the **GNU General Public License v3.0**,
and so is this fork — see `LICENSE`.

Everything that makes music actually play on a phone comes from upstream: the
Media3 playback service, the InnerTube client, the Room database, downloads,
the media notification and lock-screen controls, and the sync with a YouTube
Music account. That is the whole point of forking rather than rewriting — those
parts took years to get right and reimplementing them would only reproduce
their bugs.

What is JixOne's own: the identity (name, icon, palette), the Persian themes
and artwork, and the Farsi-first, right-to-left interface.

Upstream changes are pulled in rather than diverged from, so fork-specific
edits are kept small and localised on purpose.
