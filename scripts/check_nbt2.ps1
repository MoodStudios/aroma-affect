$biomes = @('plains','desert','savanna','snowy','taiga')
$base = "C:\Users\toxic\Projects\MoodStudios\Github\aromacraft\common\src\main\resources\data\aromaaffect\structure\village"

foreach ($biome in $biomes) {
    Write-Host "=== START: $biome ==="
    $path = "$base\$biome\town_centers\nose_smith_start.nbt"
    $bytes = [System.IO.File]::ReadAllBytes($path)
    $ms = New-Object System.IO.MemoryStream(,$bytes)
    $gz = New-Object System.IO.Compression.GZipStream($ms, [System.IO.Compression.CompressionMode]::Decompress)
    $reader = New-Object System.IO.StreamReader($gz, [System.Text.Encoding]::GetEncoding("iso-8859-1"))
    $text = $reader.ReadToEnd()
    # All minecraft: references
    foreach ($m in [regex]::Matches($text, "minecraft:[a-z_/]+")) { Write-Host "  MC: $($m.Value)" }
    # All aroma references
    foreach ($m in [regex]::Matches($text, "aroma[a-z]+:[a-z_/]+")) { Write-Host "  AR: $($m.Value)" }
    $reader.Close(); $gz.Close(); $ms.Close()
    Write-Host ""
}
