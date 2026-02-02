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
    Write-Host "  Size: $($text.Length)"
    foreach ($m in [regex]::Matches($text, "aroma[a-z]+:[a-z_/]+")) { Write-Host "  REF: $($m.Value)" }
    foreach ($o in [regex]::Matches($text, "(east_up|west_up|up_north|up_south|up_east|up_west|north_up|south_up)")) { Write-Host "  ORI: $($o.Value)" }
    $reader.Close(); $gz.Close(); $ms.Close()

    Write-Host "=== HOUSE: $biome ==="
    $path2 = "$base\$biome\houses\nose_smith_house.nbt"
    $bytes2 = [System.IO.File]::ReadAllBytes($path2)
    $ms2 = New-Object System.IO.MemoryStream(,$bytes2)
    $gz2 = New-Object System.IO.Compression.GZipStream($ms2, [System.IO.Compression.CompressionMode]::Decompress)
    $reader2 = New-Object System.IO.StreamReader($gz2, [System.Text.Encoding]::GetEncoding("iso-8859-1"))
    $text2 = $reader2.ReadToEnd()
    Write-Host "  Size: $($text2.Length)"
    foreach ($m2 in [regex]::Matches($text2, "aroma[a-z]+:[a-z_/]+")) { Write-Host "  REF: $($m2.Value)" }
    foreach ($o2 in [regex]::Matches($text2, "(east_up|west_up|up_north|up_south|up_east|up_west|north_up|south_up)")) { Write-Host "  ORI: $($o2.Value)" }
    $reader2.Close(); $gz2.Close(); $ms2.Close()
    Write-Host ""
}
