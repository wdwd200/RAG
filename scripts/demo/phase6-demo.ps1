param(
    [string] $BaseUrl = "http://localhost:8080",
    [string] $KnowledgeBaseName = "Phase 6 Demo KB",
    [long] $CreatedBy = 1
)

$ErrorActionPreference = "Stop"

function Invoke-JsonPost {
    param(
        [string] $Url,
        [object] $Body
    )

    $json = $Body | ConvertTo-Json -Depth 10
    return Invoke-RestMethod -Method Post -Uri $Url -ContentType "application/json" -Body $json
}

function Invoke-CurlJson {
    param(
        [string[]] $Arguments
    )

    $output = & curl.exe @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "curl.exe failed with exit code $LASTEXITCODE"
    }
    return $output | ConvertFrom-Json
}

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$sampleDir = Join-Path $repoRoot "docs\demo\sample-documents"

Write-Host "Using API base URL: $BaseUrl"
Write-Host "Using sample documents: $sampleDir"

Write-Host "Creating knowledge base..."
$knowledgeBaseResponse = Invoke-JsonPost `
    -Url "$BaseUrl/api/knowledge-bases" `
    -Body @{
        name = $KnowledgeBaseName
        description = "Reproducible Phase 6 evaluation demo knowledge base"
        ownerId = $CreatedBy
        visibility = "PRIVATE"
    }

$knowledgeBaseId = $knowledgeBaseResponse.data.id
Write-Host "Knowledge base id: $knowledgeBaseId"

$documents = @(
    "hr-handbook.md",
    "expense-policy.md",
    "engineering-guide.md"
)

$uploadedDocuments = @()

foreach ($documentName in $documents) {
    $filePath = Join-Path $sampleDir $documentName
    if (-not (Test-Path $filePath)) {
        throw "Sample document not found: $filePath"
    }

    Write-Host "Uploading $documentName..."
    $uploadResponse = Invoke-CurlJson -Arguments @(
        "-s",
        "-X", "POST",
        "$BaseUrl/api/documents/upload",
        "-F", "knowledgeBaseId=$knowledgeBaseId",
        "-F", "createdBy=$CreatedBy",
        "-F", "file=@$filePath"
    )

    $documentId = $uploadResponse.data.id
    Write-Host "Uploaded $documentName as document id $documentId"

    Write-Host "Processing document $documentId..."
    Invoke-JsonPost -Url "$BaseUrl/api/documents/$documentId/process" -Body @{} | Out-Null

    Write-Host "Indexing document $documentId..."
    Invoke-JsonPost -Url "$BaseUrl/api/documents/$documentId/index" -Body @{} | Out-Null

    $uploadedDocuments += [pscustomobject]@{
        Name = $documentName
        DocumentId = $documentId
    }
}

Write-Host ""
Write-Host "Documents are uploaded, processed, and indexed."
Write-Host "Use the chunks below to replace placeholders in docs/demo/sample-evaluation-questions.json."
Write-Host "Only choose active chunks returned by the API."
Write-Host ""

foreach ($document in $uploadedDocuments) {
    Write-Host "Chunks for $($document.Name), document id $($document.DocumentId):"
    $chunks = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/documents/$($document.DocumentId)/chunks"
    foreach ($chunk in $chunks.data) {
        Write-Host "  chunkId=$($chunk.id), chunkIndex=$($chunk.chunkIndex), isActive=$($chunk.isActive)"
        Write-Host "  content=$($chunk.content)"
        Write-Host ""
    }
}

Write-Host "Next manual steps:"
Write-Host "1. Copy docs/demo/sample-evaluation-questions.json to a local import JSON file."
Write-Host "2. Replace REPLACE_WITH_... placeholders with real numeric chunk IDs."
Write-Host "3. Remove sourceHint fields before calling the import API."
Write-Host "4. Follow docs/demo/phase-006-demo-guide.md to create dataset, import questions, run evaluation, and view reports."
