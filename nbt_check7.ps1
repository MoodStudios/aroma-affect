# Find the jigsaw orientation for the nose_smith_house connector in each start
$base = "C:\Users\toxic\Projects\MoodStudios\Github\aromacraft\common\src\main\resources\data\aromaaffect\structure\village"
$biomes = @("plains","desert","savanna","snowy","taiga")

foreach ($b in $biomes) {
    $path = "$base\$b\town_centers\nose_smith_start.nbt"
    $bytes = [System.IO.File]::ReadAllBytes($path)
    $ms = New-Object System.IO.MemoryStream(,$bytes)
    $gz = New-Object System.IO.Compression.GZipStream($ms, [System.IO.Compression.CompressionMode]::Decompress)
    $reader = New-Object System.IO.StreamReader($gz, [System.Text.Encoding]::GetEncoding("iso-8859-1"))
    $text = $reader.ReadToEnd()

    # Find nose_smith_house and surrounding context
    $idx = $text.IndexOf("aromaaffect:nose_smith_house")
    if ($idx -ge 0) {
        # Look for orientation nearby (within 200 chars before the pool reference)
        $start = [Math]::Max(0, $idx - 300)
        $chunk = $text.Substring($start, [Math]::Min(600, $text.Length - $start))
        $orientations = [regex]::Matches($chunk, "orientation.{1,5}([a-z_]+)")
        Write-Output "$b start nose_smith jigsaw orientations:"
        foreach ($o in $orientations) { Write-Output "  $($o.Groups[1].Value)" }
    }

    # Also check house NBT jigsaw orientation
    $reader.Close(); $gz.Close(); $ms.Close()

    $path2 = "$base\$b\houses\nose_smith_house.nbt"
    $bytes2 = [System.IO.File]::ReadAllBytes($path2)
    $ms2 = New-Object System.IO.MemoryStream(,$bytes2)
    $gz2 = New-Object System.IO.Compression.GZipStream($ms2, [System.IO.Compression.CompressionMode]::Decompress)
    $reader2 = New-Object System.IO.StreamReader($gz2, [System.Text.Encoding]::GetEncoding("iso-8859-1"))
    $text2 = $reader2.ReadToEnd()

    $idx2 = $text2.IndexOf("aromaaffect:nose_smith_house")
    if ($idx2 -ge 0) {
        $start2 = [Math]::Max(0, $idx2 - 300)
        $chunk2 = $text2.Substring($start2, [Math]::Min(600, $text2.Length - $start2))
        $orientations2 = [regex]::Matches($chunk2, "orientation.{1,5}([a-z_]+)")
        Write-Output "$b house nose_smith jigsaw orientations:"
        foreach ($o in $orientations2) { Write-Output "  $($o.Groups[1].Value)" }
    }
    $reader2.Close(); $gz2.Close(); $ms2.Close()
}
