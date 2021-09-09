# Dynamic Trees Add-on Template
A template for a Dynamic Trees add-on. Template is mainly aimed at add-ons providing support for another mod, but it can also be used for a generic add-on providing its own content too. 

## Features
- Pre-configured gradle script written in Kotlin. Also contains plugins needed for automatic CurseForge and maven uploading.
- Required elements for mod setup configuration-ready, including a `mods.toml` and main mod class.
- GitHub issue templates, stale, and build workflows pre-configured and setup. 

## Initial Setup
To use this, follow the below steps:

1. Generate a new repository from this template, and/or clone it locally. 
2. Configure properties in [gradle.properties](./gradle.properties) as necessary.
3. Refactor existing `com.example.dtaddon` package and enclosed `DTAddOn` mod class.
4. Update the name and year in the [LICENSE](./LICENSE) file, or replace with your own license if you don't want to use MIT.

After this, you are ready to start creating content for your add-on.

## Publishing Setup
_Coming soon..._

## Mod ID Conventions
Mod IDs should use the flat case convention. By DT convention, should start with `dt`, and end with the ID of the add-on or mod it is supporting. For example, `dtnaturesaura` for the Nature's Aura add-on.