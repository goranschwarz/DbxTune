param(
    [Parameter(Mandatory)][string]$Username  # e.g. DOMAIN\dbxtune
)

$sid = (New-Object System.Security.Principal.NTAccount($Username)).Translate(
          [System.Security.Principal.SecurityIdentifier]).Value

$cfg = "$env:TEMP\secpol_edit.cfg"
$sdb = "$env:TEMP\secpol_edit.sdb"

secedit /export /cfg $cfg /quiet

$content = Get-Content $cfg
if ($content -match 'SeDebugPrivilege') {
    $content = $content -replace '(SeDebugPrivilege\s*=\s*)(.*)', "`$1`$2,*$sid"
} else {
    $content = $content -replace '\[Privilege Rights\]', "[Privilege Rights]`nSeDebugPrivilege = *$sid"
}
$content | Set-Content $cfg

secedit /import /cfg $cfg /db $sdb /overwrite /quiet
secedit /configure /db $sdb /areas USER_RIGHTS /quiet
gpupdate /force

Write-Host "SeDebugPrivilege granted to $Username ($sid). Re-login SSH to apply."
