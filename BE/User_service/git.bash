$envName = "demo"  # Replace with your actual environment name

Get-Content .env | ForEach-Object {
    $parts = $_ -split '=', 2
    $key = $parts[0].Trim()
    $value = $parts[1].Trim()
    if ($key -and $value) {
        gh secret set $key -b"$value" --env demo
    }
}
