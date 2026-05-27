# run-tests.ps1
# Local test runner for Task Manager API — Windows PowerShell
# Usage: .\run-tests.ps1
# Usage with coverage: .\run-tests.ps1 -Coverage

param(
    [switch]$Coverage,
    [switch]$Verbose
)

$ProjectDir = Join-Path $PSScriptRoot "demo"

Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "  Task Manager API — Test Runner" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

# Verify Maven is available
if (-not (Get-Command "mvn" -ErrorAction SilentlyContinue)) {
    Write-Host "[ERROR] Maven (mvn) is not found in your PATH." -ForegroundColor Red
    Write-Host "        Please install Maven and add it to your PATH." -ForegroundColor Red
    exit 1
}

# Change to the project directory
Push-Location $ProjectDir

try {
    if ($Coverage) {
        Write-Host "[INFO] Running tests with JaCoCo coverage report..." -ForegroundColor Yellow
        Write-Host ""

        if ($Verbose) {
            mvn clean test jacoco:report -Dspring.profiles.active=test
        } else {
            mvn clean test jacoco:report -Dspring.profiles.active=test --batch-mode --no-transfer-progress
        }

        if ($LASTEXITCODE -eq 0) {
            Write-Host ""
            Write-Host "[SUCCESS] All tests passed!" -ForegroundColor Green
            $ReportPath = Join-Path $ProjectDir "target\site\jacoco\index.html"
            if (Test-Path $ReportPath) {
                Write-Host "[INFO] Coverage report generated at:" -ForegroundColor Cyan
                Write-Host "       $ReportPath" -ForegroundColor Cyan
                Write-Host ""
                $OpenReport = Read-Host "Open coverage report in browser? (y/n)"
                if ($OpenReport -eq "y" -or $OpenReport -eq "Y") {
                    Start-Process $ReportPath
                }
            }
        } else {
            Write-Host ""
            Write-Host "[FAILED] Some tests failed. Check output above." -ForegroundColor Red
            exit 1
        }

    } else {
        Write-Host "[INFO] Running all tests (use -Coverage flag for coverage report)..." -ForegroundColor Yellow
        Write-Host ""

        if ($Verbose) {
            mvn clean test -Dspring.profiles.active=test
        } else {
            mvn clean test -Dspring.profiles.active=test --batch-mode --no-transfer-progress
        }

        if ($LASTEXITCODE -eq 0) {
            Write-Host ""
            Write-Host "[SUCCESS] All tests passed!" -ForegroundColor Green
            Write-Host ""
            Write-Host "TIP: Run with -Coverage flag to generate a code coverage report." -ForegroundColor Gray
            Write-Host "     Example: .\run-tests.ps1 -Coverage" -ForegroundColor Gray
        } else {
            Write-Host ""
            Write-Host "[FAILED] Some tests failed. Check output above for details." -ForegroundColor Red
            Write-Host ""
            Write-Host "TIP: Run with -Verbose flag to see full Maven output." -ForegroundColor Gray
            Write-Host "     Example: .\run-tests.ps1 -Verbose" -ForegroundColor Gray
            exit 1
        }
    }

} finally {
    Pop-Location
}
