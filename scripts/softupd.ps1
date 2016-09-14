
echo "!! Starting provisioning for software upgrade"

$latestBldPath = $env:LatestBuildPath
If ( $latestBldPath -eq $null)
{
	If ( $env:NightlyBuildPath -ne $null)
	{
		$latestBldDir = Get-ChildItem "$env:NightlyBuildPath" | ? { $_.PSIsContainer } | Sort CreationTime -desc | Select -f 1
		If ( $latestBldDir -ne $null)
		{
			$latestBldPath = Join-Path "$env:NightlyBuildPath" "$latestBldDir"
		}
	}
}


If ( $latestBldPath -ne $null)
{
	echo "!! Latest build directrory is :" $latestBldPath

	$strsystmCommDir = "c:\\strsystm\\comm"
	$strsystmZip = Join-Path "$latestBldPath" "strsystm.zip"
	If ( Test-Path $strsystmZip)
	{
		echo "!! Copying latest strsystm zip file"
		Copy-Item "$strsystmZip" "$strsystmCommDir"
	}
	Else
	{
		echo "## Unable to find latest strsystm zip file"
	}

	$storelabZip = Join-Path "$latestBldPath" "storelab.zip"
	If ( Test-Path $storelabZip)
	{
		echo "!! Copying latest storelab zip file"
		Copy-Item "$storelabZip" "$strsystmCommDir"
	}
	Else
	{
		echo "## Unable to find latest strsystm zip file"
	}
	
	If ( Test-Path $env:LatestTestDataFile)
	{
		echo "!! Copying latest test data file"
		Copy-Item "$env:LatestTestDataFile" "$strsystmCommDir"
	}

	if ( Test-Path $strsystmZip -or Test-Path $storelabZip -or Test-Path $env:LatestTestDataFile)
	{
		$env:Path += ";c:\strsystm\bin"
		$env:Path += ";c:\strsystm\dll"

		$secpass = ConvertTo-SecureString 'mayhack' -AsPlainText -Force
		$mycreds = New-Object System.Management.Automation.PSCredential ('mayhack', $secpass)

		Start-Process -Credential $mycreds -FilePath "sscmd" -ArgumentList "`"batchname softupdate force`""
	}
}
Else
{
	echo "## Latest build directrory is not defined"
}

echo "!! Finishing provisioning for software upgrade"