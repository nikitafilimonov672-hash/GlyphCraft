param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^1\.21(?:\.\d+)?$')]
    [string]$MinecraftVersion,
    [string]$FabricApiVersion
)

if ([version]$MinecraftVersion -lt [version]'1.21' -or [version]$MinecraftVersion -gt [version]'1.21.11') {
    throw "MinecraftVersion must be between 1.21 and 1.21.11"
}

if ([string]::IsNullOrWhiteSpace($FabricApiVersion)) {
    [xml]$metadata = (Invoke-WebRequest -UseBasicParsing `
        'https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml').Content
    $suffix = "+$MinecraftVersion"
    $FabricApiVersion = @($metadata.metadata.versioning.versions.version |
        Where-Object { $_.EndsWith($suffix) })[-1]
    if ([string]::IsNullOrWhiteSpace($FabricApiVersion)) {
        throw "No Fabric API release found for $MinecraftVersion"
    }
}

& .\gradlew.bat build --no-daemon --console=plain `
    "-Pminecraft_version=$MinecraftVersion" `
    "-Pfabric_api_version=$FabricApiVersion"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$dist = Join-Path $PSScriptRoot 'dist'
New-Item -ItemType Directory -Force $dist | Out-Null
Copy-Item -Force (Join-Path $PSScriptRoot 'build\libs\glyphcraft-0.2.1.jar') `
    (Join-Path $dist "glyphcraft-fabric-$MinecraftVersion.jar")
Write-Host "Built Fabric $MinecraftVersion with Fabric API $FabricApiVersion"
