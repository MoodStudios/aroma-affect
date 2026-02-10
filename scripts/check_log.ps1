$bytes = [System.IO.File]::ReadAllBytes("C:\Users\toxic\Projects\MoodStudios\Github\aromacraft\neoforge\run\logs\2026-02-02-7.log.gz")
$ms = New-Object System.IO.MemoryStream(,$bytes)
$gz = New-Object System.IO.Compression.GZipStream($ms, [System.IO.Compression.CompressionMode]::Decompress)
$sr = New-Object System.IO.StreamReader($gz)
$text = $sr.ReadToEnd()
$sr.Close()
$lines = $text -split "`n"
Write-Host "Total lines: $($lines.Count)"
foreach ($line in $lines) {
    if ($line -match "nose_smith|Empty or non-existent|Forced village|jigsaw|couldn.t place|pool.*warn|WARN.*pool") {
        Write-Host $line.Trim()
    }
}
