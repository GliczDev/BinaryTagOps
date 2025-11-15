# BinaryTagOps

An implementation of [DataFixerUpper](https://github.com/Mojang/DataFixerUpper)'s `DynamicOps`
for [Adventure](https://github.com/PaperMC/adventure)'s `BinaryTag`s

## Adding dependency

### Repository

#### Releases

```kts
maven {
    name = "roxymcReleases"
    url = uri("https://repo.roxymc.net/releases")
}

```

#### Snapshots

```kts
maven {
    name = "roxymcSnapshots"
    url = uri("https://repo.roxymc.net/snapshots")
}
```

### Dependency

```kts
dependencies {
    implementation("me.glicz:binarytagops:VERSION")
}
```

## Usage

All you need is to obtain the instance of `BinaryTagOps`.
<br>
You can use `BinaryTagOps.INSTANCE` for that, but remember, use it wisely!
