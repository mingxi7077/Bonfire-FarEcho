# FarEcho Fabric Client Build Notes

## Preferred Release Artifact

Use remapped production jar when network access is healthy:

```powershell
./gradlew.bat :client-fabric-1.21.8:releaseJarPreferred
```

Output:
- `client-fabric-1.21.8/build/libs/*.jar`

Prerequisite:
- `JAVA_HOME` must point to JDK root (example: `C:\Program Files\Java\jdk-21`), not `...\bin`.

## TLS Mitigation

Root `gradle.properties` enforces:
- `jdk.tls.client.protocols=TLSv1.2,TLSv1.3`
- `https.protocols=TLSv1.2,TLSv1.3`

This reduces handshake failures during dependency/native downloads for Loom remap tasks.

## Fallback Path (When remapJar Fails)

If `remapJar` fails due external TLS/network issues, produce a fallback jar without remap:

```powershell
./gradlew.bat :client-fabric-1.21.8:releaseJarFallback
```

Output:
- `client-fabric-1.21.8/build/libs-fallback/*-fallback-dev.jar`

Use this fallback artifact for internal testing while keeping a clear marker that remap was skipped.
